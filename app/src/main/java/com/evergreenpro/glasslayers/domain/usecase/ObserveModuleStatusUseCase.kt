package com.evergreenpro.glasslayers.domain.usecase

import com.evergreenpro.glasslayers.domain.model.ModuleStatus
import com.evergreenpro.glasslayers.domain.repository.StatusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveModuleStatusUseCase(private val repository: StatusRepository) {
    operator fun invoke(): Flow<ModuleStatus> = combine(repository.framework, repository.hookReport) { framework, report ->
        val active = framework != null
        ModuleStatus(
            frameworkActive = active,
            restartNeeded = active && (report == null || report.isStale(repository.installedApkPath(), repository.installedAt())),
            safeMode = active && report?.safeMode == true
        )
    }
}
