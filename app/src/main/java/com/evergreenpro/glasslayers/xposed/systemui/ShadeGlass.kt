package com.evergreenpro.glasslayers.xposed.systemui

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.core.graphics.ColorUtils
import com.evergreenpro.glasslayers.domain.model.BatterySaverBackground
import com.evergreenpro.glasslayers.xposed.compose.ComposeToolkit
import com.evergreenpro.glasslayers.xposed.glass.BackdropZoom
import com.evergreenpro.glasslayers.xposed.hook.Guard
import com.evergreenpro.glasslayers.xposed.hook.HookEnv
import com.evergreenpro.glasslayers.xposed.hook.call
import com.evergreenpro.glasslayers.xposed.hook.field
import com.evergreenpro.glasslayers.xposed.hook.hookAll
import com.evergreenpro.glasslayers.xposed.hook.hookMatching
import com.evergreenpro.glasslayers.xposed.hook.proceedWithArgs
import java.lang.ref.WeakReference
import java.lang.reflect.Method
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.roundToInt

object ShadeGlass {

    private const val SCRIM_CONTROLLER = "${SystemUiModule.PACKAGE}.statusbar.phone.ScrimController"
    private const val BLUR_UTILS = "${SystemUiModule.PACKAGE}.statusbar.BlurUtils"
    private const val ANIMATED_BACKGROUND_CLASS = "com.android.compose.modifiers.AnimatedBackgroundKt"
    private const val NOTIFICATIONS_COMPOSABLE_CLASS =
        "${SystemUiModule.PACKAGE}.notifications.ui.composable.NotificationsKt"
    private const val PANEL_SCRIM_KT = "${SystemUiModule.PACKAGE}.shade.ui.composable.ShadePanelScrimKt"
    private const val BACKGROUND_KT = "androidx.compose.foundation.BackgroundKt"
    private const val SHADE_WINDOW_TITLE = "NotificationShade"
    private const val OPAQUE = 255
    private const val SEMI_TRANSPARENT_ALPHA = 0.85f
    private const val SEMI_TRANSPARENT_PANEL_ALPHA = 0.92f
    private const val SPLIT_SHADE_CONFIG = "config_use_split_notification_shade"
    private val GLASS_SCRIM_STATES = setOf("SHADE_LOCKED", "UNLOCKED")

    private val scrimAlphas = WeakHashMap<Any, Any>()
    private val zoomLocation = IntArray(2)
    private val lastScrimCalls = WeakHashMap<View, Array<Any?>>()
    private val peakScrimAlphas = WeakHashMap<Any, Float>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var scrimController: WeakReference<Any>? = null
    private var scrimMethod: Method? = null
    private var behindAlpha = 0f
    private var splitShadeId: Int? = null
    private var panelDepth = 0
    private var panelScrimSeen = false
    private val panelScopes = Collections.newSetFromMap(WeakHashMap<Any, Boolean>())

