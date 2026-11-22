package com.drdisagree.glasslayer.xposed.systemui

import android.graphics.Color
import androidx.core.graphics.ColorUtils

internal object ThumbContrast {

    private const val MIN_CONTRAST = 1.6
    private const val STEPS = 8
    private const val MAX_CACHED = 32

    private val cache = HashMap<Int, Int>()

    fun againstThumb(color: Int): Int {
        cache[color]?.let { return it }
        if (cache.size >= MAX_CACHED) cache.clear()
        return darkened(color).also { cache[color] = it }
    }

    private fun darkened(color: Int): Int {
        val alpha = Color.alpha(color)
        if (alpha == 0) return color
        val opaque = ColorUtils.setAlphaComponent(color, 255)
        if (ColorUtils.calculateContrast(Color.WHITE, opaque) >= MIN_CONTRAST) return color
        var low = 0f
        var high = 1f
        repeat(STEPS) {
            val mid = (low + high) / 2f
            if (ColorUtils.calculateContrast(Color.WHITE, ColorUtils.blendARGB(opaque, Color.BLACK, mid)) >= MIN_CONTRAST) high = mid else low = mid
        }
        return ColorUtils.setAlphaComponent(ColorUtils.blendARGB(opaque, Color.BLACK, high), alpha)
    }
}
