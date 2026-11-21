package com.evergreenpro.glasslayers.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evergreenpro.glasslayers.domain.model.BatterySaverAction
import com.evergreenpro.glasslayers.domain.model.GlassLevel
import com.evergreenpro.glasslayers.domain.model.GlassSurface
import com.evergreenpro.glasslayers.domain.model.GlassToggle
import com.evergreenpro.glasslayers.domain.model.PerformanceMode
import com.evergreenpro.glasslayers.domain.model.StyleParam
import com.evergreenpro.glasslayers.domain.model.BatterySaverBackground
import com.evergreenpro.glasslayers.domain.model.ThermalAction
import com.evergreenpro.glasslayers.domain.usecase.GetDefaultSettingsUseCase
import com.evergreenpro.glasslayers.domain.usecase.ObserveModuleStatusUseCase
import com.evergreenpro.glasslayers.domain.usecase.ObserveSettingsUseCase
import com.evergreenpro.glasslayers.domain.usecase.RequestHookReportUseCase
import com.evergreenpro.glasslayers.domain.usecase.RestartSystemUiUseCase
import com.evergreenpro.glasslayers.domain.usecase.SetBatterySaverActionUseCase
import com.evergreenpro.glasslayers.domain.usecase.SetLevelUseCase
import com.evergreenpro.glasslayers.domain.usecase.SetPerformanceModeUseCase
import com.evergreenpro.glasslayers.domain.usecase.SetStyleUseCase
import com.evergreenpro.glasslayers.domain.usecase.SetBatterySaverBackgroundUseCase
import com.evergreenpro.glasslayers.domain.usecase.SetThermalActionUseCase
import com.evergreenpro.glasslayers.domain.usecase.SetToggleUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    observeSettings: ObserveSettingsUseCase,
    getDefaultSettings: GetDefaultSettingsUseCase,
    observeModuleStatus: ObserveModuleStatusUseCase,
    private val requestHookReport: RequestHookReportUseCase,
    private val setToggle: SetToggleUseCase,
    private val setLevel: SetLevelUseCase,
    private val setStyle: SetStyleUseCase,
    private val setPerformanceMode: SetPerformanceModeUseCase,
    private val setBatterySaverAction: SetBatterySaverActionUseCase,
    private val setThermalAction: SetThermalActionUseCase,
    private val setBatterySaverBackground: SetBatterySaverBackgroundUseCase,
    private val restartSystemUi: RestartSystemUiUseCase
) : ViewModel() {

    private val defaults = getDefaultSettings()
    private val restart = MutableStateFlow(RestartState.IDLE)

    val uiState: StateFlow<SettingsUiState> = combine(
        observeSettings(),
        observeModuleStatus(),
        restart
    ) { settings, status, restartState ->
        SettingsUiState(
            settings = settings,
            defaults = defaults,
            status = status,
            restart = restartState,
            loaded = true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState(defaults = defaults))

    private var safeHandled = false

    init {
        viewModelScope.launch {
            uiState.collect { state ->
                if (!state.loaded) return@collect
                if (!state.status.safeMode) {
                    safeHandled = false
                    return@collect
                }
                if (safeHandled) return@collect
                safeHandled = true
                if (state.settings.isOn(GlassToggle.ENABLED)) setToggle(GlassToggle.ENABLED, false)
            }
        }
    }

    fun onResume() {
        requestHookReport()
    }

    fun onToggle(toggle: GlassToggle, enabled: Boolean) {
        viewModelScope.launch { setToggle(toggle, enabled) }
    }

    fun onLevelChange(level: GlassLevel, value: Float) {
        viewModelScope.launch { setLevel(level, value) }
    }

    fun onLevelReset(level: GlassLevel) {
        onLevelChange(level, defaults.level(level))
    }

    fun onStyleChange(surface: GlassSurface?, param: StyleParam, value: Float) {
        viewModelScope.launch { setStyle(surface, param, value) }
    }

    fun onStyleReset(surface: GlassSurface?, param: StyleParam) {
        viewModelScope.launch { setStyle(surface, param, null) }
    }

    fun onPerformance(mode: PerformanceMode) {
        viewModelScope.launch { setPerformanceMode(mode) }
    }

    fun onBatterySaver(action: BatterySaverAction) {
        viewModelScope.launch { setBatterySaverAction(action) }
    }

    fun onThermal(action: ThermalAction) {
        viewModelScope.launch { setThermalAction(action) }
    }

    fun onBatterySaverBackground(background: BatterySaverBackground) {
        viewModelScope.launch { setBatterySaverBackground(background) }
    }

    fun onRestart() {
        if (restart.value == RestartState.RUNNING) return
        restart.value = RestartState.RUNNING
        viewModelScope.launch {
            if (restartSystemUi()) {
                delay(RESTART_SETTLE_MS)
                restart.value = RestartState.IDLE
            } else {
                restart.value = RestartState.FAILED
            }
        }
    }
}

private const val STOP_TIMEOUT_MS = 5_000L
private const val RESTART_SETTLE_MS = 15_000L
