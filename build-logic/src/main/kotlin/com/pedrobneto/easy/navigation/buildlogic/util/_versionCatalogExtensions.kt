package com.pedrobneto.easy.navigation.buildlogic.util

import org.gradle.api.artifacts.VersionCatalog
import kotlin.jvm.optionals.getOrNull

internal fun VersionCatalog.version(alias: String) =
    findVersion(alias).getOrNull()?.requiredVersion
        ?: error("Unable to find version with alias: $alias")

internal val VersionCatalog.allDefinedDependencies: List<String>
    get() = libraryAliases.map(::findLibrary).mapNotNull { library ->
        library.getOrNull()?.get()?.run {
            versionConstraint.requiredVersion.takeUnless(String::isBlank)
                ?.let { version -> "${module.group}:${module.name}:$version" }
        }
    }.toList()

internal fun VersionCatalog.getPluginId(alias: String) = try {
    findPlugin(alias).get().get().pluginId
} catch (e: Exception) {
    throw IllegalArgumentException("No plugin found with alias: $alias", e)
}
