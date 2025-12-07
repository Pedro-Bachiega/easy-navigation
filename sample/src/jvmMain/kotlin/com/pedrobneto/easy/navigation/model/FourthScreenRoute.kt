package com.pedrobneto.easy.navigation.model

import com.pedrobneto.easy.navigation.core.model.NavigationDeeplink
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import kotlinx.serialization.Serializable

@Serializable
data class FourthScreenRoute(
    val title: String,
    val description: String,
    val pathVariable: String,
    val deeplink: NavigationDeeplink
) : NavigationRoute
