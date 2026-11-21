package com.evergreenpro.glasslayers.domain.usecase

import com.evergreenpro.glasslayers.domain.repository.StatusRepository

class RestartSystemUiUseCase(private val repository: StatusRepository) {
    suspend operator fun invoke(): Boolean = repository.restartSystemUi()
}
