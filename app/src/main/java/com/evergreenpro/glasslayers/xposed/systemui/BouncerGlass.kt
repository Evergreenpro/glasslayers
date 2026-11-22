package com.evergreenpro.glasslayers.xposed.systemui

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.graphics.ColorUtils
import com.evergreenpro.glasslayers.data.source.PrefKeys
import com.evergreenpro.glasslayers.xposed.compose.ComposeModifiers
import com.evergreenpro.glasslayers.xposed.compose.ComposeToolkit
import com.evergreenpro.glasslayers.xposed.glass.ComposeLiquidGlass
import com.evergreenpro.glasslayers.xposed.hook.Guard
import com.evergreenpro.glasslayers.xposed.hook.HookEnv
import com.evergreenpro.glasslayers.xposed.hook.call
import com.evergreenpro.glasslayers.xposed.hook.hookAllAfter
import com.evergreenpro.glasslayers.xposed.hook.hookMatching
import com.evergreenpro.glasslayers.xposed.hook.proceedWithArgs
import java.lang.reflect.Method
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.roundToInt

object BouncerGlass {

    private const val NUM_PAD_KEY = "com.android.keyguard.NumPadKey"
    private const val NUM_PAD_BUTTON = "com.android.keyguard.NumPadButton"
    private const val EMERGENCY_BUTTON = "com.android.keyguard.EmergencyButton"
    private const val PIN_BOUNCER_KT = "${SystemUiModule.PACKAGE}.bouncer.ui.composable.PinBouncerKt"
    private const val DRAW_SCOPE = "androidx.compose.ui.graphics.drawscope.DrawScope"
    private const val BOUNCER_CONTENT_KT = "${SystemUiModule.PACKAGE}.bouncer.ui.composable.BouncerContentKt"
    private const val CLIP_KT = "androidx.compose.ui.draw.ClipKt"
    private const val BOUNCER_OVERLAY_KT = "${SystemUiModule.PACKAGE}.bouncer.ui.composable.BouncerOverlayKt"
    private const val BACKGROUND_KT = "androidx.compose.foundation.BackgroundKt"
    private const val BACKGROUND_ARGS = 3
    private const val PIN_GLASS_KEY = 0
    private const val ROUND_RECT_ARGS = 5
    private const val GET_SIZE = "getSize-NH-jbRc"

    private val views = WeakHashMap<View, Boolean>()
    private val recomposeScopes = Collections.newSetFromMap(WeakHashMap<Any, Boolean>())
    private val modifierToOriginal = WeakHashMap<Any, Any>()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var pinDrawDepth = 0
    private var actionKeyDepth = 0
    private var overlayDepth = 0
    @Volatile
    private var scrimColor = Color.TRANSPARENT
    private var drawAccent = false
    private var actionDepth = 0
    private var actionShape: Any? = null

