package com.evergreenpro.glasslayers.domain.usecase

import com.evergreenpro.glasslayers.domain.model.BatterySaverBackground
import com.evergreenpro.glasslayers.domain.repository.SettingsRepository

class SetBatterySaverBackgroundUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(background: BatterySaverBackground) = repository.setBatterySaverBackground(background)
}
