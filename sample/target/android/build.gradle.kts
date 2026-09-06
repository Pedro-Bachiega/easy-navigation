plugins {
    id("plugin-android-application")
}

android {
    namespace = "com.pedrobneto.easy.navigation.sample.android"
}

dependencies {
    implementation(projects.sample.app)
}
