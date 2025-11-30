package com.pedrobneto.navigation.processor.library

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies

internal fun CodeGenerator.createModuleRegistryFile(
    packageName: String,
    fileName: String,
    directions: List<String>,
) {
    val imports = listOf(
        "com.pedrobneto.navigation.core.model.DirectionRegistry",
    ) + directions

    val template = """
package $packageName

${imports.sorted().joinToString(separator = "\n") { "import $it" }}

data object $fileName : DirectionRegistry(
    directions = listOf(
        ${
        directions.joinToString(separator = ",\n\t\t") { directionQualifiedName ->
            directionQualifiedName.substringAfterLast(".")
        }
    }
    )
)""".trimIndent()

    createNewFile(
        dependencies = Dependencies(true),
        packageName = packageName,
        fileName = fileName
    ).use { it.write(template.toByteArray()) }
}
