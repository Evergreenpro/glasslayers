package com.evergreenpro.glasslayers.domain.usecase

import com.evergreenpro.glasslayers.domain.model.BatterySaverAction
import com.evergreenpro.glasslayers.domain.repository.SettingsRepository

class SetBatterySaverActionUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(action: BatterySaverAction) = repository.setBatterySaverAction(action)
}
