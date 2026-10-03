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
                implementation(libs.jetbrains.compose.ui)
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
