@file:Suppress("UnstableApiUsage")

package com.pedrobneto.easy.navigation.buildlogic.android

import com.android.build.api.dsl.ApplicationExtension
import com.pedrobneto.easy.navigation.buildlogic.commonSetup
import com.pedrobneto.easy.navigation.buildlogic.setupOptIns
import com.pedrobneto.easy.navigation.buildlogic.util.androidApplication
import com.pedrobneto.easy.navigation.buildlogic.util.applyPlugins
import com.pedrobneto.easy.navigation.buildlogic.util.kotlinAndroid
import com.pedrobneto.easy.navigation.buildlogic.util.libs
import com.pedrobneto.easy.navigation.buildlogic.util.projectJavaTarget
import com.pedrobneto.easy.navigation.buildlogic.util.projectJavaVersionCode
import com.pedrobneto.easy.navigation.buildlogic.util.version
import org.gradle.api.Plugin
import org.gradle.api.Project

internal class ApplicationPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        applyPlugins(
            "android-application",
            "jetbrains-compose-compiler",
            "jetbrains-compose-kotlin",
            "jetbrains-serialization",
        )

        val kotlin = kotlinAndroid ?: return@with
        val android = androidApplication ?: return@with

        kotlin.jvmToolchain(projectJavaVersionCode)
        kotlin.compilerOptions { jvmTarget.set(projectJavaTarget) }
        kotlin.setupOptIns()
        setup(android)

        plugins.apply("plugin-lint")
        plugins.apply("plugin-test")
        plugins.apply("plugin-optimize")
    }

    private fun Project.setup(android: ApplicationExtension) = with(android) {
        // Exclusive Application Configurations
        compileSdk = libs.version("build-sdk-compile").toInt()
        buildToolsVersion = libs.version("build-tools")

        defaultConfig {
            minSdk = libs.version("build-sdk-min-sample").toInt()
            targetSdk = libs.version("build-sdk-target").toInt()

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

            versionCode = libs.version("build-version-code").toInt()
            versionName = libs.version("build-version-name")

            androidResources.localeFilters += listOf("en", "pt-rBR")
        }

        // TODO Config json for buildTypes and flavors
        buildTypes.maybeCreate("release").apply { isMinifyEnabled = false }
        buildTypes.maybeCreate("debug").apply {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }

        // Common Setup
        commonSetup()

        sourceSets {
            maybeCreate("main").java.srcDirs("src/main/kotlin")
            maybeCreate("test").java.srcDirs("src/test/kotlin")
            maybeCreate("androidTest").java.srcDirs("src/androidTest/kotlin")
            maybeCreate("androidTest").resources.srcDirs("src/androidTest/res")
        }
    }
}
