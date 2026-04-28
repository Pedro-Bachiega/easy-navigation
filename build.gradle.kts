plugins {
    id("jacoco")

    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.ksp) apply false
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
            substitute(module("io.github.pedro-bachiega:easy-navigation-library-processor"))
                .using(project(":processor"))
        }
    }
}
