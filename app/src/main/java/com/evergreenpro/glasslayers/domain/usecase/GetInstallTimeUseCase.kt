package com.evergreenpro.glasslayers.domain.usecase

import com.evergreenpro.glasslayers.domain.repository.StatusRepository

class GetInstallTimeUseCase(private val repository: StatusRepository) {
    operator fun invoke(): Long = repository.installedAt()
}
