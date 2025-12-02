package com.pedrobneto.navigation.processor.application

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies

internal fun CodeGenerator.createGlobalRegistryFile(packageName: String, registries: List<String>) {
    val fileName = "GlobalDirectionRegistry"
    val imports = listOf("com.pedrobneto.navigation.core.model.DirectionRegistry") + registries
    val registriesFormatted = if (registries.isEmpty()) {
        "emptyList()"
    } else {
        "listOf(\n\t\t" + registries.joinToString(separator = ",\n\t\t") { registryQualifiedName ->
            registryQualifiedName.substringAfterLast(".")
        } + "\n\t).flatMap(DirectionRegistry::directions)"
    }

    val template = """
package $packageName

${imports.sorted().joinToString(separator = "\n") { "import $it" }}

data object $fileName : DirectionRegistry(
    directions = $registriesFormatted
)""".trimIndent()

    createNewFile(
        dependencies = Dependencies(true),
        packageName = packageName,
        fileName = fileName
    ).use { it.write(template.toByteArray()) }
}
