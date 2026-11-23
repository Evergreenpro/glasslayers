package com.evergreenpro.glasslayers.xposed.systemui

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import androidx.core.graphics.ColorUtils
import com.evergreenpro.glasslayers.data.source.PrefKeys
import com.evergreenpro.glasslayers.xposed.compose.ComposeModifiers
import com.evergreenpro.glasslayers.xposed.compose.ComposeToolkit
import com.evergreenpro.glasslayers.xposed.glass.ComposeLiquidGlass
import com.evergreenpro.glasslayers.xposed.hook.Guard
import com.evergreenpro.glasslayers.xposed.hook.HookEnv
import com.evergreenpro.glasslayers.xposed.hook.call
import com.evergreenpro.glasslayers.xposed.hook.field
import com.evergreenpro.glasslayers.xposed.hook.hookMatching
import com.evergreenpro.glasslayers.xposed.hook.proceedWithArgs
import java.lang.reflect.Method
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.roundToInt

/**
 * Liquid glass for the tiles of the Modes panel (Do Not Disturb and friends).
 *
 * The tiles are a Compose composable, `ModeTile`, built on a Material3 `Surface`, which paints itself with
 * `Modifier.background(color, shape)`. Same approach as the lockscreen shortcuts: while `ModeTile` is composing,
 * the first background call (the tile surface itself, not the icon inside) gets its colour tinted and glass drawn
 * behind it in the tile's own shape.
 */
object ModesGlass {

    private const val MODE_TILE_KT = "${SystemUiModule.PACKAGE}.statusbar.policy.ui.dialog.composable.ModeTileKt"
    private const val BACKGROUND_KT = "androidx.compose.foundation.BackgroundKt"

    private val recomposeScopes = Collections.newSetFromMap(WeakHashMap<Any, Boolean>())
    private val drawNodes = Collections.newSetFromMap(WeakHashMap<Any, Boolean>())
    private val modifierToOriginal = WeakHashMap<Any, Any>()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var tileDepth = 0
    private var surfaceDone = false

    fun install(): Int {
        var hooks = 0
        hooks += HookEnv.findClass(MODE_TILE_KT).hookMatching(Regex("ModeTile")) { chain ->
            captureRecomposeScope(chain.executable as Method, chain.args)
            if (tileDepth++ == 0) surfaceDone = false
            try {
                chain.proceed()
            } finally {
                tileDepth--
            }
        }

        hooks += HookEnv.findClass(BACKGROUND_KT).hookMatching(Regex("background-.*")) { chain ->
            val args = chain.args
            if (tileDepth <= 0 || surfaceDone || !GlassConfig.modesActive || args.size < 3 || args[1] !is Long) {
                return@hookMatching chain.proceed()
            }
            val shape = args[2] ?: return@hookMatching chain.proceed()
            surfaceDone = true
            chain.proceedWithArgs { values ->
                values[1] = tinted(values[1] as Long)
                values[0]?.let { receiver -> applyComposeGlass(receiver, shape)?.let { values[0] = it } }
            }
        }
        return hooks
    }

    fun refresh() {
        mainHandler.post {
            recomposeScopes.toList().forEach { it.call("invalidateForResult", null) }
            drawNodes.toList().forEach { node -> Guard.run(REDRAW_TAG, Unit) { ComposeToolkit.invalidateDraw(node) } }
        }
    }

    private fun captureRecomposeScope(method: Method, args: List<Any?>) {
        val composerIndex = method.parameterTypes.indexOfFirst { it.name == ComposeToolkit.COMPOSER_CLASS }
        args.getOrNull(composerIndex)?.call("getRecomposeScope")?.let { recomposeScopes.add(it) }
    }

    // Active and inactive tiles keep their own colour; only the opacity is scaled so the glass shows through.
    private fun tinted(color: Long): Long {
        val argb = ComposeToolkit.toArgb(color) ?: return color
        if (Color.alpha(argb) == 0) return color
        val alpha = (255 * GlassConfig.modesTint).roundToInt().coerceIn(0, 255)
        return ComposeToolkit.colorOf(ColorUtils.setAlphaComponent(argb, alpha))
    }

    private fun applyComposeGlass(receiver: Any, shape: Any): Any? {
        val original = modifierToOriginal[receiver] ?: receiver
        val drawer = ComposeModifiers.proxy("ModesGlass.draw") { drawScope -> drawComposeGlass(drawScope, shape) }
            ?: return null
        return ComposeModifiers.drawBehind(original, drawer)?.also { modifierToOriginal[it] = original }
    }

    private fun drawComposeGlass(drawScope: Any, shape: Any) {
        drawScope.field(DRAW_NODE)?.let { drawNodes.add(it) }
        if (!GlassConfig.modesActive) return
        val surface = GlassConfig.surface(PrefKeys.MODES)
        Guard.run("ModesGlass.compose_glass", Unit) {
            ComposeLiquidGlass.draw(drawScope, shape, surface.blurRadiusPx, Color.TRANSPARENT, surface.style)
        }
    }

    private const val DRAW_NODE = "drawNode"
    private const val REDRAW_TAG = "ModesGlass.redraw"
}
