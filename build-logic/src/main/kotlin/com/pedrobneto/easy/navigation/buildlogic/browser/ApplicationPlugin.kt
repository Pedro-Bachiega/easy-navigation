package com.pedrobneto.easy.navigation.buildlogic.browser

import com.pedrobneto.easy.navigation.buildlogic.util.applyPlugins
import com.pedrobneto.easy.navigation.buildlogic.util.kotlinMultiplatform
import com.pedrobneto.easy.navigation.buildlogic.util.projectJavaVersionCode
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

@OptIn(ExperimentalWasmDsl::class)
internal class ApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        applyPlugins(
            "jetbrains-kotlin-multiplatform",
            "jetbrains-compose-compiler",
            "jetbrains-compose-kotlin",
        )

        kotlinExtension.jvmToolchain(projectJavaVersionCode)
        kotlinMultiplatform?.wasmJs {
            outputModuleName.set("BrowserApp")
            browser {
                commonWebpackConfig {
                    outputFileName = "browserApp.js"
                    devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                        static(project.projectDir.path)
                    }
                }
            }
            binaries.executable()
        }

        plugins.apply("plugin-lint")
        plugins.apply("plugin-optimize")
    }
}