    fun install(): Int {
        var hooks = 0
        hooks += HookEnv.findClass(SCRIM_CONTROLLER).hookAll("updateScrimColor") { chain ->
            chain.proceedWithArgs { args ->
                remember(chain.thisObject, chain.executable as? Method, args)
                if (!GlassConfig.shadeBackgroundActive || usesStockScrim()) return@proceedWithArgs
                val colorIndex = args.indexOfFirst { it is Int }
                val alphaIndex = args.indexOfFirst { it is Float }
                if (colorIndex < 0 || alphaIndex < 0) return@proceedWithArgs
                val color = args[colorIndex] as Int
                val alpha = args[alphaIndex] as Float
                val scrim = args.firstOrNull { it is View } ?: return@proceedWithArgs
                val state = chain.thisObject.field("mState").toString()
                if (state !in GLASS_SCRIM_STATES) return@proceedWithArgs

                val behind = scrim === chain.thisObject.field("mScrimBehind")
                val notifications = scrim === chain.thisObject.field("mNotificationsScrim")
                if (behind || notifications) {
                    args[colorIndex] = ColorUtils.setAlphaComponent(color, OPAQUE)
                    var applied = scrimAlpha(scrim, alpha)
                    if (behind) behindAlpha = applied
                    if (notifications && stacksOverBehind(scrim as View)) {
                        applied = alphaOver(expansionOf(scrim, alpha) * SEMI_TRANSPARENT_PANEL_ALPHA, behindAlpha)
                    }
                    args[alphaIndex] = applied
                }
            }
        }

        hooks += HookEnv.findClass(ANIMATED_BACKGROUND_CLASS).hookAll("animatedBackground") { chain ->
            chain.proceedWithArgs { args ->
                if (args.size < 3) return@proceedWithArgs
                val alpha = args[2] ?: return@proceedWithArgs
                if (alpha in scrimAlphas.values) return@proceedWithArgs
                if (!isCalledFromNotificationPanel()) return@proceedWithArgs

                args[2] = scrimAlphas.getOrPut(alpha) {
                    ComposeToolkit.function0(
                        "ShadeGlass.scrimAlpha",
                        fallback = { ComposeToolkit.invokeFunction(alpha) ?: 1f }
                    ) {
                        val value = ComposeToolkit.invokeFunction(alpha) as? Float ?: 1f
                        if (GlassConfig.shadeBackgroundActive && !usesStockScrim()) notificationAlpha(alpha, value) else value
                    } ?: return@proceedWithArgs
                }
            }
        }

        hooks += HookEnv.findClass(PANEL_SCRIM_KT).hookAll("ShadePanelScrim") { chain ->
            captureScope(chain.executable as Method, chain.args, panelScopes)
            panelScrimSeen = true
            panelDepth++
            try {
                chain.proceed()
            } finally {
                panelDepth--
            }
        }

        hooks += HookEnv.findClass(BACKGROUND_KT).hookMatching(Regex("background-.*")) { chain ->
            if (panelDepth <= 0 || chain.args.size < 2 || chain.args[1] !is Long) return@hookMatching chain.proceed()
            if (!GlassConfig.shadeBackgroundActive || usesStockScrim()) return@hookMatching chain.proceed()
            chain.proceedWithArgs { args -> args[1] = panelColor(args[1] as Long) }
        }

        hooks += HookEnv.findClass(BLUR_UTILS).hookAll("applyBlur") { chain ->
            chain.proceedWithArgs { args ->
                if (args.size < 2) return@proceedWithArgs
                val title = (args[0].field("mWindowAttributes") as? WindowManager.LayoutParams)?.title?.toString()
                if (title != SHADE_WINDOW_TITLE) return@proceedWithArgs
                Guard.run("ShadeGlass.zoom", Unit) { trackZoom(args[0], args.getOrNull(2) as? Float) }
                if (!GlassConfig.shadeBackgroundActive) return@proceedWithArgs
                val radius = args[1] as? Int ?: return@proceedWithArgs
                if (radius <= 0) return@proceedWithArgs
                args[1] = (radius * GlassConfig.backgroundBlur).roundToInt()
            }
        }
        return hooks
    }

    fun refresh() {
        mainHandler.post {
            panelScopes.toList().forEach { it.call("invalidateForResult", null) }
            val controller = scrimController?.get() ?: return@post
            val method = scrimMethod ?: return@post
            lastScrimCalls.values.toList().forEach { args ->
                Guard.run("ShadeGlass.refresh", Unit) { method.invoke(controller, *args) }
            }
        }
    }

    fun onBlurChanged() {
        peakScrimAlphas.clear()
        refresh()
    }

    private fun captureScope(method: Method, args: List<Any?>, scopes: MutableSet<Any>) {
        val composerIndex = method.parameterTypes.indexOfFirst { it.name == ComposeToolkit.COMPOSER_CLASS }
        args.getOrNull(composerIndex)?.call("getRecomposeScope")?.let { scopes.add(it) }
    }

