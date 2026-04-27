package com.pedrobneto.easy.navigation.processor.library

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.pedrobneto.easy.navigation.core.adaptive.AdaptivePane
import com.pedrobneto.easy.navigation.core.adaptive.ExtraPane
import com.pedrobneto.easy.navigation.core.adaptive.SinglePane
import com.pedrobneto.easy.navigation.core.annotation.Deeplink
import com.pedrobneto.easy.navigation.core.annotation.GlobalScope
import com.pedrobneto.easy.navigation.core.annotation.ParentDeeplink
import com.pedrobneto.easy.navigation.core.annotation.ParentRoute
import com.pedrobneto.easy.navigation.core.annotation.Route
import com.pedrobneto.easy.navigation.core.annotation.Scope
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import com.pedrobneto.easy.navigation.processor.library.model.Direction
import com.pedrobneto.easy.navigation.processor.library.model.PaneStrategy
import com.pedrobneto.easy.navigation.processor.library.model.QualifiedName
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo

private val composableAnnotation = ClassName("androidx.compose.runtime", "Composable")
private const val SERIALIZABLE_ANNOTATION = "kotlinx.serialization.Serializable"
private val navigationDirection = ClassName("com.pedrobneto.easy.navigation.core.model", "NavigationDirection")
private val navigationRoute = ClassName("com.pedrobneto.easy.navigation.core.model", "NavigationRoute")
private val navigationDeeplink = ClassName("com.pedrobneto.easy.navigation.core.model", "NavigationDeeplink")
private val paneStrategyClass = ClassName("com.pedrobneto.easy.navigation.core.adaptive", "PaneStrategy")
private val polymorphicModuleBuilder = ClassName("kotlinx.serialization.modules", "PolymorphicModuleBuilder")

internal fun KSFunctionDeclaration.extractDirection(
    logger: KSPLogger,
    moduleName: String,
    isMultiplatformWithSingleTarget: Boolean
): Direction? {
    val ksFile = containingFile ?: return null
    if (shouldSkipSourceSet(moduleName, ksFile.filePath, isMultiplatformWithSingleTarget)) return null

    if (!hasAnnotation(composableAnnotation)) {
        logger.error("Navigation destination ${simpleName.asString()} must be annotated with @Composable.", this)
        return null
    }

    val routeType = annotationsByType(Route::class)
        .firstOrNull()
        ?.typeArgument("value")

    if (routeType == null) {
        logger.error("Navigation destination ${simpleName.asString()} must declare @Route(...).", this)
        return null
    }

    if (!routeType.isNavigationRoute()) {
        logger.error("@Route value ${routeType.qualifiedName().orEmpty()} must implement NavigationRoute.", this)
        return null
    }

    if (!routeType.declaration.hasAnnotation(SERIALIZABLE_ANNOTATION)) {
        logger.error("@Route value ${routeType.qualifiedName().orEmpty()} must be annotated with @Serializable.", this)
        return null
    }

    val routeQualifiedName = routeType.qualifiedName()
        ?: return logger.errorAndNull("Could not resolve @Route value for ${simpleName.asString()}.", this)
    val routeSimpleName = routeType.declaration.simpleName.asString()

    val routeParameterName = findRouteParameterName(routeType, logger) ?: return null
    val parentRouteType = annotationsByType(ParentRoute::class)
        .firstOrNull()
        ?.typeArgument("value")

    return Direction(
        ksFile = ksFile,
        isGlobal = hasAnnotation(GlobalScope::class),
        scopes = annotationsByType(Scope::class).mapNotNull { it.stringArgument("value") },
        deeplinks = annotationsByType(Deeplink::class).mapNotNull { it.stringArgument("value") },
        directionClassName = "${routeSimpleName}Direction",
        routeQualifiedName = routeQualifiedName,
        routePackageName = routeType.declaration.packageName.asString(),
        routeClassName = routeSimpleName,
        parentRouteQualifiedName = parentRouteType?.qualifiedName(),
        parentRoutePackageName = parentRouteType?.declaration?.packageName?.asString(),
        parentRouteClassName = parentRouteType?.declaration?.simpleName?.asString(),
        parentDeeplink = annotationsByType(ParentDeeplink::class).firstOrNull()?.stringArgument("value"),
        paneStrategy = getPaneStrategy(logger) ?: return null,
        functionPackageName = packageName.asString(),
        functionName = simpleName.asString(),
        routeParameterName = routeParameterName,
    )
}

