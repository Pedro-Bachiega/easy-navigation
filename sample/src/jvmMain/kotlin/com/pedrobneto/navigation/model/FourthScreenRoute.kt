package com.pedrobneto.navigation.model

import com.pedrobneto.navigation.core.NavigationRoute
import kotlinx.serialization.Serializable

@Serializable
data class FourthScreenRoute(val title: String, val description: String, val pathVariable: String) :
    NavigationRoute
