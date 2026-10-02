plugins {
    id("plugin-multiplatform-library")
    id("plugin-compose")
}

kotlin {
    android.namespace = "com.pedrobneto.easy.navigation.fixture.consumer"
    sourceSets.commonMain.dependencies {
        implementation(projects.integration.feature)
        implementation(libs.jetbrains.serialization)
        implementation(libs.jetbrains.compose.ui)
    }
}
