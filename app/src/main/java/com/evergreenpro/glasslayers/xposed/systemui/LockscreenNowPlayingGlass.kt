package com.evergreenpro.glasslayers.xposed.systemui

import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import com.evergreenpro.glasslayers.data.source.PrefKeys
import com.evergreenpro.glasslayers.xposed.hook.HookEnv
import com.evergreenpro.glasslayers.xposed.hook.field
import com.evergreenpro.glasslayers.xposed.hook.hookAllAfter
import java.util.WeakHashMap

/**
 * Glass for the expanded Now Playing card on the lockscreen (the Pixel ambient music indication).
 *
 * The collapsed pill is left alone. Only the card backdrop that fades in on expand is touched:
 * an [ImageView] holding the album art and its tinted scrim. That backdrop becomes a live glass
 * surface, and the art plus scrim are faded by the tint level so the glass reads through them.
 * The round like / play action buttons get glass over their pastel background too. Text and the
 * album thumbnail are siblings of those views and keep their stock look.
 */
object LockscreenNowPlayingGlass {

    private const val CONTAINER = "com.google.android.systemui.ambientmusic.AmbientIndicationContainer"
    private const val BACKGROUND_FIELD = "mAmbientIndicationContainerBackground"
    private const val LIKE_FIELD = "mAmbientIndicationLikeContainer"
    private const val PLAY_FIELD = "mAmbientIndicationPlayContainer"
    private const val FALLBACK_RADIUS_DP = 32f

    private val shapes = WeakHashMap<View, GradientDrawable>()
    private val outline = Outline()

    fun install(): Int {
        val container = HookEnv.findClass(CONTAINER)
        var hooks = 0
        hooks += container.hookAllAfter("performExpandAnimation") { chain, _ -> attach(chain.thisObject) }
        hooks += container.hookAllAfter("updateColorScheme") { chain, _ -> attach(chain.thisObject) }
        return hooks
    }

    private fun attach(container: Any?) {
        attachCard(container.field(BACKGROUND_FIELD) as? ImageView)
        attachButton(container.field(LIKE_FIELD) as? View)
        attachButton(container.field(PLAY_FIELD) as? View)
    }

    // SystemUI recolours the button backgrounds with setColor, so the tint scales their alpha, not their colour.
    private fun attachButton(button: View?) {
        if (button == null || ViewGlassUnderlay.isAttached(button)) return
        ViewGlassUnderlay.attach(
            button,
            PrefKeys.LOCKSCREEN_NOW_PLAYING,
            { GlassConfig.lockscreenNowPlayingActive },
            { GlassConfig.lockscreenNowPlayingTint },
            rimOnTop = true
        )
    }

    private fun attachCard(backdrop: ImageView?) {
        if (backdrop == null || ViewGlassUnderlay.isAttached(backdrop)) return
        ViewGlassUnderlay.attach(
            backdrop,
            PrefKeys.LOCKSCREEN_NOW_PLAYING,
            { GlassConfig.lockscreenNowPlayingActive },
            { 1f },
            source = ::cardShape,
            rimOnTop = true,
            contentAlpha = { GlassConfig.lockscreenNowPlayingTint }
        )
    }

    private fun cardShape(view: View): Drawable {
        val drawable = shapes.getOrPut(view) { GradientDrawable().apply { setColor(Color.TRANSPARENT) } }
        val radius = cornerRadius(view)
        if (drawable.cornerRadius != radius) drawable.cornerRadius = radius
        return drawable
    }

    // The card is clipped by its parent's outline, so the glass takes its corners from there.
    private fun cornerRadius(view: View): Float {
        val parent = view.parent as? View
        outline.setEmpty()
        parent?.outlineProvider?.getOutline(parent, outline)
        return outline.radius.takeIf { it > 0f } ?: (FALLBACK_RADIUS_DP * view.resources.displayMetrics.density)
    }
}
