package com.pedrobneto.easy.navigation.buildlogic.desktop

import com.pedrobneto.easy.navigation.buildlogic.setupOptIns
import com.pedrobneto.easy.navigation.buildlogic.util.applyPlugins
import com.pedrobneto.easy.navigation.buildlogic.util.kotlinMultiplatform
import com.pedrobneto.easy.navigation.buildlogic.util.projectJavaVersionCode
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

internal class ApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        applyPlugins(
            "jetbrains-kotlin-multiplatform",
            "jetbrains-compose-compiler",
            "jetbrains-compose-kotlin",
            "google-ksp",
        )

        kotlinExtension.jvmToolchain(projectJavaVersionCode)
        kotlinMultiplatform?.run {
            applyDefaultHierarchyTemplate()
            jvm()
            setupOptIns()
        }

        plugins.apply("plugin-lint")
        plugins.apply("plugin-optimize")
    }
}
