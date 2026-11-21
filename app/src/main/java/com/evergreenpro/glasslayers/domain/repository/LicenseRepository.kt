package com.evergreenpro.glasslayers.domain.repository

import com.evergreenpro.glasslayers.domain.model.License

interface LicenseRepository {
    suspend fun licenses(): List<License>
}
