package com.pedrobneto.easy.navigation.processor.library

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSFile

internal fun CodeGenerator.createModuleRegistryFile(
    packageName: String,
    fileName: String,
    sources: List<KSFile>,
    directions: List<String>,
) {
    val imports = listOf(
        "com.pedrobneto.easy.navigation.core.model.DirectionRegistry",
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
        dependencies = Dependencies(true, *sources.toTypedArray()),
        packageName = packageName,
        fileName = fileName
    ).use { it.write(template.toByteArray()) }
}
