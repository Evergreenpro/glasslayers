package com.evergreenpro.glasslayers.domain.usecase

import com.evergreenpro.glasslayers.domain.repository.StatusRepository

class GetInstalledApkUseCase(private val repository: StatusRepository) {
    operator fun invoke(): String = repository.installedApkPath()
}
