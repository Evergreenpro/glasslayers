package com.evergreenpro.glasslayers.xposed.glass

import android.graphics.Path

internal class GlassSegment(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val path: Path,
    val radii: FloatArray
)
