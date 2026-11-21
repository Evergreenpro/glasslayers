package com.evergreenpro.glasslayers.domain.usecase

import com.evergreenpro.glasslayers.domain.model.GlassToggle
import com.evergreenpro.glasslayers.domain.repository.SettingsRepository

class SetToggleUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(toggle: GlassToggle, enabled: Boolean) = repository.setToggle(toggle, enabled)
}
