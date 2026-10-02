package com.pedrobneto.easy.navigation.buildlogic.util

import org.gradle.api.Project

val Project.versionName: String
    get() = providers.gradleProperty("VERSION_NAME").orElse(
        providers.exec {
            workingDir(rootDir)
            commandLine("git", "describe")
            isIgnoreExitValue = true
        }.standardOutput.asText.map { it.trim().ifEmpty { "0.0.1" } }
    ).get()
