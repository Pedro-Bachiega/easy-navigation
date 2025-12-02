package com.pedrobneto.navigation.processor.library

import br.com.arch.toolkit.lumber.DebugTree
import br.com.arch.toolkit.lumber.Lumber
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.pedrobneto.navigation.core.annotation.NavigationEntry
import com.pedrobneto.navigation.core.model.NavigationRoute
import java.util.Locale
import kotlin.reflect.KClass

internal class LibraryProcessor(private val environment: SymbolProcessorEnvironment) :
    SymbolProcessor {

    private val navigationClassPackage = "com.pedrobneto.navigation.registry"

    private var invoked = false

    init {
        Lumber.plant(DebugTree())
    }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> = runCatching {
        if (invoked) return emptyList()
        invoked = true

        val isMultiplatformWithSingleTarget =
            environment.options["isMultiplatformWithSingleTarget"]?.toBooleanStrictOrNull() == true
        val moduleName = resolver.getModuleName().getShortName()
        val commonMainModuleName = "commonMain"

        val symbolList = resolver
            .getSymbolsWithAnnotation(
                annotationName = NavigationEntry::class.qualifiedName!!,
            )
            .filterIsInstance<KSFunctionDeclaration>()
            .toList()

        val directionList = symbolList.mapNotNull { function ->
            val ksFile = function.containingFile ?: return@mapNotNull null
            val fileInCommonMainPath = ksFile.filePath.contains(commonMainModuleName)
            val moduleNameContainsCommonMain = moduleName.contains(commonMainModuleName)
            val isNotInCommonMain = (moduleNameContainsCommonMain && !fileInCommonMainPath) ||
                    (!moduleNameContainsCommonMain && fileInCommonMainPath)

            val shouldSkip = !isMultiplatformWithSingleTarget && isNotInCommonMain
            if (shouldSkip) return@mapNotNull null

            val annotation = function.getAnnotationsByType(NavigationEntry::class)
                .firstOrNull()
                ?: return@mapNotNull null

            val routeQualifiedName =
                annotation.qualifiedNameForRoute(NavigationEntry::route) ?: run {
                    Lumber.tag("LibraryProcessor")
                        .error("Route not found for function ${function.simpleName.asString()}")
                    return@mapNotNull null
                }

            val parentRouteQualifiedName =
                annotation.qualifiedNameForRoute(NavigationEntry::parentRoute)

            val deeplinks = runCatching { annotation.deeplinks }.getOrDefault(emptyArray())

            val routePackageName = routeQualifiedName.substringBeforeLast('.')
            val routeClassName = routeQualifiedName.substringAfterLast('.')
            val directionClassName = "${routeClassName}Direction"

            val parentRoutePackageName = parentRouteQualifiedName?.substringBeforeLast('.')
            val parentRouteClassName = parentRouteQualifiedName?.substringAfterLast('.')

            val routeParameterName = function.parameters.firstOrNull {
                it.type.resolve().declaration.qualifiedName?.asString() == routeQualifiedName
            }?.name?.asString()

            environment.codeGenerator.createDirectionFile(
                deeplinks = deeplinks,
                directionClassName = directionClassName,
                routePackageName = routePackageName,
                routeClassName = routeClassName,
                parentRoutePackageName = parentRoutePackageName,
                parentRouteClassName = parentRouteClassName,
                functionPackageName = function.packageName.asString(),
                functionName = function.simpleName.asString(),
                routeParameterName = routeParameterName,
            )
        }

        if (directionList.isNotEmpty()) {
            environment.codeGenerator.createModuleRegistryFile(
                packageName = navigationClassPackage,
                directions = directionList,
                fileName = "${normalizeModuleName(moduleName)}DirectionRegistry".trim()
            )
        }

        return emptyList()
    }.getOrElse { exception ->
        Lumber.tag("LibraryProcessor").error(exception, "Error while processing")
        emptyList()
    }

    private fun normalizeModuleName(name: String) = name.replace("_([a-zA-Z]+)$".toRegex(), "")
        .replace("[-_]([a-zA-Z])".toRegex()) { it.groupValues.last().uppercase() }
        .trimEnd('_')
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    private fun NavigationEntry.qualifiedNameForRoute(
        predicate: NavigationEntry.() -> KClass<*>
    ): String? = runCatching { predicate().qualifiedName.orEmpty() }
        .getOrElse { exception ->
            Regex("(.*ClassNotFoundException: )([a-zA-Z._]+)")
                .find(exception.message.orEmpty())
                ?.groupValues
                ?.last()
        }.takeIf { it != NavigationRoute::class.qualifiedName }
}