internal fun CodeGenerator.createDirection(
    logger: KSPLogger,
    function: KSFunctionDeclaration,
    moduleName: String,
    isMultiplatformWithSingleTarget: Boolean
): Direction? =
    function.extractDirection(logger, moduleName, isMultiplatformWithSingleTarget)
        ?.also { it.createDirectionFile(this) }

internal fun Direction.createDirectionFile(generator: CodeGenerator) {
    val routeClass = ClassName.bestGuess(routeQualifiedName)
    val function = ClassName(functionPackageName, functionName)
    val type = TypeSpec.objectBuilder(directionClassName)
        .addModifiers(KModifier.INTERNAL, KModifier.DATA)
        .superclass(navigationDirection)
        .addSuperclassConstructorParameter("routeClass = %T::class", routeClass)
        .addSuperclassConstructorParameter("deeplinks = %L", deeplinksCode())
        .apply {
            if (isGlobal) addAnnotation(ClassName("com.pedrobneto.easy.navigation.core.annotation", "GlobalScope"))
            parentDeeplink?.let {
                addSuperclassConstructorParameter("parentDeeplink = %T(%S)", navigationDeeplink, it)
            }
            parentRouteQualifiedName?.let {
                addSuperclassConstructorParameter(
                    "parentRouteClass = %T::class",
                    ClassName.bestGuess(it)
                )
            }
            addSuperclassConstructorParameter("paneStrategy = %L", paneStrategy.code())
        }
        .addFunction(registerFunction(routeClass))
        .addFunction(drawFunction(function))
        .build()

    val file = FileSpec.builder(routePackageName, directionClassName)
        .addType(type)
        .build()

    file.writeTo(
        codeGenerator = generator,
        dependencies = Dependencies(aggregating = false, ksFile)
    )
}

private fun KSFunctionDeclaration.findRouteParameterName(routeType: KSType, logger: KSPLogger): String? {
    if (parameters.isEmpty()) return ""

    val matchingParameters = parameters.filter {
        it.type.resolve().qualifiedName() == routeType.qualifiedName()
    }

    return when {
        matchingParameters.size == 1 -> matchingParameters.first().name?.asString().orEmpty()
        matchingParameters.isEmpty() -> {
            logger.error(
                "Navigation destination ${simpleName.asString()} has parameters, but none match " +
                        "@Route(${routeType.qualifiedName().orEmpty()}::class).",
                this
            )
            null
        }

        else -> {
            logger.error("Navigation destination ${simpleName.asString()} has multiple route parameters.", this)
            null
        }
    }
}

private fun KSFunctionDeclaration.getPaneStrategy(logger: KSPLogger): PaneStrategy? {
    val extraPaneHosts = annotationsByType(ExtraPane::class).mapNotNull {
        val host = it.typeArgument("host")?.qualifiedName()?.let(::QualifiedName)
        val ratio = it.floatArgument("ratio") ?: 0.5f
        host?.let { qualifiedName -> PaneStrategy.Extra.PaneHost(route = qualifiedName, ratio = ratio) }
    }
    val extraPane = PaneStrategy.Extra(hosts = extraPaneHosts).takeIf { extraPaneHosts.isNotEmpty() }
    val singlePane = PaneStrategy.Single.takeIf { hasAnnotation(SinglePane::class) }
    val adaptivePane = annotationsByType(AdaptivePane::class)
        .firstOrNull()
        ?.let { PaneStrategy.Adaptive(ratio = it.floatArgument("ratio") ?: 1f) }

    val foundList = listOfNotNull(extraPane, singlePane, adaptivePane)
    if (foundList.size > 1) {
        logger.error(
            "Navigation destination ${simpleName.asString()} declares multiple pane strategies. " +
                    "Use only one of @AdaptivePane, @SinglePane, or @ExtraPane.",
            this
        )
        return null
    }

    return extraPane ?: singlePane ?: adaptivePane ?: PaneStrategy.Adaptive()
}

