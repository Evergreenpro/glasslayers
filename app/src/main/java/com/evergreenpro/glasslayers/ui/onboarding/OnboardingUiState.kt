package com.evergreenpro.glasslayers.ui.onboarding

import com.evergreenpro.glasslayers.domain.model.FrameworkInfo
import com.evergreenpro.glasslayers.domain.model.HookReport

data class OnboardingUiState(
    val framework: FrameworkInfo? = null,
    val report: HookReport? = null,
    val root: RootState = RootState.UNKNOWN,
    val restarting: Boolean = false,
    val installedApkPath: String = "",
    val installedAt: Long = 0L
) {
    val isScoped: Boolean
        get() = framework?.scope?.contains(SYSTEMUI) == true

    val isLoaded: Boolean
        get() = report != null && !report.isStale(installedApkPath, installedAt)

    val isReady: Boolean
        get() = framework != null && isScoped && isLoaded
}

private const val SYSTEMUI = "com.android.systemui"
