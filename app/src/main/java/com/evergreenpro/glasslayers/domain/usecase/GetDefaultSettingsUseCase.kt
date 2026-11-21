package com.evergreenpro.glasslayers.domain.usecase

import com.evergreenpro.glasslayers.domain.model.GlassSettings
import com.evergreenpro.glasslayers.domain.repository.SettingsRepository

class GetDefaultSettingsUseCase(private val repository: SettingsRepository) {
    operator fun invoke(): GlassSettings = repository.defaults
}
