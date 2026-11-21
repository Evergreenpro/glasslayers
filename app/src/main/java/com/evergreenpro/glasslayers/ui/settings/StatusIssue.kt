package com.evergreenpro.glasslayers.ui.settings

import androidx.compose.ui.graphics.Color
import com.evergreenpro.glasslayers.domain.model.ModuleStatus
import com.evergreenpro.glasslayers.ui.theme.Negative
import com.evergreenpro.glasslayers.ui.theme.Warning

internal fun ModuleStatus.issueColor(): Color? = when {
    !frameworkActive || safeMode -> Negative
    restartNeeded -> Warning
    else -> null
}
