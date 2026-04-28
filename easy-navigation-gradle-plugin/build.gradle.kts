plugins {
    `maven-publish`
    id("java-gradle-plugin")
    `kotlin-dsl`
    alias(libs.plugins.gradle.publish)
}

val repositoryRoot = rootDir.takeIf { it.resolve("versioning.gradle.kts").exists() }
    ?: rootDir.parentFile

apply(from = repositoryRoot.resolve("versioning.gradle.kts"))

base {
    archivesName = "easy-navigation-library"
}

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

tasks.processResources {
    val pluginVersion = project.version.toString()
    inputs.property("pluginVersion", pluginVersion)
    filesMatching("easy-navigation-plugin.properties") {
        expand("pluginVersion" to pluginVersion)
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        if (name == "pluginMaven") {
            artifactId = "easy-navigation-library"
        }
    }
}

gradlePlugin {
    website = providers.gradleProperty("POM_URL")
    vcsUrl = providers.gradleProperty("POM_SCM_URL")

    plugins {
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
