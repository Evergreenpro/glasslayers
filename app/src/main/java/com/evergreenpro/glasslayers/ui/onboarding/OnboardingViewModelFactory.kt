package com.evergreenpro.glasslayers.ui.onboarding

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.evergreenpro.glasslayers.di.AppContainer

fun onboardingViewModelFactory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        OnboardingViewModel(
            observeFramework = container.observeFramework,
            observeHookReport = container.observeHookReport,
            getInstalledApk = container.getInstalledApk,
            getInstallTime = container.getInstallTime,
            requestHookReport = container.requestHookReport,
            restartSystemUi = container.restartSystemUi,
            checkRoot = container.checkRoot,
            completeOnboarding = container.completeOnboarding
        )
    }
}
