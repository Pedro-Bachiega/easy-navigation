package com.pedrobneto.easy.navigation.buildlogic.multiplatform

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.pedrobneto.easy.navigation.buildlogic.setupOptIns
import com.pedrobneto.easy.navigation.buildlogic.util.Config
import com.pedrobneto.easy.navigation.buildlogic.util.Target
import com.pedrobneto.easy.navigation.buildlogic.util.applyPlugins
import com.pedrobneto.easy.navigation.buildlogic.util.getPluginId
import com.pedrobneto.easy.navigation.buildlogic.util.kotlinMultiplatform
import com.pedrobneto.easy.navigation.buildlogic.util.libs
import com.pedrobneto.easy.navigation.buildlogic.util.projectJavaTarget
import com.pedrobneto.easy.navigation.buildlogic.util.projectJavaVersionCode
import com.pedrobneto.easy.navigation.buildlogic.util.version
import kotlinx.serialization.json.Json
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

@OptIn(ExperimentalWasmDsl::class)
internal class LibraryPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        applyPlugins(
            "jetbrains-kotlin-multiplatform",
            "android-library",
            "jetbrains-serialization",
        )

        kotlinExtension.jvmToolchain(projectJavaVersionCode)
        target.setupTargets(kotlinMultiplatform ?: return@with)
        plugins.withId(libs.getPluginId("google-ksp")) {
            tasks.matching { it.name == "extractAndroidMainAnnotations" }.configureEach {
                dependsOn(
                    tasks.matching { task ->
                        task.name == "kspCommonMainKotlinMetadata" || task.name == "kspAndroidMain"
                    }
                )
            }
        }

        plugins.apply("plugin-lint")
        plugins.apply("plugin-optimize")
    }

    private fun Project.setupAndroid(
        android: KotlinMultiplatformAndroidLibraryTarget
    ) = with(android) {
        withJava()
        compilations.all {
            compileTaskProvider.configure { compilerOptions.jvmTarget.set(projectJavaTarget) }
        }

        compileSdk = libs.version("build-sdk-compile").toInt()
        buildToolsVersion = libs.version("build-tools")
        minSdk = libs.version("build-sdk-min-toolkit").toInt()

        androidResources.noCompress.add("")
        packaging.resources.excludes.add("META-INF/LICENSE")
        packaging.resources.pickFirsts.add("protobuf.meta")
        packaging.jniLibs.keepDebugSymbols.addAll(setOf("*/mips/*.so", "*/mips64/*.so"))

        listOf("consumer-rules.pro", "consumer-proguard-rules.pro")
            .firstOrNull { file(it).exists() }
            ?.let {
                optimization.consumerKeepRules.publish = true
                optimization.consumerKeepRules.file(it)
            }

        withHostTest {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    private fun Project.setupTargets(kotlin: KotlinMultiplatformExtension) = with(kotlin) {
        var targets = emptySet<Target>()

        val projectConfigFile = rootProject.file("project-config.json")
        if (projectConfigFile.exists()) {
            targets = Json.decodeFromString<Config>(projectConfigFile.readText()).targets.toSet()
        }

        val moduleConfigFile = project.file("module-config.json")
        if (moduleConfigFile.exists()) {
            val moduleConfig = Json.decodeFromString<Config>(moduleConfigFile.readText())
            if (moduleConfig.targets.isNotEmpty()) {
                targets = moduleConfig.targets.toSet()
            }
        }

        if (targets.isEmpty()) {
            throw GradleException(
                "No targets configured for project '$displayName'. " +
                        "Please create a 'project-config.json' at the project root " +
                        "or a 'module-config.json' in the module directory to specify the targets."
            )
        }

        setupOptIns()

        if (targets.contains(Target.Android)) {
            val android = (this as ExtensionAware)
                .extensions
                .getByType(KotlinMultiplatformAndroidLibraryTarget::class.java)
            setupAndroid(android)
        }
        if (targets.contains(Target.Jvm)) {
            jvm()
        }
        if (targets.contains(Target.Js)) {
            js(IR) {
                browser()
            }
        }
        if (targets.contains(Target.WasmJs)) {
            wasmJs {
                browser()
            }
        }
        if (targets.contains(Target.WasmWasi)) {
            wasmWasi()
        }
        if (targets.contains(Target.AndroidNativeArm32)) {
            androidNativeArm32()
        }
        if (targets.contains(Target.AndroidNativeArm64)) {
            androidNativeArm64()
        }
        if (targets.contains(Target.AndroidNativeX86)) {
            androidNativeX86()
        }
        if (targets.contains(Target.AndroidNativeX64)) {
            androidNativeX64()
        }
        if (targets.contains(Target.IosArm64)) {
            iosArm64()
        }
        if (targets.contains(Target.IosX64)) {
            iosX64()
        }
        if (targets.contains(Target.IosSimulatorArm64)) {
            iosSimulatorArm64()
        }
        if (targets.contains(Target.WatchosArm32)) {
            watchosArm32()
        }
        if (targets.contains(Target.WatchosArm64)) {
            watchosArm64()
        }
        if (targets.contains(Target.WatchosX64)) {
            watchosX64()
        }
        if (targets.contains(Target.WatchosSimulatorArm64)) {
            watchosSimulatorArm64()
        }
        if (targets.contains(Target.TvosArm64)) {
            tvosArm64()
        }
        if (targets.contains(Target.TvosX64)) {
            tvosX64()
        }
        if (targets.contains(Target.TvosSimulatorArm64)) {
            tvosSimulatorArm64()
        }
        if (targets.contains(Target.MacosX64)) {
            macosX64()
        }
        if (targets.contains(Target.MacosArm64)) {
            macosArm64()
        }
        if (targets.contains(Target.LinuxX64)) {
            linuxX64()
        }
        if (targets.contains(Target.LinuxArm64)) {
            linuxArm64()
        }
        if (targets.contains(Target.MingwX64)) {
            mingwX64()
        }

        applyDefaultHierarchyTemplate()
    }
}
