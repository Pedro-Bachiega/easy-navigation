package com.pedrobneto.navigation.model

import com.pedrobneto.navigation.core.model.NavigationRoute
import kotlinx.serialization.Serializable

@Serializable
data class SecondScreenRoute(val title: String, val description: String? = null) : NavigationRoute
