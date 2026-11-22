package com.evergreenpro.glasslayers.xposed.systemui

import android.graphics.Color
import android.view.View
import com.evergreenpro.glasslayers.xposed.hook.HookEnv
import com.evergreenpro.glasslayers.xposed.hook.call
import com.evergreenpro.glasslayers.xposed.hook.hookAllBefore
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.roundToInt

internal object BouncerScrim {

    private const val SCRIM_VIEW = "${SystemUiModule.PACKAGE}.scrim.ScrimView"
    private const val BEHIND = "scrim_behind"
    private const val NOTIFICATIONS = "scrim_notifications"
    private const val OPAQUE = 255

    private val scrims = Collections.newSetFromMap(WeakHashMap<View, Boolean>())
    private val names = WeakHashMap<View, String>()

    fun install(): Int = HookEnv.findClass(SCRIM_VIEW).hookAllBefore("onDraw") { chain ->
        (chain.thisObject as? View)?.let { scrims.add(it) }
    }

    fun color(): Int {
        var result = Color.TRANSPARENT
        listOf(BEHIND, NOTIFICATIONS).forEach { name ->
            val view = scrims.firstOrNull { it.isAttachedToWindow && nameOf(it) == name } ?: return@forEach
            result = ColorBlend.over(drawnColor(view), result)
        }
        return result
    }

    private fun drawnColor(view: View): Int {
        if (!view.isShown) return Color.TRANSPARENT
        val drawable = view.call("getDrawable") ?: return Color.TRANSPARENT
        val main = drawable.call("getMainColor") as? Int ?: return Color.TRANSPARENT
        val drawableAlpha = (drawable.call("getAlpha") as? Int ?: OPAQUE) / OPAQUE.toFloat()
        val alpha = (Color.alpha(main) * drawableAlpha * view.alpha).roundToInt().coerceIn(0, OPAQUE)
        return Color.argb(alpha, Color.red(main), Color.green(main), Color.blue(main))
    }

    private fun nameOf(view: View): String? = names[view] ?: run {
        val id = view.id
        if (id == View.NO_ID || id ushr 24 == 0) return null
        runCatching { view.resources.getResourceEntryName(id) }.getOrNull()?.also { names[view] = it }
    }
}
