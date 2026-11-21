package com.evergreenpro.glasslayers.domain.repository

interface AppStateRepository {
    fun isOnboardingDone(): Boolean
    fun completeOnboarding()
    suspend fun hasRoot(): Boolean
}
