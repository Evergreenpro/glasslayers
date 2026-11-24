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
import com.evergreenpro.glasslayers.xposed.hook.hookAllBefore
import com.evergreenpro.glasslayers.xposed.hook.intercept
import com.evergreenpro.glasslayers.xposed.report.HookReporter
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
 * R8 strips the constructors of the digit view classes and builds each view inside its controller, so views are
 * found two ways: the controller's constructor (its `view` field), and the digit view's own `onDraw`, which is a
 * real method and catches any view the constructor hook missed. Attaching is idempotent, so both can run.
 *
 * Every state change that decides whether glass shows is written to the report log, so a clock that stays stock
 * can be diagnosed from the exported report.
 */
object LockscreenClockGlass {

    private const val CLOCK_CONTROLLER = "${SystemUiModule.PACKAGE}.shared.clocks.controller.FlexClockTextViewController"
    private const val CLOCK_VIEW = "${SystemUiModule.PACKAGE}.customization.clocks.view.DigitalClockTextView"
    private const val VIEW_FIELD = "view"
    private const val MAX_STATE_LOGS = 8

    private val clockViews = Collections.newSetFromMap(WeakHashMap<View, Boolean>())

    // One white carrier per digit: the underlay rewrites its alpha, so it can't be shared between views.
    private val tintCarriers = WeakHashMap<View, GradientDrawable>()
    private val loggedStates = HashSet<String>()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun install(): Int {
        var hooks = HookEnv.findClass(CLOCK_CONTROLLER)
            ?.declaredConstructors
            ?.count { constructor ->
                constructor.intercept { chain ->
                    val result = chain.proceed()
                    val view = chain.thisObject.field(VIEW_FIELD) as? View
                    if (view != null) Guard.run("LockscreenClockGlass.attach", Unit) { attach(view, "controller") }
                    result
                } != null
            }
            ?: 0
        hooks += HookEnv.findClass(CLOCK_VIEW).hookAllBefore("onDraw") { chain ->
            val view = chain.thisObject as? View ?: return@hookAllBefore
            Guard.run("LockscreenClockGlass.onDraw", Unit) {
                attach(view, "onDraw")
                logState(view)
            }
        }
        HookReporter.log("LockscreenClockGlass installed, hooks=$hooks")
        DozeTracker.addAmountListener(::refresh)
        return hooks
    }

    fun refresh() {
        mainHandler.post { clockViews.toList().forEach { it.invalidate() } }
    }

    private fun attach(view: View, via: String) {
        if (ViewGlassUnderlay.isAttached(view)) return
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
        HookReporter.log("LockscreenClockGlass attached ${view.javaClass.simpleName} via $via ${view.width}x${view.height}")
        // Attaching during a draw pass only takes effect from the next frame, so ask for one.
        mainHandler.post { view.invalidate() }
    }

    private fun logState(view: View) {
        if (loggedStates.size >= MAX_STATE_LOGS) return
        val state = "master=${GlassConfig.isActive} toggle=${GlassConfig.lockscreenClock} " +
            "glass=${GlassConfig.glassAvailable} doze=${DozeTracker.dozeAmount} " +
            "attached=${ViewGlassUnderlay.isAttached(view)} shown=${view.isShown} " +
            "size=${view.width}x${view.height}"
        if (loggedStates.add(state)) HookReporter.log("LockscreenClockGlass draw: $state")
    }

    private fun tintCarrier(view: View): Drawable =
        tintCarriers.getOrPut(view) { GradientDrawable().apply { setColor(Color.WHITE) } }
}
