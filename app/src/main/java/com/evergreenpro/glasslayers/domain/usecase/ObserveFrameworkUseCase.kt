package com.evergreenpro.glasslayers.domain.usecase

import com.evergreenpro.glasslayers.domain.model.FrameworkInfo
import com.evergreenpro.glasslayers.domain.repository.StatusRepository
import kotlinx.coroutines.flow.Flow

class ObserveFrameworkUseCase(private val repository: StatusRepository) {
    operator fun invoke(): Flow<FrameworkInfo?> = repository.framework
}
