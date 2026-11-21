package com.evergreenpro.glasslayers.ui.settings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.util.lerp

@Composable
internal fun rememberPreviewPieces(page: GlassPage): List<PreviewPiece> {
    if (page != GlassPage.TRANSITIONS) return PreviewLayouts.of(page)
    val progress by rememberInfiniteTransition(label = "morph").animateFloat(
        0f,
        0f,
        infiniteRepeatable(
            keyframes {
                durationMillis = MORPH_CYCLE_MS
                0f at 0
                0f at HOLD_MS using FastOutSlowInEasing
                1f at HOLD_MS + GROW_MS
                1f at HOLD_MS + GROW_MS + HOLD_MS using FastOutSlowInEasing
                0f at MORPH_CYCLE_MS
            },
            RepeatMode.Restart
        ),
        label = "morphProgress"
    )
    val radius = lerp(TILE_HEIGHT / 2f, DIALOG_RADIUS, progress)
    return listOf(
        PreviewPiece(BOUNDS_X, BOUNDS_Y, BOUNDS_WIDTH, BOUNDS_HEIGHT, List(CORNERS) { 0f }, visible = false),
        PreviewPiece(
            lerp(TILE_X, DIALOG_X, progress),
            lerp(TILE_Y, DIALOG_Y, progress),
            lerp(TILE_WIDTH, DIALOG_WIDTH, progress),
            lerp(TILE_HEIGHT, DIALOG_HEIGHT, progress),
            List(CORNERS) { radius },
            tint = 0
        )
    )
}

private const val MORPH_CYCLE_MS = 3200
private const val HOLD_MS = 700
private const val GROW_MS = 900
private const val CORNERS = 4
private const val TILE_X = 24f
private const val TILE_Y = 116f
private const val TILE_WIDTH = 120f
private const val TILE_HEIGHT = 48f
private const val DIALOG_X = 40f
private const val DIALOG_Y = 8f
private const val DIALOG_WIDTH = 220f
private const val DIALOG_HEIGHT = 156f
private const val DIALOG_RADIUS = 30f
private const val BOUNDS_X = 24f
private const val BOUNDS_Y = 8f
private const val BOUNDS_WIDTH = 236f
private const val BOUNDS_HEIGHT = 156f
