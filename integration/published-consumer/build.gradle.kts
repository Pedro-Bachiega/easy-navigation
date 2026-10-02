plugins {
    kotlin("multiplatform") version "2.4.20"
    kotlin("plugin.serialization") version "2.4.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20"
    id("io.github.pedro-bachiega.easy-navigation-library") version "0.0.1-compiler-ci"
}

kotlin {
    jvmToolchain(21)
    jvm()
    sourceSets.commonMain.dependencies {
        implementation("io.github.pedro-bachiega:easy-navigation-feature:0.0.1-compiler-ci")
        implementation("org.jetbrains.compose.runtime:runtime:1.12.0")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    }
}
