package com.pedrobneto.navigation.processor.library

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies

internal fun CodeGenerator.createDirectionFile(
    deeplinks: Array<String>,
    directionClassName: String,
    routePackageName: String,
    routeClassName: String,
    functionPackageName: String,
    functionName: String,
    routeParameterName: String?
): String {
    val parameter = routeParameterName?.let { "$it = route as $routeClassName" }.orEmpty()

    val imports = listOf(
        "androidx.compose.runtime.Composable",
        "com.pedrobneto.navigation.core.NavigationDirection",
        "com.pedrobneto.navigation.core.NavigationRoute",
        "$functionPackageName.$functionName"
    ).sorted()

    val deeplinksFormatted = if (deeplinks.isEmpty()) {
        "emptyList<String>()"
    } else {
        "listOf(\n\t\t${deeplinks.joinToString(",\n\t\t") { "\"$it\"" }}\n\t)"
    }

    val template = """
package $routePackageName

${imports.joinToString(separator = "\n") { "import $it" }}

internal data object $directionClassName : NavigationDirection(
    routeClass = $routeClassName::class,
    deeplinks = $deeplinksFormatted
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