plugins {
    id("plugin-multiplatform-library")
    id("plugin-multiplatform-publish")
    id("plugin-compose")
    id("plugin-test")
}

kotlin {
    android.namespace = "com.pedrobneto.easy.navigation.core"

    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.compose.material3.adaptive.navigation3)
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
