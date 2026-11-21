package com.drdisagree.glasslayer.xposed.systemui

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import com.drdisagree.glasslayer.data.source.PrefKeys
import com.drdisagree.glasslayer.xposed.hook.Guard
import com.drdisagree.glasslayer.xposed.hook.HookEnv
import com.drdisagree.glasslayer.xposed.hook.field
import com.drdisagree.glasslayer.xposed.hook.intercept
import java.util.Collections
import java.util.WeakHashMap

object FingerprintGlass {

    private const val DEVICE_ENTRY_ICON_VIEW = "${SystemUiModule.PACKAGE}.keyguard.ui.view.DeviceEntryIconView"
    private const val BACKGROUND = "bgView"

    private val backgrounds = Collections.newSetFromMap(WeakHashMap<View, Boolean>())
    private val mainHandler = Handler(Looper.getMainLooper())

    fun install(): Int {
        val hooks = HookEnv.findClass(DEVICE_ENTRY_ICON_VIEW)
            ?.declaredConstructors
            ?.count { constructor ->
                constructor.intercept { chain ->
                    val result = chain.proceed()
                    val background = chain.thisObject.field(BACKGROUND) as? ImageView
                    if (background != null) Guard.run("FingerprintGlass.attach", Unit) { attach(background) }
                    result
                } != null
            }
            ?: 0
        DozeTracker.addAmountListener(::refresh)
        return hooks
    }

    fun refresh() {
        mainHandler.post { backgrounds.toList().forEach { it.invalidate() } }
    }

    private fun stockBlend(tint: Float): Float = tint + (1f - tint) * DozeTracker.dozeAmount

    private fun attach(background: ImageView) {
        backgrounds.add(background)
        ViewGlassUnderlay.attach(
            background,
            PrefKeys.FINGERPRINT,
            { GlassConfig.fingerprintActive },
            { stockBlend(GlassConfig.fingerprintTint) },
            source = { (it as? ImageView)?.drawable },
            imageContent = true,
            glassFade = { 1f - DozeTracker.dozeAmount }
        )
    }
}
