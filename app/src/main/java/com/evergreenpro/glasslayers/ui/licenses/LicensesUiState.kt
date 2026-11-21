package com.evergreenpro.glasslayers.ui.licenses

import com.evergreenpro.glasslayers.domain.model.License

data class LicensesUiState(
    val licenses: List<License> = emptyList()
)
