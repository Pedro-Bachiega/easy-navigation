package com.pedrobneto.easy.navigation.buildlogic.util

import org.gradle.api.Project

internal fun Project.getMissingVariables(vararg name: String) = name.filterNot(::containsEnv)

internal fun Project.containsEnv(name: String): Boolean {
    val env = providers.environmentVariable(name).orNull ?: providers.gradleProperty(name).orNull
    if (env.isNullOrBlank()) {
        println("Missing Variable: $name")
        return false
    }

    return true
}

internal fun Project.requireAll(currentPluginName: String, vararg names: String) {
    names.forEach { pluginName ->
        if (plugins.hasPlugin(pluginName).not()) {
            error("To use $currentPluginName plugin you must implement $pluginName plugin")
        }
    }
}

internal fun Project.requireAny(currentPluginName: String, vararg names: String) {
    if (names.none { pluginName -> plugins.hasPlugin(pluginName).not() }) {
        error("To use $currentPluginName plugin you must implement one of [${names.joinToString()}]")
    }
}
