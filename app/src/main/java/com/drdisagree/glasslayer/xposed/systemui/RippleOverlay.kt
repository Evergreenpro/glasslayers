package com.drdisagree.glasslayer.xposed.systemui

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import com.drdisagree.glasslayer.xposed.hook.Guard
import com.drdisagree.glasslayer.xposed.hook.HostApplication

internal object RippleOverlay {

    private const val WINDOW_TITLE = "GlassLayerWaterRipple"
    private const val TYPE_SECURE_SYSTEM_OVERLAY = 2015
    private const val TAG_REMOVE = "RippleOverlay.remove"

    private val mainHandler = Handler(Looper.getMainLooper())
    private var current: UnlockRippleView? = null

    val isShowing: Boolean
        get() = current != null

    fun play(context: Context, origin: PointF, durationScale: Float) {
        cancel()
        val view = UnlockRippleView(context, origin, durationScale) { finished -> remove(finished) }
        val manager = context.getSystemService(WindowManager::class.java) ?: return
        manager.addView(view, layoutParams())
        current = view
    }

    fun cancel() {
        current?.finish()
    }

    fun screenCenter(context: Context): PointF {
        val bounds = context.getSystemService(WindowManager::class.java)?.maximumWindowMetrics?.bounds
        return PointF(bounds?.exactCenterX() ?: 0f, bounds?.exactCenterY() ?: 0f)
    }

    private fun remove(view: UnlockRippleView) {
        if (current === view) current = null
        mainHandler.post {
            Guard.run(TAG_REMOVE, Unit) {
                if (view.isAttachedToWindow) HostApplication.get()?.getSystemService(WindowManager::class.java)?.removeViewImmediate(view)
            }
        }
    }

    private fun layoutParams(): WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        TYPE_SECURE_SYSTEM_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT
    ).apply {
        title = WINDOW_TITLE
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        fitInsetsTypes = 0
    }
}
