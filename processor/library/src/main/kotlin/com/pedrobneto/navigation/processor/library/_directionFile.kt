package com.pedrobneto.navigation.processor.library

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies

internal fun CodeGenerator.createDirectionFile(
    deeplinks: Array<String>,
    directionClassName: String,
    routePackageName: String,
    routeClassName: String,
    parentRoutePackageName: String?,
    parentRouteClassName: String?,
    functionPackageName: String,
    functionName: String,
    routeParameterName: String?
): String {
    val parameter = routeParameterName?.let { "$it = route as $routeClassName" }.orEmpty()

    val (parentRouteClassParameter, parentRouteClassImport) =
        if (parentRoutePackageName != null && parentRouteClassName != null) {
            "parentRouteClass = $parentRouteClassName::class" to "$parentRoutePackageName.$parentRouteClassName"
        } else {
            null to null
        }

    val imports = listOfNotNull(
        "androidx.compose.runtime.Composable",
        "com.pedrobneto.navigation.core.model.NavigationDeeplink",
        "com.pedrobneto.navigation.core.model.NavigationDirection",
        "com.pedrobneto.navigation.core.model.NavigationRoute",
        "$functionPackageName.$functionName",
        parentRouteClassImport
    ).sorted()

    val deeplinksFormatted = if (deeplinks.isEmpty()) {
        "emptyList()"
    } else {
        "listOf(\n\t\t${deeplinks.joinToString(",\n\t\t") { "NavigationDeeplink(\"$it\")" }}\n\t)"
    }

    val constructorParametersFormatted = listOfNotNull(
        "deeplinks = $deeplinksFormatted",
        "routeClass = $routeClassName::class",
        parentRouteClassParameter
    ).joinToString(",\n\t")

    val template = """
package $routePackageName

${imports.joinToString(separator = "\n") { "import $it" }}

internal data object $directionClassName : NavigationDirection(
    $constructorParametersFormatted
) {
    @Composable
    override fun Draw(route: NavigationRoute) {
        $functionName($parameter)
    }
}
""".trimIndent()

    createNewFile(
        dependencies = Dependencies(true),
        packageName = routePackageName,
        fileName = directionClassName
    ).use { it.write(template.toByteArray()) }

    return "$routePackageName.$directionClassName"
}