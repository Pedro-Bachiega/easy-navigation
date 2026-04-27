@file:Suppress("UnstableApiUsage")

rootProject.name = "easy-navigation-gradle-plugin"

pluginManagement {
    apply(from = "$rootDir/../kmp-build-plugin/repositories.gradle.kts")
    val repositoryList: RepositoryHandler.() -> Unit by extra
    repositories(repositoryList)
}

dependencyResolutionManagement {
    apply(from = "$rootDir/../kmp-build-plugin/repositories.gradle.kts")
    val repositoryList: RepositoryHandler.() -> Unit by extra
    repositories(repositoryList)

    versionCatalogs {
        register("libs") {
            from(files("$rootDir/../gradle/libs.versions.toml"))
        }
    }
}
