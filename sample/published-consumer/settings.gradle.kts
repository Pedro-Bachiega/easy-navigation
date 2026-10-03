pluginManagement {
    repositories {
        maven { url = uri(providers.gradleProperty("verificationRepository").get()) }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri(providers.gradleProperty("verificationRepository").get()) }
        google()
        mavenCentral()
    }
}

rootProject.name = "published-consumer"
