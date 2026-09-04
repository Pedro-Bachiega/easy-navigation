package com.pedrobneto.easy.navigation.buildlogic.util

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val targets: List<Target> = emptyList()
)
