package com.evergreenpro.glasslayers.domain.repository

import com.evergreenpro.glasslayers.domain.model.BatterySaverAction
import com.evergreenpro.glasslayers.domain.model.GlassLevel
import com.evergreenpro.glasslayers.domain.model.GlassSettings
import com.evergreenpro.glasslayers.domain.model.GlassSurface
import com.evergreenpro.glasslayers.domain.model.GlassToggle
import com.evergreenpro.glasslayers.domain.model.PerformanceMode
import com.evergreenpro.glasslayers.domain.model.StyleParam
import com.evergreenpro.glasslayers.domain.model.BatterySaverBackground
import com.evergreenpro.glasslayers.domain.model.ThermalAction
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<GlassSettings>
    val defaults: GlassSettings
    suspend fun setToggle(toggle: GlassToggle, enabled: Boolean)
    suspend fun setLevel(level: GlassLevel, value: Float)
    suspend fun setStyle(param: StyleParam, value: Float)
    suspend fun setStyleOverride(surface: GlassSurface, param: StyleParam, value: Float?)
    suspend fun setPerformance(mode: PerformanceMode)
    suspend fun setBatterySaverAction(action: BatterySaverAction)
    suspend fun setThermalAction(action: ThermalAction)
    suspend fun setBatterySaverBackground(background: BatterySaverBackground)
}
