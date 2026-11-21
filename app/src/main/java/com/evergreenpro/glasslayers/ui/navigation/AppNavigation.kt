package com.evergreenpro.glasslayers.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.evergreenpro.glasslayers.di.AppContainer
import com.evergreenpro.glasslayers.ui.about.AboutScreen
import com.evergreenpro.glasslayers.ui.licenses.LicensesScreen
import com.evergreenpro.glasslayers.ui.licenses.licensesViewModelFactory
import com.evergreenpro.glasslayers.ui.onboarding.OnboardingScreen
import com.evergreenpro.glasslayers.ui.onboarding.onboardingViewModelFactory
import com.evergreenpro.glasslayers.ui.settings.GlassPageScreen
import com.evergreenpro.glasslayers.ui.settings.SettingsScreen
import com.evergreenpro.glasslayers.ui.settings.settingsViewModelFactory
import com.evergreenpro.glasslayers.ui.status.StatusScreen
import com.evergreenpro.glasslayers.ui.status.statusViewModelFactory

@Composable
fun AppNavigation(container: AppContainer) {
    val backStack = rememberNavBackStack(if (container.isOnboardingDone()) HomeRoute else OnboardingRoute)
    val slide = with(LocalDensity.current) { SLIDE_DISTANCE.roundToPx() }

    fun back() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    NavDisplay(
        backStack = backStack,
        modifier = Modifier.fillMaxSize(),
        onBack = { back() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
            rememberTransitionInputDecorator()
        ),
        transitionSpec = { sharedAxisX(forward = true, slideDistance = slide) },
        popTransitionSpec = { sharedAxisX(forward = false, slideDistance = slide) },
        predictivePopTransitionSpec = { sharedAxisX(forward = false, slideDistance = slide) },
        entryProvider = entryProvider {
            entry<OnboardingRoute> {
                OnboardingScreen(
                    viewModel = viewModel(factory = onboardingViewModelFactory(container)),
                    onDone = {
                        backStack.add(HomeRoute)
                        backStack.remove(OnboardingRoute)
                    }
                )
            }
            entry<HomeRoute> {
                SettingsScreen(
                    viewModel = viewModel(factory = settingsViewModelFactory(container)),
                    onOpen = { page -> backStack.add(PageRoute(page)) },
                    onOpenStatus = { backStack.add(StatusRoute) },
                    onOpenAbout = { backStack.add(AboutRoute) }
                )
            }
            entry<PageRoute> { route ->
                GlassPageScreen(
                    page = route.page,
                    viewModel = viewModel(factory = settingsViewModelFactory(container)),
                    onBack = { back() }
                )
            }
            entry<StatusRoute> {
                StatusScreen(viewModel(factory = statusViewModelFactory(container)), onBack = { back() })
            }
            entry<AboutRoute> {
                AboutScreen(onBack = { back() }, onOpenLicenses = { backStack.add(LicensesRoute) })
            }
            entry<LicensesRoute> {
                LicensesScreen(viewModel(factory = licensesViewModelFactory(container)), onBack = { back() })
            }
        }
    )
}

private val SLIDE_DISTANCE = 30.dp
