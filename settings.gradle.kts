@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    apply(from = "$rootDir/kmp-build-plugin/repositories.gradle.kts")
    val repositoryList: RepositoryHandler.() -> Unit by extra
    repositories(repositoryList)
}

dependencyResolutionManagement {
    apply(from = "$rootDir/kmp-build-plugin/repositories.gradle.kts")
    val repositoryList: RepositoryHandler.() -> Unit by extra
    repositories(repositoryList)

    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}

rootProject.name = "easy-navigation"

includeBuild("kmp-build-plugin")

include(
    ":core",
    ":easy-navigation-gradle-plugin",
    ":processor:application",
    ":processor:library",
    ":sample",
    ":test",
)
