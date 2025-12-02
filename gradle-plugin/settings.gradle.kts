@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    apply(from = "../kmp-build-plugin/repositories.gradle.kts")
    val repositoryList: RepositoryHandler.() -> Unit by extra
    repositories(repositoryList)

    dependencyResolutionManagement {
        repositories(repositoryList)

        repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
        versionCatalogs {
            create("libs") {
                from(files("../gradle/libs.versions.toml"))
            }
        }
    }
}
