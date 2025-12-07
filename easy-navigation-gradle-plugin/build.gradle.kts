plugins {
    id("java-gradle-plugin")
    alias(libs.plugins.jetbrains.kotlin.dsl)
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.gradle.publish)
}

apply(from = "$rootDir/versioning.gradle.kts")

kotlin { jvmToolchain(21) }

dependencies {
    compileOnly(gradleApi())
    implementation(libs.toolkit.arch.lumber)
    implementation(libs.plugin.jetbrains.kotlin.plugin)
}

sourceSets {
    main {
        java { srcDirs("src/main/java") }
        kotlin { srcDirs("src/main/kotlin") }
    }
}

gradlePlugin {
    website = providers.gradleProperty("POM_URL")
    vcsUrl = providers.gradleProperty("POM_SCM_URL")

    plugins {
        create("applicationPlugin") {
            id = "io.github.pedro-bachiega.easy-navigation-application"
            displayName = "Easy-Navigation Application Gradle Plugin"
            description =
                "Aggregate all module DirectionRegistries into a single GlobalDirectionRegistry :)"
            implementationClass = "com.pedrobneto.easy.navigation.plugin.ApplicationGradlePlugin"
            tags = listOf(
                "application",
                "easy-navigation",
                "easy-navigation-application",
                "easy",
                "google",
                "jetpack",
                "navigation",
                "navigation3",
            )
        }
        create("libraryPlugin") {
            id = "io.github.pedro-bachiega.easy-navigation-library"
            displayName = "Easy-Navigation Library Gradle Plugin"
            description = "Generate a contextual DirectionRegistry for your module :)"
            implementationClass = "com.pedrobneto.easy.navigation.plugin.LibraryGradlePlugin"
            tags = listOf(
                "easy-navigation",
                "easy-navigation-library",
                "easy",
                "google",
                "jetpack",
                "library",
                "navigation",
                "navigation3",
            )
        }
    }
}
