package com.evergreenpro.glasslayers.ui.settings

import com.evergreenpro.glasslayers.domain.model.GlassSettings
import com.evergreenpro.glasslayers.domain.model.ModuleStatus

data class SettingsUiState(
    val settings: GlassSettings = GlassSettings(),
    val defaults: GlassSettings = GlassSettings(),
    val status: ModuleStatus = ModuleStatus(),
    val restart: RestartState = RestartState.IDLE,
    val loaded: Boolean = false
)
