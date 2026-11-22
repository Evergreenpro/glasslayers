package com.evergreenpro.glasslayers.xposed.systemui

import android.graphics.PointF
import android.view.View
import com.evergreenpro.glasslayers.xposed.hook.Guard
import com.evergreenpro.glasslayers.xposed.hook.HookEnv
import com.evergreenpro.glasslayers.xposed.hook.HostApplication
import com.evergreenpro.glasslayers.xposed.hook.field
import com.evergreenpro.glasslayers.xposed.hook.hookAll
import com.evergreenpro.glasslayers.xposed.hook.hookAllAfter
import java.util.Collections
import java.util.WeakHashMap

object ChargingRipple {

    private const val WIRED_CONTROLLER = "${SystemUiModule.PACKAGE}.charging.WiredChargingRippleController"
    private const val WIRELESS_LAYOUT = "${SystemUiModule.PACKAGE}.charging.WirelessChargingLayout"
    private const val PORT_X = "normalizedPortPosX"
    private const val PORT_Y = "normalizedPortPosY"
    private const val WIRELESS_RIPPLE = "mRippleView"
    private const val DURATION_SCALE = 1.5f

    private val wirelessLayouts = Collections.newSetFromMap(WeakHashMap<View, Boolean>())

    fun install(): Int {
        var hooks = 0
        hooks += HookEnv.findClass(WIRED_CONTROLLER).hookAll("startRipple") { chain ->
            if (!GlassConfig.chargingRippleActive) return@hookAll chain.proceed()
            val played = Guard.run(TAG_WIRED, false) { playWired(chain.thisObject) }
            if (played) null else chain.proceed()
        }
        hooks += HookEnv.findClass(WIRELESS_LAYOUT).hookAllAfter("onLayout") { chain, _ ->
            val layout = chain.thisObject as? View ?: return@hookAllAfter
            if (!GlassConfig.chargingRippleActive || !wirelessLayouts.add(layout)) return@hookAllAfter
            Guard.run(TAG_WIRELESS, Unit) { playWireless(layout) }
        }
        return hooks
    }

    private fun playWired(controller: Any?): Boolean {
        val context = HostApplication.get() ?: return false
        val x = controller.field(PORT_X) as? Float ?: return false
        val y = controller.field(PORT_Y) as? Float ?: return false
        val center = RippleOverlay.screenCenter(context)
        RippleOverlay.play(context, PointF(x * center.x * 2f, y * center.y * 2f), DURATION_SCALE)
        return true
    }

    private fun playWireless(layout: View) {
        val context = HostApplication.get() ?: return
        (layout.field(WIRELESS_RIPPLE) as? View)?.alpha = 0f
        val location = IntArray(2)
        layout.getLocationOnScreen(location)
        val origin = if (layout.width > 0) {
            PointF(location[0] + layout.width / 2f, location[1] + layout.height / 2f)
        } else {
            RippleOverlay.screenCenter(context)
        }
        RippleOverlay.play(context, origin, DURATION_SCALE)
    }
}

private const val TAG_WIRED = "ChargingRipple.wired"
private const val TAG_WIRELESS = "ChargingRipple.wireless"
