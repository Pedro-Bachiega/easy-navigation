import org.apache.tools.ant.taskdefs.condition.Os

group = runCatching {
    providers.gradleProperty("GROUP").get().takeIf { it.isNotEmpty() }
}.getOrDefault("io.github.pedro-bachiega")

version = runCatching {
    providers.gradleProperty("VERSION_NAME").get().takeIf { it.isNotEmpty() }
}.getOrNull() ?: runGitCommand(
    fileName = "version-name.txt",
    command = "git describe",
    default = "0.0.1",
)

private val String.execute: Process
    get() = Runtime.getRuntime().exec(this.split(" ").toTypedArray())

private val Process.text: String
    get() = inputStream.bufferedReader().readText().trim()

private val String.executeWithText: String?
    get() {
        val process = execute
        if (process.waitFor() != 0) return null
        return process.text
    }

private fun Project.runGitCommand(fileName: String, command: String, default: String): String {
    val file = File("$rootDir/build", fileName)
    if (file.exists()) return file.readText().trim()
    if (validateGit().not()) return default

    val output = command.executeWithText
    return if (output.isNullOrBlank()) {
        default
    } else {
        file.parentFile.mkdirs()
        output.also(file::writeText)
    }
}

private fun validateGit(): Boolean {
    val command = if (Os.isFamily(Os.FAMILY_WINDOWS)) "git --version" else "whereis git"
    val output = command.executeWithText
    return (output.isNullOrEmpty() || output == "git:").not()
}