private fun KSFunctionDeclaration.shouldSkipSourceSet(
    moduleName: String,
    filePath: String,
    isMultiplatformWithSingleTarget: Boolean
): Boolean {
    val fileInCommonMainPath = filePath.contains("commonMain")
    val moduleNameContainsCommonMain = moduleName.contains("commonMain")
    val isNotInCommonMain = (moduleNameContainsCommonMain && !fileInCommonMainPath) ||
            (!moduleNameContainsCommonMain && fileInCommonMainPath)

    return !isMultiplatformWithSingleTarget && isNotInCommonMain
}

private fun KSFunctionDeclaration.hasAnnotation(annotation: ClassName): Boolean =
    annotations.any { it.annotationType.resolve().declaration.qualifiedName?.asString() == annotation.canonicalName }

private fun KSType.isNavigationRoute(): Boolean =
    qualifiedName() == NavigationRoute::class.qualifiedName ||
            (declaration as? KSClassDeclaration)
                ?.getAllSuperTypes()
                ?.any { it.qualifiedName() == NavigationRoute::class.qualifiedName } == true

private fun com.google.devtools.ksp.symbol.KSAnnotation.floatArgument(name: String): Float? =
    arguments.firstOrNull { it.name?.asString() == name }?.value as? Float

private fun KSPLogger.errorAndNull(message: String, symbol: KSFunctionDeclaration): Nothing? {
    error(message, symbol)
    return null
}

private fun Direction.deeplinksCode(): CodeBlock {
    if (deeplinks.isEmpty()) return CodeBlock.of("emptyList()")
    return CodeBlock.builder()
        .add("listOf(")
        .indent()
        .apply {
            deeplinks.forEachIndexed { index, deeplink ->
                add("\n%T(%S)", navigationDeeplink, deeplink)
                if (index < deeplinks.lastIndex) add(",")
            }
        }
        .unindent()
        .add("\n)")
        .build()
}

private fun PaneStrategy.code(): CodeBlock = when (this) {
    is PaneStrategy.Adaptive -> CodeBlock.of("%T.Adaptive(ratio = %Lf)", paneStrategyClass, ratio)
    is PaneStrategy.Single -> CodeBlock.of("%T.Single", paneStrategyClass)
    is PaneStrategy.Extra -> CodeBlock.builder()
        .add("%T.Extra(", paneStrategyClass)
        .indent()
        .apply {
            hosts.forEachIndexed { index, host ->
                add(
                    "\n%T.Extra.PaneHost(route = %T::class, ratio = %Lf)",
                    paneStrategyClass,
                    ClassName.bestGuess(host.route.raw),
                    host.ratio
                )
                if (index < hosts.lastIndex) add(",")
            }
        }
        .unindent()
        .add("\n)")
        .build()
}

private fun registerFunction(routeClass: ClassName): FunSpec =
    FunSpec.builder("register")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("builder", polymorphicModuleBuilder.parameterizedBy(navigationRoute))
        .addStatement("builder.subclass(%T::class, %T.serializer())", routeClass, routeClass)
        .build()

private fun Direction.drawFunction(function: ClassName): FunSpec {
    val routeArgument = routeParameterName
        ?.takeIf(String::isNotEmpty)
        ?.let { "%L = route as %T" }

    return FunSpec.builder("Draw")
        .addAnnotation(AnnotationSpec.builder(composableAnnotation).build())
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("route", navigationRoute)
        .apply {
            if (routeArgument == null) {
                addStatement("%T()", function)
            } else {
                addStatement("%T($routeArgument)", function, routeParameterName, ClassName.bestGuess(routeQualifiedName))
            }
        }
        .build()
}
