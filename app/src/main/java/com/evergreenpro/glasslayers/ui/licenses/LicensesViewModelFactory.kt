package com.evergreenpro.glasslayers.ui.licenses

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.evergreenpro.glasslayers.di.AppContainer

fun licensesViewModelFactory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer { LicensesViewModel(container.getLicenses) }
}
