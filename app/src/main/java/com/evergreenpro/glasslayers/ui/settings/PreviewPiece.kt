package com.evergreenpro.glasslayers.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color

internal data class PreviewPiece(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val radii: List<Float>,
    val tint: Int = NO_TINT,
    val base: Int = NO_TINT,
    val fill: Color? = null,
    val accent: Boolean = false,
    val color: Color? = null,
    val anchor: Int = ANCHOR_NONE,
    val visible: Boolean = true,
    @param:DrawableRes val icon: Int? = null
)

internal const val NO_TINT = -1
internal const val PILL = -1f
internal const val ANCHOR_NONE = 0
internal const val ANCHOR_START = -1
internal const val ANCHOR_END = 1
