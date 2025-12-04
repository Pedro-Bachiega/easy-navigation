package com.pedrobneto.easy.navigation.processor.library

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSFile
import com.pedrobneto.easy.navigation.processor.library.model.Direction
import java.util.Locale

private fun normalizeModuleName(name: String) = name.replace(Regex("_([a-zA-Z]+)$"), "")
    .replace(Regex("[-_]([a-zA-Z])")) { it.groupValues.last().uppercase() }
    .trimEnd('_')
    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

internal fun CodeGenerator.createRegistries(directionList: List<Direction>, moduleName: String) {
    val navigationClassPackage = "com.pedrobneto.easy.navigation.registry"

    val directionListByScope = directionList.flatMap(Direction::scopes)
        .associateWith { scope -> directionList.filter { scope in it.scopes } }
    val globalDirections = directionList.filter { it.scopes.isEmpty() }

    directionListByScope.forEach { (scope, directions) ->
        createModuleRegistryFile(
            scope = scope,
            packageName = navigationClassPackage,
            fileName = "${scope.capitalize(Locale.getDefault())}DirectionRegistry".trim(),
            sources = directions.map(Direction::ksFile),
            directions = directions.map(Direction::qualifiedDirectionName),
        )
    }

    if (globalDirections.isNotEmpty()) {
        createModuleRegistryFile(
            packageName = navigationClassPackage,
            fileName = "${normalizeModuleName(moduleName)}DirectionRegistry".trim(),
            sources = globalDirections.map(Direction::ksFile),
            directions = globalDirections.map(Direction::qualifiedDirectionName),
        )
    }
}

internal fun CodeGenerator.createModuleRegistryFile(
    scope: String? = null,
    packageName: String,
    fileName: String,
    sources: List<KSFile>,
    directions: List<String>,
) {
    val imports = listOfNotNull(
        "com.pedrobneto.easy.navigation.core.model.DirectionRegistry",
        scope?.let { "com.pedrobneto.easy.navigation.core.annotation.Scope" }
    ) + directions

    val template = """
package $packageName

${imports.sorted().joinToString(separator = "\n") { "import $it" }}
${scope?.let { "\n@Scope(\"$scope\")" }.orEmpty()}
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
