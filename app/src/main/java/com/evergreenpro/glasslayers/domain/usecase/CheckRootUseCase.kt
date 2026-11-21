package com.evergreenpro.glasslayers.domain.usecase

import com.evergreenpro.glasslayers.domain.repository.AppStateRepository

class CheckRootUseCase(private val repository: AppStateRepository) {
    suspend operator fun invoke(): Boolean = repository.hasRoot()
}
