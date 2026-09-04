@file:Suppress("UnstableApiUsage")

package com.pedrobneto.easy.navigation.buildlogic

import com.pedrobneto.easy.navigation.buildlogic.util.androidApplication
import com.pedrobneto.easy.navigation.buildlogic.util.androidLibrary
import com.pedrobneto.easy.navigation.buildlogic.util.applyPlugins
import com.pedrobneto.easy.navigation.buildlogic.util.jacoco
import com.pedrobneto.easy.navigation.buildlogic.util.kover
import com.pedrobneto.easy.navigation.buildlogic.util.libs
import com.pedrobneto.easy.navigation.buildlogic.util.version
import org.gradle.api.Plugin
import org.gradle.api.Project

internal class TestPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        plugins.apply("jacoco")
        applyPlugins("jetbrains-kover")

        // Kover configuration
        kover.reports {
            filters {
                excludes {
                    androidGeneratedClasses()
                    annotatedBy("*KoverExcludes*", "*Composable*")
                }
            }
        }
        kover.currentProject {
            createVariant("combined") {
                val isAndroid = (androidApplication ?: androidLibrary) != null
                val flavors = androidApplication?.productFlavors ?: androidLibrary?.productFlavors
                flavors?.forEach { add("${it.name}Debug") }
                    ?: run { if (isAndroid) add("debug") }

                if (plugins.hasPlugin("plugin-desktop-application") || plugins.hasPlugin("plugin-multiplatform-library")) {
                    add("jvm")
                }
            }
        }

        // Jacoco configuration
        jacoco.toolVersion = target.libs.version("jacoco")

        // Regular Test configuration
        runCatching { setForApplication() }
        runCatching { setForLibrary() }
    }

    private fun Project.setForApplication() = with(androidApplication!!) {
        defaultConfig {
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        buildTypes.maybeCreate("debug").apply {
            enableAndroidTestCoverage = false
            enableUnitTestCoverage = false
        }
        buildTypes.maybeCreate("release").apply {
            enableAndroidTestCoverage = false
            enableUnitTestCoverage = false
        }

        testOptions {
            unitTests.all { it.enabled = false }
            unitTests.isIncludeAndroidResources = true
            unitTests.isReturnDefaultValues = true
            animationsDisabled = true
        }
    }

    private fun Project.setForLibrary() = with(androidLibrary!!) {
        defaultConfig {
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        buildTypes.maybeCreate("debug").apply {
            enableAndroidTestCoverage = false
            enableUnitTestCoverage = false
        }
        buildTypes.maybeCreate("release").apply {
            enableAndroidTestCoverage = false
            enableUnitTestCoverage = false
        }

        testOptions {
            unitTests.isIncludeAndroidResources = true
            unitTests.isReturnDefaultValues = true
            animationsDisabled = true
        }
    }
}