    private fun panelAlpha(): Float =
        if (WindowBlurState.enabled || GlassConfig.batterySaverBackground == BatterySaverBackground.KEEP_TINT) {
            GlassConfig.backgroundDim
        } else {
            SEMI_TRANSPARENT_ALPHA
        }

    private fun panelColor(color: Long): Long {
        val argb = ComposeToolkit.toArgb(color) ?: return color
        val alpha = (OPAQUE * panelAlpha()).roundToInt().coerceIn(0, OPAQUE)
        return ComposeToolkit.colorOf(ColorUtils.setAlphaComponent(argb, alpha))
    }

    private fun notificationAlpha(key: Any, value: Float): Float {
        val semiTransparent = !WindowBlurState.enabled &&
                GlassConfig.batterySaverBackground == BatterySaverBackground.SEMI_TRANSPARENT
        if (!semiTransparent || !panelScrimSeen) return scrimAlpha(key, value)
        return alphaOver(expansionOf(key, value) * SEMI_TRANSPARENT_PANEL_ALPHA, SEMI_TRANSPARENT_ALPHA)
    }

    private fun usesStockScrim(): Boolean =
        !WindowBlurState.enabled && GlassConfig.batterySaverBackground == BatterySaverBackground.STOCK

    private fun scrimAlpha(scrim: Any, alpha: Float): Float {
        if (WindowBlurState.enabled || GlassConfig.batterySaverBackground == BatterySaverBackground.KEEP_TINT) {
            return alpha * GlassConfig.backgroundDim
        }
        return expansionOf(scrim, alpha) * SEMI_TRANSPARENT_ALPHA
    }

    private fun stacksOverBehind(scrim: View): Boolean =
        !WindowBlurState.enabled &&
                GlassConfig.batterySaverBackground == BatterySaverBackground.SEMI_TRANSPARENT &&
                !isSplitShade(scrim)

    private fun alphaOver(target: Float, below: Float): Float =
        if (below >= 1f) 0f else ((target - below) / (1f - below)).coerceIn(0f, 1f)

    private fun isSplitShade(view: View): Boolean {
        val resources = view.resources
        val id = splitShadeId ?: resources.getIdentifier(SPLIT_SHADE_CONFIG, "bool", SystemUiModule.PACKAGE).also { splitShadeId = it }
        return id != 0 && resources.getBoolean(id)
    }

    private fun expansionOf(scrim: Any, alpha: Float): Float {
        val peak = maxOf(peakScrimAlphas[scrim] ?: 0f, alpha)
        peakScrimAlphas[scrim] = peak
        return if (peak > 0f) alpha / peak else 0f
    }

    private fun remember(controller: Any?, method: Method?, args: Array<Any?>) {
        controller ?: return
        method ?: return
        val scrim = args.firstOrNull { it is View } as? View ?: return
        scrimController = WeakReference(controller)
        scrimMethod = method
        lastScrimCalls[scrim] = args.copyOf()
    }

    private fun trackZoom(viewRoot: Any?, scale: Float?) {
        val root = viewRoot.call("getView") as? View
        if (root != null && root.isAttachedToWindow) {
            root.getLocationOnScreen(zoomLocation)
            BackdropZoom.centerX = zoomLocation[0] + root.width / 2f
            BackdropZoom.centerY = zoomLocation[1] + root.height / 2f
        }
        BackdropZoom.scale = scale?.takeIf { it > 0f } ?: 1f
    }

    private fun isCalledFromNotificationPanel(): Boolean {
        val caller = Thread.currentThread().stackTrace.firstOrNull { frame ->
            (frame.className.startsWith("com.android.systemui.") ||
                    frame.className.startsWith("com.android.compose.")) &&
                    !frame.className.startsWith(ANIMATED_BACKGROUND_CLASS)
        } ?: return false

        return caller.className.startsWith(NOTIFICATIONS_COMPOSABLE_CLASS)
    }
}
