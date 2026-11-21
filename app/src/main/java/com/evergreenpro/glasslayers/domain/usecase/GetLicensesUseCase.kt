package com.evergreenpro.glasslayers.domain.usecase

import com.evergreenpro.glasslayers.domain.model.License
import com.evergreenpro.glasslayers.domain.repository.LicenseRepository

class GetLicensesUseCase(private val repository: LicenseRepository) {
    suspend operator fun invoke(): List<License> = repository.licenses()
}
