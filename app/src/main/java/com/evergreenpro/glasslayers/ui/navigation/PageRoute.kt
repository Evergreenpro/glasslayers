package com.evergreenpro.glasslayers.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.evergreenpro.glasslayers.ui.settings.GlassPage
import kotlinx.serialization.Serializable

@Serializable
data class PageRoute(val page: GlassPage) : NavKey
