package com.pedrobneto.easy.navigation.core

import kotlinx.serialization.json.Json

@PublishedApi
internal fun defaultNavigationJson(): Json = navigationJson

private val navigationJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}
