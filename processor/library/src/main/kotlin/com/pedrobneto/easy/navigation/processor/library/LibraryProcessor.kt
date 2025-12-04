package com.pedrobneto.easy.navigation.processor.library

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.pedrobneto.easy.navigation.processor.library.model.Direction
import java.util.Locale

internal class LibraryProcessor(private val environment: SymbolProcessorEnvironment) :
    SymbolProcessor {

    private val navigationClassPackage = "com.pedrobneto.easy.navigation.registry"

    private var invoked = false

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) return emptyList()
        invoked = true

        val isMultiplatformWithSingleTarget =
            environment.options["isMultiplatformWithSingleTarget"]?.toBooleanStrictOrNull() == true
        val moduleName = resolver.getModuleName().getShortName()

        val symbols = resolver.findSymbols()
        if (symbols.isEmpty()) return emptyList()

        val directionList = resolver.findSymbols().mapNotNull { function ->
            environment.codeGenerator.process(function, moduleName, isMultiplatformWithSingleTarget)
        }

        val directionListByScope = directionList.flatMap(Direction::scopes)
            .associateWith { scope -> directionList.filter { scope in it.scopes } }
        val globalDirections = directionList.filter { it.scopes.isEmpty() }

        directionListByScope.forEach { (scope, directions) ->
            environment.codeGenerator.createModuleRegistryFile(
                scope = scope,
                packageName = navigationClassPackage,
                fileName = "${scope.capitalize(Locale.getDefault())}DirectionRegistry".trim(),
                sources = directions.map(Direction::ksFile),
                directions = directions.map(Direction::qualifiedDirectionName),
            )
        }

        if (globalDirections.isNotEmpty()) {
            environment.codeGenerator.createModuleRegistryFile(
                packageName = navigationClassPackage,
                fileName = "${normalizeModuleName(moduleName)}DirectionRegistry".trim(),
                sources = globalDirections.map(Direction::ksFile),
                directions = globalDirections.map(Direction::qualifiedDirectionName),
            )
        }

        return emptyList()
    }

    private fun normalizeModuleName(name: String) = name.replace(Regex("_([a-zA-Z]+)$"), "")
        .replace(Regex("[-_]([a-zA-Z])")) { it.groupValues.last().uppercase() }
        .trimEnd('_')
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}
