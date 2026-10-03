import org.gradle.api.publish.PublishingExtension

plugins {
    id("jacoco")

    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.compose.compiler) apply false
    alias(libs.plugins.jetbrains.compose.kotlin) apply false
    alias(libs.plugins.jetbrains.serialization) apply false
    alias(libs.plugins.dexcount) apply false
    alias(libs.plugins.lint.detekt) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.jetbrains.kotlin.multiplatform) apply false
    alias(libs.plugins.vanniktech.publish) apply false
}

subprojects {
    configurations.configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module("io.github.pedro-bachiega:easy-navigation-compiler-plugin"))
                .using(project(":compiler-plugin"))
        }
    }
}

// Used only by CI to exercise the real published artifact graph without Maven Local.
subprojects {
    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension> {
            repositories.maven {
                name = "Consumer"
                url = rootProject.layout.buildDirectory.dir("consumer-repository").get().asFile.toURI()
            }
        }
    }
}
