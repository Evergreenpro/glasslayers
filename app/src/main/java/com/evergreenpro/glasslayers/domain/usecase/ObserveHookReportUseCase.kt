package com.evergreenpro.glasslayers.domain.usecase

import com.evergreenpro.glasslayers.domain.model.HookReport
import com.evergreenpro.glasslayers.domain.repository.StatusRepository
import kotlinx.coroutines.flow.Flow

class ObserveHookReportUseCase(private val repository: StatusRepository) {
    operator fun invoke(): Flow<HookReport?> = repository.hookReport
}
