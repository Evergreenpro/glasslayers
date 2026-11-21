package com.evergreenpro.glasslayers.domain.usecase

import com.evergreenpro.glasslayers.domain.repository.AppStateRepository

class IsOnboardingDoneUseCase(private val repository: AppStateRepository) {
    operator fun invoke(): Boolean = repository.isOnboardingDone()
}
