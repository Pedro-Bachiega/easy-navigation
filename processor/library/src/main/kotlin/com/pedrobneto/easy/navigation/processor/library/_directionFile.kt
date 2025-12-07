@file:OptIn(KspExperimental::class)

package com.pedrobneto.easy.navigation.processor.library

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.pedrobneto.easy.navigation.core.adaptive.AdaptivePane
import com.pedrobneto.easy.navigation.core.adaptive.ExtraPane
import com.pedrobneto.easy.navigation.core.adaptive.SinglePane
import com.pedrobneto.easy.navigation.core.annotation.Deeplink
import com.pedrobneto.easy.navigation.core.annotation.GlobalScope
import com.pedrobneto.easy.navigation.core.annotation.ParentDeeplink
import com.pedrobneto.easy.navigation.core.annotation.ParentRoute
import com.pedrobneto.easy.navigation.core.annotation.Route
import com.pedrobneto.easy.navigation.core.annotation.Scope
import com.pedrobneto.easy.navigation.processor.library.model.Direction
import com.pedrobneto.easy.navigation.processor.library.model.PaneStrategy
import com.pedrobneto.easy.navigation.processor.library.model.QualifiedName

private fun KSFunctionDeclaration.getPaneStrategy(logger: KSPLogger): PaneStrategy {
    val extraPaneHosts = getAnnotationsByType(ExtraPane::class).mapNotNull {
        val qualifiedName = QualifiedName(it::host) ?: return@mapNotNull null
        PaneStrategy.Extra.PaneHost(route = qualifiedName, ratio = it.ratio)
    }.toList()
    val extraPane = PaneStrategy.Extra(hosts = extraPaneHosts)
        .takeIf { extraPaneHosts.isNotEmpty() }

    val singlePane = getAnnotationsByType(SinglePane::class)
        .firstOrNull()
        ?.let { PaneStrategy.Single }

    val adaptivePane = getAnnotationsByType(AdaptivePane::class)
        .firstOrNull()
        .let { PaneStrategy.Adaptive(ratio = it?.ratio ?: 1f) }

    val foundList = listOfNotNull(extraPane, singlePane, adaptivePane)
    if (foundList.size > 1) {
        logger.warn(
            "Multiple pane strategies found for function ${this.simpleName.asString()}. " +
                    "Using ${foundList.first().javaClass.simpleName}.",
            this
        )
        return foundList.first()
    }

    return extraPane ?: singlePane ?: adaptivePane
}

internal fun CodeGenerator.createDirection(
    logger: KSPLogger,
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
        ?.let { QualifiedName.invoke(it::value) }
    if (routeQualifiedName == null) {
        logger.warn(
            "Missing @Route annotation for function ${function.simpleName.asString()}",
            function
        )
        return null
    }

    val parentRouteQualifiedName = function.getAnnotationsByType(ParentRoute::class)
        .firstOrNull()
        ?.let { QualifiedName.invoke(it::value) }

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

    val directionClassName = "${routeQualifiedName.className}Direction"

    val routeParameterName = function.parameters.firstOrNull {
        it.type.resolve().declaration.qualifiedName?.asString() == routeQualifiedName.raw
    }?.name?.asString()

    val direction = Direction(
        ksFile = ksFile,
        isGlobal = isGlobal,
        scopes = scopes,
        deeplinks = deeplinks,
        directionClassName = directionClassName,
        routePackageName = routeQualifiedName.packageName,
        routeClassName = routeQualifiedName.className,
        parentRoutePackageName = parentRouteQualifiedName?.packageName,
        parentRouteClassName = parentRouteQualifiedName?.className,
        parentDeeplink = parentDeeplink,
        paneStrategy = function.getPaneStrategy(logger),
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

    val paneStrategyImports =
        mutableListOf("com.pedrobneto.easy.navigation.core.adaptive.PaneStrategy")
    if (paneStrategy is PaneStrategy.Extra) {
        paneStrategyImports.addAll(paneStrategy.hosts.map { it.route.raw })
    }

    val paneStrategyFormatted = "paneStrategy = " + when (paneStrategy) {
        is PaneStrategy.Adaptive -> "PaneStrategy.Adaptive(" +
                "\n\t\tratio = ${paneStrategy.ratio}f" +
                "\n\t)"

        is PaneStrategy.Single -> "PaneStrategy.Single"
        is PaneStrategy.Extra -> {
            val hostListFormatted = paneStrategy.hosts.joinToString(separator = ",\n\t\t") {
                "PaneStrategy.Extra.PaneHost(route = ${it.route.className}::class, ratio = ${it.ratio}f)"
            }
            "PaneStrategy.Extra(" +
                    "\n\t\t$hostListFormatted," +
                    "\n\t)"
        }
    }

    val imports = listOfNotNull(
        "androidx.compose.runtime.Composable",
        "com.pedrobneto.easy.navigation.core.annotation.GlobalScope"
            .takeIf { isGlobal },
        "com.pedrobneto.easy.navigation.core.model.NavigationDeeplink"
            .takeIf { deeplinks.isNotEmpty() || parentDeeplink != null },
        "com.pedrobneto.easy.navigation.core.model.NavigationDirection",
        "com.pedrobneto.easy.navigation.core.model.NavigationRoute",
        "kotlinx.serialization.modules.PolymorphicModuleBuilder",
        "$functionPackageName.$functionName",
        parentRouteClassImport,
        *paneStrategyImports.toTypedArray()
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
        paneStrategyFormatted
    ).joinToString(",\n\t")

    val template = """
package $routePackageName

${imports.joinToString(separator = "\n") { "import $it" }}
${"\n@GlobalScope".takeIf { isGlobal }.orEmpty()}
internal data object $directionClassName : NavigationDirection(
    $constructorParametersFormatted
) {
    override fun register(builder: PolymorphicModuleBuilder<NavigationRoute>) =
        builder.subclass($routeClassName::class, $routeClassName.serializer())

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