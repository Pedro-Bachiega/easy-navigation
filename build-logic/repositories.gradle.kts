val repositoryList: RepositoryHandler.() -> Unit = {
    google()
    mavenCentral()
    mavenLocal()
    gradlePluginPortal()
}

extra["repositoryList"] = repositoryList
