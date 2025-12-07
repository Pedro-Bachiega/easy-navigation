package com.pedrobneto.easy.navigation.model

import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavigationRoute

@Serializable
data class DetailsRoute(val id: Long) : NavigationRoute

@Serializable
data object SettingsRoute : NavigationRoute
