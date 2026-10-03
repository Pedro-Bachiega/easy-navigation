group = "io.github.pedro-bachiega"

// Provider-backed execution is tracked by configuration cache; do not cache a stale tag in build/.
version = providers.gradleProperty("VERSION_NAME").orElse(
    providers.exec {
        workingDir(repositoryRootForVersioning())
        commandLine("git", "describe")
        isIgnoreExitValue = true
    }.standardOutput.asText.map { it.trim().ifEmpty { "0.0.1" } }
).get()

fun Project.repositoryRootForVersioning(): File = rootDir.takeIf { it.resolve(".git").exists() }
    ?: rootDir.parentFile
