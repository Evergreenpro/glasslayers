package com.evergreenpro.glasslayers.xposed.systemui

import com.evergreenpro.glasslayers.xposed.glass.LiquidGlassStyle

data class SurfaceStyle(
    val blurRadiusPx: Float,
    val style: LiquidGlassStyle
)