    fun install(): Int {
        var hooks = BouncerScrim.install()
        listOf(NUM_PAD_KEY to false, NUM_PAD_BUTTON to true).forEach { (name, accent) ->
            val type = HookEnv.findClass(name)
            listOf("onLayout", "onConfigurationChanged").forEach { method ->
                hooks += type.hookAllAfter(method) { chain, _ -> track(chain.thisObject as View, accent) }
            }
        }
        hooks += HookEnv.findClass(EMERGENCY_BUTTON).hookAllAfter("onFinishInflate") { chain, _ ->
            track(chain.thisObject as View, false)
        }
        hooks += HookEnv.findClass(PIN_BOUNCER_KT).hookMatching(Regex("ActionButton(-.*)?")) { chain ->
            actionKeyDepth++
            try {
                chain.proceed()
            } finally {
                actionKeyDepth--
            }
        }
        hooks += HookEnv.findClass(PIN_BOUNCER_KT).hookMatching(Regex("PinPadButton(-.*)?")) { chain ->
            val method = chain.executable as Method
            captureRecomposeScope(method, chain.args)
            chain.proceedWithArgs { args -> applyComposeGlass(method, args) }
        }
        hooks += HookEnv.findClass(DRAW_SCOPE).hookMatching(Regex("drawRoundRect-[^\$]*[\$]default")) { chain ->
            if (pinDrawDepth <= 0 || !GlassConfig.bouncerActive) return@hookMatching chain.proceed()
            Guard.run("BouncerGlass.pin_glass", Unit) { drawPinGlass(chain.args) }
            chain.proceedWithArgs { args -> (args.getOrNull(1) as? Long)?.let { args[1] = cappedFill(it) } }
        }
        hooks += HookEnv.findClass(BOUNCER_CONTENT_KT).hookMatching(Regex("ActionArea")) { chain ->
            captureRecomposeScope(chain.executable as Method, chain.args)
            actionDepth++
            actionShape = null
            try {
                chain.proceed()
            } finally {
                actionDepth--
            }
        }
        hooks += HookEnv.findClass(CLIP_KT).hookMatching(Regex("clip")) { chain ->
            if (actionDepth > 0) chain.args.getOrNull(1)?.let { actionShape = it }
            chain.proceed()
        }
        hooks += HookEnv.findClass(BOUNCER_OVERLAY_KT).hookMatching(Regex("Background(-.*)?")) { chain ->
            overlayDepth++
            try {
                chain.proceed()
            } finally {
                overlayDepth--
            }
        }
        hooks += HookEnv.findClass(BACKGROUND_KT).hookMatching(Regex("background-.*")) { chain ->
            if (overlayDepth > 0 && chain.args.size == BACKGROUND_ARGS) {
                (chain.args[1] as? Long)?.let { ComposeToolkit.toArgb(it) }?.let { scrimColor = it }
            }
            if (actionDepth <= 0 || chain.args.size != BACKGROUND_ARGS || chain.args[1] !is Long) {
                return@hookMatching chain.proceed()
            }
            chain.proceedWithArgs { args -> applyActionGlass(args) }
        }
        return hooks
    }

    fun refresh() {
        mainHandler.post {
            views.toList().forEach { (view, accent) -> apply(view, accent) }
            recomposeScopes.toList().forEach { it.call("invalidateForResult", null) }
        }
    }

    private fun track(view: View, accent: Boolean) {
        views[view] = accent
        apply(view, accent)
    }

    private fun apply(view: View, accent: Boolean) {
        ViewGlassUnderlay.attach(view, PrefKeys.BOUNCER, { GlassConfig.bouncerActive }, { keyTint(accent) }, behind = { scrimDim() })
    }

    private fun scrimDim(): Int = if (Color.alpha(scrimColor) > 0) scrimColor else BouncerScrim.color()

    private fun keyTint(accent: Boolean): Float = if (accent) GlassConfig.bouncerAccentTint else GlassConfig.bouncerTint

    private fun captureRecomposeScope(method: Method, args: List<Any?>) {
        val composerIndex = method.parameterTypes.indexOfFirst { it.name == ComposeToolkit.COMPOSER_CLASS }
        args.getOrNull(composerIndex)?.call("getRecomposeScope")?.let { recomposeScopes.add(it) }
    }

    private fun applyComposeGlass(method: Method, args: Array<Any?>) {
        val types = method.parameterTypes
        val modifierIndex = types.indexOfFirst { it.name == ComposeToolkit.MODIFIER_CLASS }
        if (modifierIndex == -1) return
        val passed = args[modifierIndex]
        val original = passed?.let { modifierToOriginal[it] ?: it } ?: ComposeToolkit.emptyModifier ?: return
        args[modifierIndex] = passed?.let { original }
        if (!GlassConfig.bouncerActive) return
        val accent = actionKeyDepth > 0

        val colorIndex = types.indexOfFirst { it == Long::class.javaPrimitiveType }
        (args.getOrNull(colorIndex) as? Long)?.let { color ->
            val argb = ComposeToolkit.toArgb(color)
            if (argb != null) {
                val alpha = (OPAQUE * keyTint(accent)).roundToInt().coerceIn(0, OPAQUE)
                args[colorIndex] = ComposeToolkit.colorOf(ColorUtils.setAlphaComponent(argb, alpha))
            }
        }

        val drawer = ComposeModifiers.contentProxy("BouncerGlass.draw") { drawScope ->
            pinDrawDepth++
            val outer = drawAccent
            drawAccent = accent
            try {
                drawScope.call("drawContent")
            } finally {
                drawAccent = outer
                pinDrawDepth--
            }
        } ?: return
        ComposeModifiers.drawWithContent(original, drawer)?.let {
            modifierToOriginal[it] = original
            args[modifierIndex] = it
            if (passed == null) ComposeToolkit.clearDefaultBit(method, args, modifierIndex)
        }
    }

