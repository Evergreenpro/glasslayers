package com.evergreenpro.glasslayers.domain.usecase

import com.evergreenpro.glasslayers.domain.repository.AppStateRepository

class CompleteOnboardingUseCase(private val repository: AppStateRepository) {
    operator fun invoke() = repository.completeOnboarding()
}
