package com.evergreenpro.glasslayers.domain.usecase

import com.evergreenpro.glasslayers.domain.repository.StatusRepository

class RequestHookReportUseCase(private val repository: StatusRepository) {
    operator fun invoke() = repository.requestHookReport()
}
