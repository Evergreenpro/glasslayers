package com.evergreenpro.glasslayers.di

import android.content.Context
import com.evergreenpro.glasslayers.data.repository.AppStateRepositoryImpl
import com.evergreenpro.glasslayers.data.repository.LicenseRepositoryImpl
import com.evergreenpro.glasslayers.data.repository.SettingsRepositoryImpl
import com.evergreenpro.glasslayers.data.repository.StatusRepositoryImpl
import com.evergreenpro.glasslayers.data.source.HookReportSource
import com.evergreenpro.glasslayers.data.source.LocalPrefsSource
import com.evergreenpro.glasslayers.data.source.ReportFileSource
import com.evergreenpro.glasslayers.data.source.XposedServiceSource
import com.evergreenpro.glasslayers.domain.repository.AppStateRepository
import com.evergreenpro.glasslayers.domain.repository.LicenseRepository
import com.evergreenpro.glasslayers.domain.repository.SettingsRepository
import com.evergreenpro.glasslayers.domain.repository.StatusRepository
import com.evergreenpro.glasslayers.domain.usecase.CheckRootUseCase
import com.evergreenpro.glasslayers.domain.usecase.CompleteOnboardingUseCase
import com.evergreenpro.glasslayers.domain.usecase.GetDefaultSettingsUseCase
import com.evergreenpro.glasslayers.domain.usecase.GetInstallTimeUseCase
import com.evergreenpro.glasslayers.domain.usecase.GetInstalledApkUseCase
import com.evergreenpro.glasslayers.domain.usecase.GetLicensesUseCase
import com.evergreenpro.glasslayers.domain.usecase.IsOnboardingDoneUseCase
import com.evergreenpro.glasslayers.domain.usecase.ObserveFrameworkUseCase
import com.evergreenpro.glasslayers.domain.usecase.ObserveHookReportUseCase
import com.evergreenpro.glasslayers.domain.usecase.ObserveModuleStatusUseCase
import com.evergreenpro.glasslayers.domain.usecase.ObserveSettingsUseCase
import com.evergreenpro.glasslayers.domain.usecase.RequestHookReportUseCase
import com.evergreenpro.glasslayers.domain.usecase.RestartSystemUiUseCase
import com.evergreenpro.glasslayers.domain.usecase.SaveReportUseCase
import com.evergreenpro.glasslayers.domain.usecase.SetBatterySaverActionUseCase
import com.evergreenpro.glasslayers.domain.usecase.SetLevelUseCase
import com.evergreenpro.glasslayers.domain.usecase.SetPerformanceModeUseCase
import com.evergreenpro.glasslayers.domain.usecase.SetStyleUseCase
import com.evergreenpro.glasslayers.domain.usecase.SetBatterySaverBackgroundUseCase
import com.evergreenpro.glasslayers.domain.usecase.SetThermalActionUseCase
import com.evergreenpro.glasslayers.domain.usecase.SetToggleUseCase

class AppContainer(context: Context) {

    private val app = context.applicationContext
    private val settingsRepository: SettingsRepository =
        SettingsRepositoryImpl(LocalPrefsSource(app), XposedServiceSource)
    private val statusRepository: StatusRepository =
        StatusRepositoryImpl(XposedServiceSource, HookReportSource(app), ReportFileSource(app))
    private val licenseRepository: LicenseRepository = LicenseRepositoryImpl(app)
    private val appStateRepository: AppStateRepository = AppStateRepositoryImpl(LocalPrefsSource(app))

    val observeSettings = ObserveSettingsUseCase(settingsRepository)
    val getDefaultSettings = GetDefaultSettingsUseCase(settingsRepository)
    val setToggle = SetToggleUseCase(settingsRepository)
    val setLevel = SetLevelUseCase(settingsRepository)
    val setStyle = SetStyleUseCase(settingsRepository)
    val setPerformanceMode = SetPerformanceModeUseCase(settingsRepository)
    val setBatterySaverAction = SetBatterySaverActionUseCase(settingsRepository)
    val setThermalAction = SetThermalActionUseCase(settingsRepository)
    val setBatterySaverBackground = SetBatterySaverBackgroundUseCase(settingsRepository)
    val observeModuleStatus = ObserveModuleStatusUseCase(statusRepository)
    val observeFramework = ObserveFrameworkUseCase(statusRepository)
    val observeHookReport = ObserveHookReportUseCase(statusRepository)
    val getInstallTime = GetInstallTimeUseCase(statusRepository)
    val requestHookReport = RequestHookReportUseCase(statusRepository)
    val restartSystemUi = RestartSystemUiUseCase(statusRepository)
    val getInstalledApk = GetInstalledApkUseCase(statusRepository)
    val saveReport = SaveReportUseCase(statusRepository)
    val getLicenses = GetLicensesUseCase(licenseRepository)
    val isOnboardingDone = IsOnboardingDoneUseCase(appStateRepository)
    val completeOnboarding = CompleteOnboardingUseCase(appStateRepository)
    val checkRoot = CheckRootUseCase(appStateRepository)
}
