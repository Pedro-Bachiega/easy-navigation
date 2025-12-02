plugins {
    id("java-gradle-plugin")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.jetbrains.kotlin.dsl)
    alias(libs.plugins.jetbrains.serialization)
}

kotlin { jvmToolchain(21) }

dependencies {
    compileOnly(gradleApi())
    implementation(libs.plugin.jetbrains.kotlin.plugin)
}

sourceSets {
    main {
        java { srcDirs("src/main/java") }
        kotlin { srcDirs("src/main/kotlin") }
    }
}

gradlePlugin {
    plugins {
        create("easy-navigation-gradle-plugin") {
            id = "io.github.pedro-bachiega.easy-navigation"
            displayName = "Easy-Navigation Gradle Plugin"
            description = "\\o/"
            implementationClass = "com.pedrobneto.navigation.plugin.EasyNavigationGradlePlugin"
            tags = listOf(
                "easy-navigation",
                "easy",
                "navigation",
                "navigation3",
                "jetpack",
                "google"
            )
        }
    }
}
