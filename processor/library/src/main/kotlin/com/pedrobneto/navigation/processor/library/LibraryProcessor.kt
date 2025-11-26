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
import com.pedrobneto.navigation.annotation.NavigationEntry
import java.util.Locale

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

        val directionList = resolver
            .getSymbolsWithAnnotation(
                annotationName = NavigationEntry::class.qualifiedName!!,
            )
            .filterIsInstance<KSFunctionDeclaration>()
            .toList()
            .mapNotNull { function ->
                val annotation = function.getAnnotationsByType(NavigationEntry::class)
                    .firstOrNull()
                    ?: return@mapNotNull null

                val routeQualifiedName = runCatching { annotation.route.qualifiedName }
                    .getOrElse { exception ->
                        Regex("(.*ClassNotFoundException: )([a-zA-Z._]+)")
                            .find(exception.message.orEmpty())
                            ?.groupValues
                            ?.last()
                            ?: run {
                                Lumber.tag("LibraryProcessor").error(
                                    exception,
                                    "Route not found for function ${function.simpleName.asString()}"
                                )
                                return@mapNotNull null
                            }
                    } ?: return@mapNotNull null


                val deeplinks = runCatching { annotation.deeplinks }.getOrDefault(emptyArray())

                val routePackageName = routeQualifiedName.substringBeforeLast('.')
                val routeClassName = routeQualifiedName.substringAfterLast('.')
                val directionClassName = "${routeClassName}Direction"

                val routeParameterName = function.parameters.firstOrNull {
                    it.type.resolve().declaration.qualifiedName?.asString() == routeQualifiedName
                }?.name?.asString()

                environment.codeGenerator.createDirectionFile(
                    deeplinks = deeplinks,
                    directionClassName = directionClassName,
                    routePackageName = routePackageName,
                    routeClassName = routeClassName,
                    functionPackageName = function.packageName.asString(),
                    functionName = function.simpleName.asString(),
                    routeParameterName = routeParameterName,
                )
            }

        val moduleName = resolver.getModuleName().asString()
        val fileName = "${normalizeModuleName(moduleName)}DirectionRegistry".trim()

        if (directionList.isEmpty()) {
            Lumber.tag("LibraryProcessor").debug("No annotations found for module $moduleName")
        }

        environment.codeGenerator.createModuleRegistryFile(
            packageName = navigationClassPackage,
            directions = directionList,
            fileName = fileName
        )

        return emptyList()
    }.getOrElse { exception ->
        Lumber.tag("LibraryProcessor").error(exception, "Error while processing")
        emptyList()
    }

    private fun normalizeModuleName(name: String) = name.replace("_([a-zA-Z]+)$".toRegex(), "")
        .replace("[-_]([a-zA-Z])".toRegex()) { it.groupValues.last().uppercase() }
        .trimEnd('_')
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}
