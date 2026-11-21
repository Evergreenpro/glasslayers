package com.evergreenpro.glasslayers.data.repository

import com.evergreenpro.glasslayers.data.source.LocalPrefsSource
import com.evergreenpro.glasslayers.data.source.PrefKeys
import com.evergreenpro.glasslayers.data.source.RootShell
import com.evergreenpro.glasslayers.domain.repository.AppStateRepository

class AppStateRepositoryImpl(private val local: LocalPrefsSource) : AppStateRepository {

    override fun isOnboardingDone(): Boolean = local.boolean(PrefKeys.ONBOARDING_DONE, false)

    override fun completeOnboarding() {
        local.putBoolean(PrefKeys.ONBOARDING_DONE, true)
    }

    override suspend fun hasRoot(): Boolean = RootShell.run(ROOT_PROBE)
}

private const val ROOT_PROBE = "true"
