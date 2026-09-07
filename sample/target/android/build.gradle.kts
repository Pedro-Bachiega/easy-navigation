plugins {
    id("plugin-android-application")
}

android {
    namespace = "com.pedrobneto.easy.navigation.sample.android"
}

dependencies {
    implementation(libs.jetbrains.compose.ui)
    implementation(projects.sample.app)
}
