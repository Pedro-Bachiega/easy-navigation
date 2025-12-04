package com.pedrobneto.easy.navigation.processor.application

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies

internal fun CodeGenerator.createGlobalRegistryFile(
    registries: List<String>,
    directions: List<String>
) {
    val packageName = "com.pedrobneto.easy.navigation.registry"

    val fileName = "GlobalDirectionRegistry"
    val imports = listOf("com.pedrobneto.easy.navigation.core.model.DirectionRegistry") + registries + directions
    val registriesFormatted =
        "listOf(\n\t\t" + registries.joinToString(separator = ",\n\t\t") { registryQualifiedName ->
            registryQualifiedName.substringAfterLast(".")
        } + "\n\t).flatMap(DirectionRegistry::directions)"

    val directionsFormatted =
        "listOf(\n\t\t" + directions.joinToString(separator = ",\n\t\t") { directionQualifiedName ->
            directionQualifiedName.substringAfterLast(".")
        } + "\n\t)"

    val parameter = "directions = " + when {
        registries.isEmpty() && directions.isEmpty() -> "emptyList()"
        registries.isEmpty() -> directionsFormatted
        directions.isEmpty() -> registriesFormatted
        else -> "$registriesFormatted + $directionsFormatted"
    }

    val template = """
package $packageName

${imports.sorted().joinToString(separator = "\n") { "import $it" }}

data object $fileName : DirectionRegistry(
    $parameter
)""".trimIndent()

    createNewFile(
        dependencies = Dependencies(false),
        packageName = packageName,
        fileName = fileName
    ).use { it.write(template.toByteArray()) }
}
