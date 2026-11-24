package com.evergreenpro.glasslayers.xposed.systemui

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.View
import com.evergreenpro.glasslayers.data.source.PrefKeys
import com.evergreenpro.glasslayers.xposed.hook.Guard
import com.evergreenpro.glasslayers.xposed.hook.HookEnv
import com.evergreenpro.glasslayers.xposed.hook.field
import com.evergreenpro.glasslayers.xposed.hook.intercept
import java.util.Collections
import java.util.WeakHashMap

/**
 * Liquid glass for the lockscreen clock digits.
 *
 * The flex clock draws every digit as a [View] with its own glyph renderer (per-glyph font variations), so the
 * glyph outlines aren't available. Instead the shared underlay draws the glass into a layer and masks it with the
 * stock text (see `maskContent`), which shows glass exactly where the digits are, whatever draws them.
 * The glass fades out with the doze amount, so the always-on display keeps the plain stock clock.
 *
 * R8 strips the constructors of the digit view classes and builds each view inside its controller, so the hook
 * sits on the controller's constructor and takes the finished view from its `view` field.
 */
object LockscreenClockGlass {

    private const val CLOCK_CONTROLLER = "${SystemUiModule.PACKAGE}.shared.clocks.controller.FlexClockTextViewController"
    private const val VIEW_FIELD = "view"

    private val clockViews = Collections.newSetFromMap(WeakHashMap<View, Boolean>())

    // One white carrier per digit: the underlay rewrites its alpha, so it can't be shared between views.
    private val tintCarriers = WeakHashMap<View, GradientDrawable>()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun install(): Int {
        val hooks = HookEnv.findClass(CLOCK_CONTROLLER)
            ?.declaredConstructors
            ?.count { constructor ->
                constructor.intercept { chain ->
                    val result = chain.proceed()
                    val view = chain.thisObject.field(VIEW_FIELD) as? View
                    if (view != null) Guard.run("LockscreenClockGlass.attach", Unit) { attach(view) }
                    result
                } != null
            }
            ?: 0
        DozeTracker.addAmountListener(::refresh)
        return hooks
    }

    fun refresh() {
        mainHandler.post { clockViews.toList().forEach { it.invalidate() } }
    }

    private fun attach(view: View) {
        clockViews.add(view)
        ViewGlassUnderlay.attach(
            view,
            PrefKeys.LOCKSCREEN_CLOCK,
            { GlassConfig.lockscreenClockActive },
            { GlassConfig.lockscreenClockTint },
            tintInGlass = true,
            source = ::tintCarrier,
            glassFade = { 1f - DozeTracker.dozeAmount },
            maskContent = true
        )
    }

    private fun tintCarrier(view: View): Drawable =
        tintCarriers.getOrPut(view) { GradientDrawable().apply { setColor(Color.WHITE) } }
}
