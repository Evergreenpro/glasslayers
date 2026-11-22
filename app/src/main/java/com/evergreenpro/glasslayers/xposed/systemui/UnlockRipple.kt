package com.evergreenpro.glasslayers.xposed.systemui

import android.content.Context
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import com.evergreenpro.glasslayers.xposed.hook.Guard
import com.evergreenpro.glasslayers.xposed.hook.HookEnv
import com.evergreenpro.glasslayers.xposed.hook.HostApplication
import com.evergreenpro.glasslayers.xposed.hook.field
import com.evergreenpro.glasslayers.xposed.hook.hookAll
import com.evergreenpro.glasslayers.xposed.hook.hookAllAfter
import com.evergreenpro.glasslayers.xposed.hook.hookAllBefore

object UnlockRipple {

    private const val TRANSITION_REPOSITORY = "${SystemUiModule.PACKAGE}.keyguard.data.repository.KeyguardTransitionRepositoryImpl"
    private const val STATE_CONTROLLER = "${SystemUiModule.PACKAGE}.statusbar.policy.KeyguardStateControllerImpl"
    private const val UPDATE_MONITOR = "com.android.keyguard.KeyguardUpdateMonitor"
    private const val AUTH_RIPPLE_VIEW = "${SystemUiModule.PACKAGE}.biometrics.AuthRippleView"
    private const val LIGHT_REVEAL_SCRIM = "${SystemUiModule.PACKAGE}.statusbar.LightRevealScrim"
    private const val SHADE_WINDOW_VIEW = "${SystemUiModule.PACKAGE}.shade.NotificationShadeWindowView"
    private const val GONE = "GONE"
    private const val STARTED = "STARTED"
    private const val RECENT_MS = 3000L
    private const val STOCK_MUTE_MS = 2500L
    private const val DEDUPE_MS = 800L
    private const val DURATION_SCALE = 1f
    private val LOCKED = setOf("LOCKSCREEN", "AOD", "DOZING", "PRIMARY_BOUNCER", "ALTERNATE_BOUNCER")

    private val mainHandler = Handler(Looper.getMainLooper())
    private var fingerprintAt = 0L
    private var touchAt = 0L
    private val touch = PointF()
    private var startedAt = 0L

    fun install(): Int {
        var hooks = 0
        hooks += HookEnv.findClass(TRANSITION_REPOSITORY).hookAllAfter("emitTransition") { chain, _ ->
            val step = chain.args.firstOrNull() ?: return@hookAllAfter
            val to = step.field("to")?.toString() ?: return@hookAllAfter
            val from = step.field("from")?.toString() ?: return@hookAllAfter
            val state = step.field("transitionState")?.toString() ?: return@hookAllAfter
            when {
                to == GONE && from in LOCKED && state == STARTED -> mainHandler.post { Guard.run(TAG_START, Unit) { start() } }
                to in LOCKED && state == STARTED -> mainHandler.post { Guard.run(TAG_CANCEL, Unit) { RippleOverlay.cancel() } }
            }
        }
        hooks += HookEnv.findClass(STATE_CONTROLLER).hookAllAfter("notifyKeyguardGoingAway") { chain, _ ->
            if (chain.args.firstOrNull() == true) mainHandler.post { Guard.run(TAG_START, Unit) { start() } }
        }
        hooks += HookEnv.findClass(UPDATE_MONITOR).hookAllAfter("onFingerprintAuthenticated") { _, _ ->
            fingerprintAt = SystemClock.uptimeMillis()
        }
        hooks += HookEnv.findClass(AUTH_RIPPLE_VIEW).hookAll("onDraw") { chain ->
            if (mutesStock()) null else chain.proceed()
        }
        hooks += HookEnv.findClass(LIGHT_REVEAL_SCRIM).hookAll("onDraw") { chain ->
            if (mutesStock()) null else chain.proceed()
        }
        hooks += HookEnv.findClass(SHADE_WINDOW_VIEW).hookAllBefore("dispatchTouchEvent") { chain ->
            val event = chain.args.firstOrNull() as? MotionEvent ?: return@hookAllBefore
            if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_UP) {
                touch.set(event.rawX, event.rawY)
                touchAt = SystemClock.uptimeMillis()
            }
        }
        return hooks
    }

    fun refresh() {
        if (!GlassConfig.unlockRippleActive) mainHandler.post { Guard.run(TAG_CANCEL, Unit) { RippleOverlay.cancel() } }
    }

    private fun mutesStock(): Boolean =
        GlassConfig.unlockRippleActive && (RippleOverlay.isShowing || SystemClock.uptimeMillis() - fingerprintAt < STOCK_MUTE_MS)

    private fun start() {
        if (!GlassConfig.unlockRippleActive) return
        val now = SystemClock.uptimeMillis()
        if (now - startedAt < DEDUPE_MS) return
        startedAt = now
        val context = HostApplication.get() ?: return
        RippleOverlay.play(context, origin(context), DURATION_SCALE)
    }

    private fun origin(context: Context): PointF {
        val now = SystemClock.uptimeMillis()
        if (now - fingerprintAt < RECENT_MS) {
            Guard.run(TAG_SENSOR, null) { FingerprintSensorPoint.read(context) }?.let { return it }
        }
        if (now - touchAt < RECENT_MS) return PointF(touch.x, touch.y)
        return RippleOverlay.screenCenter(context)
    }
}

private const val TAG_START = "UnlockRipple.start"
private const val TAG_CANCEL = "UnlockRipple.cancel"
private const val TAG_SENSOR = "UnlockRipple.sensor"
