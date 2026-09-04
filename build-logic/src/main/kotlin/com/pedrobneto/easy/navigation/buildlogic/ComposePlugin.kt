package com.pedrobneto.easy.navigation.buildlogic

import com.pedrobneto.easy.navigation.buildlogic.util.androidApplication
import com.pedrobneto.easy.navigation.buildlogic.util.androidLibrary
import com.pedrobneto.easy.navigation.buildlogic.util.applyPlugins
import com.pedrobneto.easy.navigation.buildlogic.util.kotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal class ComposePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        applyPlugins(
            "jetbrains-compose-compiler",
            "jetbrains-compose-kotlin",
        )

        kotlinMultiplatform?.let { setupDefaultDependencies(it) }
        (androidApplication ?: androidLibrary)?.run { buildFeatures.compose = true }
            ?: return@with
    }

    private fun setupDefaultDependencies(kotlinExtension: KotlinMultiplatformExtension) {
        kotlinExtension.sourceSets.all {
            languageSettings {
                optIn("androidx.compose.material3.ExperimentalMaterial3Api")
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
            }
        }
    }
}
