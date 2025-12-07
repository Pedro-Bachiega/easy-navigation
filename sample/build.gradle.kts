import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("plugin-desktop-application")
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.jetbrains.serialization)
    alias(libs.plugins.easy.navigation.library)
    alias(libs.plugins.easy.navigation.application)
}

kotlin {
    sourceSets {
        val commonMain by getting
        commonMain.dependencies {
            implementation(compose.desktop.currentOs)

            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.jetbrains.compose.material.core)
            implementation(libs.jetbrains.compose.material.icons.core)
            implementation(libs.jetbrains.compose.material.icons.extended)
            implementation(libs.jetbrains.compose.material3.core)
            implementation(libs.jetbrains.compose.material3.adaptive.core)
            implementation(libs.jetbrains.compose.material3.adaptive.navigation.suite)
            implementation(libs.jetbrains.compose.material3.window)
            implementation(libs.jetbrains.compose.navigation3.ui)
            implementation(libs.jetbrains.compose.ui.tooling)

            implementation(libs.toolkit.arch.lumber)

            implementation(libs.jetbrains.serialization)

            implementation(projects.core)
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.pedrobneto.navigation"
            packageVersion = "1.0.0"
        }
    }
}
