plugins {
    id("plugin-multiplatform-library")
    id("plugin-compose")
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.jetbrains.serialization)
    alias(libs.plugins.easy.navigation.library)
    alias(libs.plugins.easy.navigation.application)
}

android.namespace = "com.pedrobneto.easy.navigation.sample"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.jetbrains.compose.material.core)
            implementation(libs.jetbrains.compose.material.icons.core)
            implementation(libs.jetbrains.compose.material.icons.extended)
            implementation(libs.jetbrains.compose.material3.core)
            implementation(libs.jetbrains.compose.material3.adaptive.core)
            implementation(libs.jetbrains.compose.material3.adaptive.navigation.suite)
            implementation(libs.jetbrains.compose.material3.window)
            implementation(libs.jetbrains.compose.navigation3.ui)
            implementation(libs.jetbrains.compose.ui)
            implementation(libs.jetbrains.compose.ui.tooling)
            implementation(libs.jetbrains.compose.ui.tooling.preview)
            implementation(libs.jetbrains.compose.ui.util)

            implementation(libs.jetbrains.serialization)

            implementation(projects.core)
        }
    }
}
