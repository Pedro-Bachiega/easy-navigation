package com.pedrobneto.easy.navigation.model

import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import kotlinx.serialization.Serializable

@Serializable
data class ThirdScreenRoute(val title: String, val description: String) : NavigationRoute
