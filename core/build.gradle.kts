plugins {
    id("plugin-multiplatform-library")
    id("plugin-multiplatform-publish")
    id("plugin-compose")
}

android.namespace = "com.pedrobneto.easy.navigation.core"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.compose.navigation3.ui)
            implementation(libs.jetbrains.serialization)
            implementation(libs.toolkit.arch.lumber)
            implementation(projects.test)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
