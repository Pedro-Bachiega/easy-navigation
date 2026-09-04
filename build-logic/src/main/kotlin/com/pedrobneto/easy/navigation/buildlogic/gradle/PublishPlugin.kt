package com.pedrobneto.easy.navigation.buildlogic.gradle

import com.pedrobneto.easy.navigation.buildlogic.util.applyPlugins
import com.pedrobneto.easy.navigation.buildlogic.util.artifactPrefixProperty
import com.pedrobneto.easy.navigation.buildlogic.util.libs
import com.pedrobneto.easy.navigation.buildlogic.util.publishing
import com.pedrobneto.easy.navigation.buildlogic.util.vanniktechPublishing
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get

internal class PublishPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        applyPlugins("java-gradle-plugin")

        plugins.apply(libs.findPlugin("vanniktech-publish").get().get().pluginId)

        with(vanniktechPublishing) {
            coordinates(
                artifactId = "${artifactPrefixProperty?.let { "$it-" }.orEmpty()}${target.name}",
            )
            publishToMavenCentral()
        }

        with(publishing) {
            repositories { mavenLocal() }
            publications {
                create<MavenPublication>("default") {
                    from(components["java"])
                }
            }
        }
    }
}
