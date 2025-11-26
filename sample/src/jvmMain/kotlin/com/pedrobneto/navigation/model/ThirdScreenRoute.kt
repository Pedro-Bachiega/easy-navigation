package com.pedrobneto.navigation.model

import com.pedrobneto.navigation.core.NavigationRoute
import kotlinx.serialization.Serializable

@Serializable
data class ThirdScreenRoute(val title: String, val description: String) : NavigationRoute
