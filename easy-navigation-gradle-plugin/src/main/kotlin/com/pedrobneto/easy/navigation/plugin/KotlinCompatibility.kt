package com.pedrobneto.easy.navigation.plugin

internal fun isSupportedKotlinVersion(version: String): Boolean =
    Regex("2\\.4\\.[0-9]+").matches(version)
