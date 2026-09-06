package com.pedrobneto.easy.navigation.buildlogic

import com.pedrobneto.easy.navigation.buildlogic.util.applyPlugins
import com.pedrobneto.easy.navigation.buildlogic.util.detekt
import com.pedrobneto.easy.navigation.buildlogic.util.ktLint
import com.pedrobneto.easy.navigation.buildlogic.util.libs
import com.pedrobneto.easy.navigation.buildlogic.util.projectJavaVersionName
import com.pedrobneto.easy.navigation.buildlogic.util.version
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.extensions.DetektReport
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

internal class LintPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        applyPlugins("lint-detekt", "lint-ktlint")

        // Detekt configuration
        dependencies.add(
            "detektPlugins",
            libs.findLibrary("lint-detekt-formatting").get()
        )

        with(detekt) {
            toolVersion = libs.version("detekt")
            parallel = true
            disableDefaultRuleSets = true
            buildUponDefaultConfig = true
            ignoreFailures = false

            autoCorrect = true
            allRules = false
            source.from("src/main")
            val detektConfig = target.rootDir.resolve("tools/detekt-config.yml")
            if (detektConfig.exists()) {
                config.setFrom(detektConfig)
            }
            val detektBaseline = target.rootDir.resolve("tools/detekt-baseline.xml")
            if (detektBaseline.exists()) {
                baseline = detektBaseline
            }
        }

        tasks.withType(Detekt::class.java).configureEach {
            jvmTarget = projectJavaVersionName
            reports.run { listOf(html, xml, txt, sarif, md) }.onEach { it.setup(target) }
        }
        tasks.withType(DetektCreateBaselineTask::class.java).configureEach {
            jvmTarget = projectJavaVersionName
        }

        // KtLint configuration
        ktLint.outputColorName.set("RED")
        ktLint.ignoreFailures.set(false)
    }

    private fun DetektReport.setup(target: Project) {
        required.set(true)
        outputLocation.set(File("${target.projectDir}/build/reports/detekt.${type.extension}"))
    }
}
