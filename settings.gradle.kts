@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    apply(from = "$rootDir/build-logic/repositories.gradle.kts")
    val repositoryList: RepositoryHandler.() -> Unit by extra
    includeBuild("easy-navigation-gradle-plugin") {
        name = "easy-navigation-gradle-plugin-build"
    }
    repositories(repositoryList)
}

dependencyResolutionManagement {
    apply(from = "$rootDir/build-logic/repositories.gradle.kts")
    val repositoryList: RepositoryHandler.() -> Unit by extra
    repositories(repositoryList)

    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}

rootProject.name = "easy-navigation"

includeBuild("build-logic")

include(
    ":core",
    ":easy-navigation-gradle-plugin",
    ":processor",
    ":sample:app",
    ":sample:target:android",
    ":sample:target:desktop",
    ":test",
)
