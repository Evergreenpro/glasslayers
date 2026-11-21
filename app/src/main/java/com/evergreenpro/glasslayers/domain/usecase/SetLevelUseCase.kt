package com.evergreenpro.glasslayers.domain.usecase

import com.evergreenpro.glasslayers.domain.model.GlassLevel
import com.evergreenpro.glasslayers.domain.repository.SettingsRepository
import kotlin.math.roundToInt

class SetLevelUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(level: GlassLevel, value: Float) =
        repository.setLevel(level, value.coerceIn(0f, level.max).roundToInt().toFloat())
}
