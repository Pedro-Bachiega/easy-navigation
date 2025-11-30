plugins {
    id("plugin-multiplatform-library")
    id("plugin-multiplatform-publish")
    id("plugin-compose")
}

android.namespace = "com.pedrobneto.navigation.core"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(compose.foundation)

            implementation(compose.material)
            implementation(compose.materialIconsExtended)
            implementation(compose.material3)
            implementation(compose.material3AdaptiveNavigationSuite)

            implementation(libs.jetbrains.compose.navigation3.ui)

            implementation(libs.jetbrains.serialization)

            implementation(libs.toolkit.arch.lumber)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
