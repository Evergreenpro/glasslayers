package com.evergreenpro.glasslayers.ui.settings

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.evergreenpro.glasslayers.R
import com.evergreenpro.glasslayers.ui.components.AppIcon
import com.evergreenpro.glasslayers.ui.components.LocalBackdrop
import com.evergreenpro.glasslayers.ui.components.PopAnimation
import com.evergreenpro.glasslayers.ui.components.liquid.InteractiveHighlight
import com.evergreenpro.glasslayers.ui.components.pressable
import com.evergreenpro.glasslayers.ui.components.rememberSpin
import com.evergreenpro.glasslayers.ui.components.stretchyGlass
import com.evergreenpro.glasslayers.ui.theme.Ink
import com.evergreenpro.glasslayers.ui.theme.Negative
import com.evergreenpro.glasslayers.ui.theme.OnInk
import com.evergreenpro.glasslayers.ui.theme.OnInkMuted
import kotlinx.coroutines.delay

@Composable
internal fun BoxScope.HomeMenu(
    visible: Boolean,
    issue: Color?,
    restart: RestartState,
    onStatus: () -> Unit,
    onRestart: () -> Unit,
    onAbout: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(visible, onDismiss)
    var restartSeen by remember { mutableStateOf(false) }
    LaunchedEffect(restart) {
        when (restart) {
            RestartState.RUNNING -> restartSeen = true
            RestartState.IDLE -> if (restartSeen) {
                restartSeen = false
                delay(DONE_HOLD_MS)
                onDismiss()
            }
            RestartState.FAILED -> restartSeen = false
        }
    }
    val pop = remember { PopAnimation() }
    LaunchedEffect(visible) { if (visible) pop.show() else pop.hide() }
    if (!visible && pop.hidden) return
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val tint = Ink.copy(alpha = PANEL_TINT)
    val animationScope = rememberCoroutineScope()
    val highlight = remember(animationScope) { InteractiveHighlight(animationScope) }
    if (visible) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { onDismiss() } }
        )
    }
    Column(
        Modifier
            .align(Alignment.TopEnd)
            .padding(top = statusTop + MENU_TOP, end = 12.dp)
            .width(MENU_WIDTH)
            .graphicsLayer {
                transformOrigin = TransformOrigin(1f, 0f)
                scaleX = pop.scaleX
                scaleY = pop.scaleY
                alpha = pop.alpha
            }
            .stretchyGlass(LocalBackdrop.current, MenuShape, tint, highlight, PANEL_BLUR)
            .clip(MenuShape)
            .padding(vertical = 6.dp)
    ) {
        MenuItem(R.drawable.ic_pulse, stringResource(R.string.status_title), issue) {
            onDismiss()
            onStatus()
        }
        RestartMenuItem(restart, onRestart)
        MenuItem(R.drawable.ic_info, stringResource(R.string.about_title), null) {
            onDismiss()
            onAbout()
        }
    }
}

@Composable
private fun MenuItem(@DrawableRes icon: Int, label: String, dot: Color?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(Role.Button, onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(icon, tint = OnInk, size = 20.dp)
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = OnInk, modifier = Modifier.weight(1f))
        if (dot != null) {
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .size(DOT_SIZE)
                    .background(dot, CircleShape)
            )
        }
    }
}

@Composable
private fun RestartMenuItem(state: RestartState, onClick: () -> Unit) {
    val running = state == RestartState.RUNNING
    val angle by rememberSpin(running, SPIN_MS)
    val tone by animateColorAsState(
        when {
            state == RestartState.FAILED -> Negative
            running -> OnInkMuted
            else -> OnInk
        },
        tween(TONE_MS),
        label = "restartMenuTone"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (running) Modifier else Modifier.pressable(Role.Button, onClick))
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(R.drawable.ic_refresh, Modifier.graphicsLayer { rotationZ = angle }, tint = tone, size = 20.dp)
        Spacer(Modifier.width(14.dp))
        Text(
            stringResource(
                when (state) {
                    RestartState.IDLE -> R.string.menu_restart_systemui
                    RestartState.RUNNING -> R.string.restart_running
                    RestartState.FAILED -> R.string.menu_restart_failed
                }
            ),
            style = MaterialTheme.typography.titleSmall,
            color = tone,
            modifier = Modifier.weight(1f)
        )
    }
}

private val MenuShape = RoundedCornerShape(24.dp)
private val MENU_WIDTH = 210.dp
private val MENU_TOP = 60.dp
private val PANEL_BLUR = 16.dp
private val DOT_SIZE = 8.dp
private const val PANEL_TINT = 0.62f
private const val SPIN_MS = 800
private const val TONE_MS = 300
private const val DONE_HOLD_MS = 450L
