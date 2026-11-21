package com.evergreenpro.glasslayers.domain.usecase

import com.evergreenpro.glasslayers.domain.model.ThermalAction
import com.evergreenpro.glasslayers.domain.repository.SettingsRepository

class SetThermalActionUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(action: ThermalAction) = repository.setThermalAction(action)
}
