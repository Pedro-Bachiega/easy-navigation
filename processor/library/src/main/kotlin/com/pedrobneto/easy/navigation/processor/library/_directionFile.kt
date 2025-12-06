package com.pedrobneto.easy.navigation.processor.library

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.pedrobneto.easy.navigation.core.annotation.Deeplink
import com.pedrobneto.easy.navigation.core.annotation.GlobalScope
import com.pedrobneto.easy.navigation.core.annotation.ParentDeeplink
import com.pedrobneto.easy.navigation.core.annotation.ParentRoute
import com.pedrobneto.easy.navigation.core.annotation.Route
import com.pedrobneto.easy.navigation.core.annotation.Scope
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import com.pedrobneto.easy.navigation.processor.library.model.Direction
import kotlin.reflect.KClass

private fun qualifiedNameForRoute(predicate: () -> KClass<*>): String? =
    runCatching { predicate().qualifiedName.orEmpty() }
        .getOrElse { exception ->
            "(.*ClassNotFoundException: )([a-zA-Z._]+)".toRegex()
                .find(exception.message.orEmpty())
                ?.groupValues
                ?.last()
        }.takeIf { it != NavigationRoute::class.qualifiedName }

@OptIn(KspExperimental::class)
internal fun CodeGenerator.createDirection(
    function: KSFunctionDeclaration,
    moduleName: String,
    isMultiplatformWithSingleTarget: Boolean
): Direction? {
    val ksFile = function.containingFile ?: return null
    val fileInCommonMainPath = ksFile.filePath.contains("commonMain")
    val moduleNameContainsCommonMain = moduleName.contains("commonMain")
    val isNotInCommonMain = (moduleNameContainsCommonMain && !fileInCommonMainPath) ||
            (!moduleNameContainsCommonMain && fileInCommonMainPath)

    val shouldSkip = !isMultiplatformWithSingleTarget && isNotInCommonMain
    if (shouldSkip) return null

    val routeQualifiedName = function.getAnnotationsByType(Route::class)
        .firstOrNull()
        ?.let { qualifiedNameForRoute(it::value) }
        ?: return null

    val parentRouteQualifiedName = function.getAnnotationsByType(ParentRoute::class)
        .firstOrNull()
        ?.let { qualifiedNameForRoute(it::value) }

    val deeplinks = function.getAnnotationsByType(Deeplink::class)
        .map(Deeplink::value)
        .toList()

    val parentDeeplink = function.getAnnotationsByType(ParentDeeplink::class)
        .firstOrNull()
        ?.value

    val scopes = function.getAnnotationsByType(Scope::class)
        .map(Scope::value)
        .toList()

    val isGlobal = function.getAnnotationsByType(GlobalScope::class)
        .firstOrNull() != null

    val routePackageName = routeQualifiedName.substringBeforeLast('.')
    val routeClassName = routeQualifiedName.substringAfterLast('.')
    val directionClassName = "${routeClassName}Direction"

    val parentRoutePackageName = parentRouteQualifiedName?.substringBeforeLast('.')
    val parentRouteClassName = parentRouteQualifiedName?.substringAfterLast('.')

    val routeParameterName = function.parameters.firstOrNull {
        it.type.resolve().declaration.qualifiedName?.asString() == routeQualifiedName
    }?.name?.asString()

    val direction = Direction(
        ksFile = ksFile,
        isGlobal = isGlobal,
        scopes = scopes,
        deeplinks = deeplinks,
        directionClassName = directionClassName,
        routePackageName = routePackageName,
        routeClassName = routeClassName,
        parentRoutePackageName = parentRoutePackageName,
        parentRouteClassName = parentRouteClassName,
        parentDeeplink = parentDeeplink,
        functionPackageName = function.packageName.asString(),
        functionName = function.simpleName.asString(),
        routeParameterName = routeParameterName,
    )

    return direction.also { it.createDirectionFile(this) }
}

private fun Direction.createDirectionFile(generator: CodeGenerator) {
    val parameter = routeParameterName?.let { "$it = route as $routeClassName" }.orEmpty()

    val (parentRouteClassParameter, parentRouteClassImport) =
        if (parentRoutePackageName != null && parentRouteClassName != null) {
            "parentRouteClass = $parentRouteClassName::class" to "$parentRoutePackageName.$parentRouteClassName"
        } else {
            null to null
        }

    val imports = listOfNotNull(
        "androidx.compose.runtime.Composable",
        "com.pedrobneto.easy.navigation.core.annotation.GlobalScope"
            .takeIf { isGlobal },
        "com.pedrobneto.easy.navigation.core.model.NavigationDeeplink"
            .takeIf { deeplinks.isNotEmpty() || parentDeeplink != null },
        "com.pedrobneto.easy.navigation.core.model.NavigationDirection",
        "com.pedrobneto.easy.navigation.core.model.NavigationRoute",
        "$functionPackageName.$functionName",
        parentRouteClassImport
    ).sorted()

    val deeplinksFormatted = "deeplinks = " + if (deeplinks.isEmpty()) {
        "emptyList()"
    } else {
        "listOf(\n\t\t${deeplinks.joinToString(",\n\t\t") { "NavigationDeeplink(\"$it\")" }}\n\t)"
    }

    val parentDeeplinkFormatted = parentDeeplink?.let {
        "parentDeeplink = NavigationDeeplink(\"$it\")"
    }

    val constructorParametersFormatted = listOfNotNull(
        "routeClass = $routeClassName::class",
        deeplinksFormatted,
        parentDeeplinkFormatted,
        parentRouteClassParameter,
    ).joinToString(",\n\t")

    val template = """
package $routePackageName

${imports.joinToString(separator = "\n") { "import $it" }}
${"\n@GlobalScope".takeIf { isGlobal }.orEmpty()}
internal data object $directionClassName : NavigationDirection(
    $constructorParametersFormatted
) {
    @Composable
    override fun Draw(route: NavigationRoute) {
        $functionName($parameter)
    }
}
""".trimIndent()

    generator.createNewFile(
        dependencies = Dependencies(true, ksFile),
        packageName = routePackageName,
        fileName = directionClassName
    ).use { it.write(template.toByteArray()) }
}