plugins {
    id("plugin-multiplatform-library")
    id("plugin-compose")
    alias(libs.plugins.easy.navigation.library)
}

kotlin {
    android.namespace = "com.pedrobneto.easy.navigation.fixture.feature"
    sourceSets {
        commonMain {
            kotlin.setSrcDirs(listOf("destinations/shared"))
            dependencies {
                api(projects.core)
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
                implementation(libs.jetbrains.compose.ui.tooling.preview)
                implementation(libs.jetbrains.compose.ui.util)
                implementation(libs.jetbrains.serialization)
            }
        }
        val sharedDevice by creating {
            dependsOn(commonMain.get())
            kotlin.setSrcDirs(listOf("destinations/device"))
        }
        androidMain.get().dependsOn(sharedDevice)
        iosMain.get().dependsOn(sharedDevice)
        androidMain.get().kotlin.setSrcDirs(listOf("destinations/android"))
        iosMain.get().kotlin.setSrcDirs(listOf("destinations/apple"))
        jvmMain.get().kotlin.setSrcDirs(listOf("destinations/desktop"))
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.jetbrains.serialization)
            implementation(libs.jetbrains.compose.ui)
        }
    }
}
