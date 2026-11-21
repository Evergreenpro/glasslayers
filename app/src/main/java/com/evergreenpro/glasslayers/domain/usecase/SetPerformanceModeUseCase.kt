package com.evergreenpro.glasslayers.domain.usecase

import com.evergreenpro.glasslayers.domain.model.PerformanceMode
import com.evergreenpro.glasslayers.domain.repository.SettingsRepository

class SetPerformanceModeUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(mode: PerformanceMode) = repository.setPerformance(mode)
}