    private fun applyActionGlass(args: Array<Any?>) {
        if (!GlassConfig.bouncerActive) return
        val shape = actionShape ?: return
        val modifier = args[0] ?: return
        val argb = ComposeToolkit.toArgb(args[1] as Long) ?: return
        val alpha = (OPAQUE * GlassConfig.bouncerTint).roundToInt().coerceIn(0, OPAQUE)
        args[1] = ComposeToolkit.colorOf(ColorUtils.setAlphaComponent(argb, alpha))
        val drawer = ComposeModifiers.proxy("BouncerGlass.action") { drawScope -> drawActionGlass(drawScope, shape) } ?: return
        ComposeModifiers.drawBehind(modifier, drawer)?.let { args[0] = it }
    }

    private fun drawActionGlass(drawScope: Any, shape: Any) {
        if (!GlassConfig.bouncerActive) return
        val surface = GlassConfig.surface(PrefKeys.BOUNCER)
        Guard.run("BouncerGlass.action_glass", Unit) {
            ComposeLiquidGlass.draw(drawScope, shape, surface.blurRadiusPx, scrimDim(), surface.style)
        }
    }

    private fun drawPinGlass(args: List<Any?>) {
        if (args.size < ROUND_RECT_ARGS) return
        val drawScope = args[0] ?: return
        val topLeft = args[2] as? Long ?: return
        val passedSize = args[3] as? Long ?: return
        val size = if (passedSize ushr 32 != 0L && passedSize and LOW_BITS != 0L) passedSize else remainingSize(drawScope, topLeft) ?: return
        val corner = args[4] as? Long ?: return
        val radius = Float.fromBits((corner ushr 32).toInt())
        val colorAlpha = (args[1] as? Long)?.let { ComposeToolkit.toArgb(it) }?.let { Color.alpha(it) / OPAQUE.toFloat() } ?: 1f
        val tint = keyTint(drawAccent)
        val fade = if (tint > 0f) (colorAlpha / tint).coerceIn(0f, 1f) else 1f
        if (fade <= 0f) return
        val surface = GlassConfig.surface(PrefKeys.BOUNCER)
        ComposeLiquidGlass.drawSegment(
            drawScope,
            PIN_GLASS_KEY,
            Float.fromBits((topLeft ushr 32).toInt()),
            Float.fromBits((topLeft and LOW_BITS).toInt()),
            Float.fromBits((size ushr 32).toInt()),
            Float.fromBits((size and LOW_BITS).toInt()),
            FloatArray(RADII) { radius },
            surface.blurRadiusPx,
            scrimDim(),
            surface.style,
            fade
        )
    }

    private fun cappedFill(color: Long): Long {
        val argb = ComposeToolkit.toArgb(color) ?: return color
        val limit = (OPAQUE * keyTint(drawAccent)).roundToInt().coerceIn(0, OPAQUE)
        if (Color.alpha(argb) <= limit) return color
        return ComposeToolkit.colorOf(ColorUtils.setAlphaComponent(argb, limit))
    }

    private fun remainingSize(drawScope: Any, topLeft: Long): Long? {
        val size = drawScope.call(GET_SIZE) as? Long ?: return null
        val width = Float.fromBits((size ushr 32).toInt()) - Float.fromBits((topLeft ushr 32).toInt())
        val height = Float.fromBits((size and LOW_BITS).toInt()) - Float.fromBits((topLeft and LOW_BITS).toInt())
        if (width <= 0f || height <= 0f) return null
        return (width.toRawBits().toLong() shl 32) or (height.toRawBits().toLong() and LOW_BITS)
    }

    private const val OPAQUE = 255
    private const val RADII = 8
    private const val LOW_BITS = 0xFFFFFFFFL
}
