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
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.materialIconsExtended)
            implementation(compose.material3)
            implementation(compose.material3AdaptiveNavigationSuite)

            implementation(compose.uiTooling)

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
