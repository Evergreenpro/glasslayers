package com.evergreenpro.glasslayers.xposed.systemui

import android.widget.ImageView
import com.evergreenpro.glasslayers.data.source.PrefKeys
import com.evergreenpro.glasslayers.xposed.hook.Guard
import com.evergreenpro.glasslayers.xposed.hook.HookEnv
import com.evergreenpro.glasslayers.xposed.hook.field
import com.evergreenpro.glasslayers.xposed.hook.intercept

/**
 * Glass for the in-display fingerprint icon on the lockscreen (and the alternate bouncer, which reuses the same view).
 *
 * `DeviceEntryIconView` stacks a circular background image under the lock / fingerprint icon. SystemUI fades that
 * circle in and out and recolours it with an image tint, so the circle becomes a live glass surface while the icon
 * on top is left alone.
 */
object LockscreenFingerprintGlass {

    private const val ICON_VIEW = "${SystemUiModule.PACKAGE}.keyguard.ui.view.DeviceEntryIconView"
    private const val BACKGROUND_FIELD = "bgView"

    fun install(): Int = HookEnv.findClass(ICON_VIEW)
        ?.declaredConstructors
        ?.count { constructor ->
            constructor.intercept { chain ->
                val result = chain.proceed()
                val background = chain.thisObject.field(BACKGROUND_FIELD) as? ImageView
                if (background != null) Guard.run("LockscreenFingerprintGlass.view", Unit) { attach(background) }
                result
            } != null
        }
        ?: 0

    private fun attach(background: ImageView) {
        if (ViewGlassUnderlay.isAttached(background)) return
        ViewGlassUnderlay.attach(
            background,
            PrefKeys.LOCKSCREEN_FINGERPRINT,
            { GlassConfig.lockscreenFingerprintActive },
            { GlassConfig.lockscreenFingerprintTint },
            source = { (it as? ImageView)?.drawable?.let { image -> SolidShapes.of(image) } },
            imageContent = true,
            rimOnTop = true
        )
    }
}
