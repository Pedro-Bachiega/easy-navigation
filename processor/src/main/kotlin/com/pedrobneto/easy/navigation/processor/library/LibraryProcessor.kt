package com.pedrobneto.easy.navigation.processor.library

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.validate

internal class LibraryProcessor(private val environment: SymbolProcessorEnvironment) :
    SymbolProcessor {

    private var invoked = false

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) return emptyList()

        val isMultiplatformWithSingleTarget =
            environment.options["isMultiplatformWithSingleTarget"]?.toBooleanStrictOrNull() == true
        val moduleName = resolver.getModuleName().getShortName()

        val navigationSymbols = resolver.findNavigationSymbols()
        val invalidSymbols = navigationSymbols.filterNot { it.validate() }
        if (invalidSymbols.isNotEmpty()) return invalidSymbols

        invoked = true
        navigationSymbols
            .filterNot { it is KSFunctionDeclaration }
            .forEach {
                environment.logger.error(
                    "Easy Navigation annotations can only be applied to composable functions.",
                    it
                )
            }

        val symbols = navigationSymbols.filterIsInstance<KSFunctionDeclaration>()
        if (symbols.isEmpty()) return emptyList()

        val directionList = symbols.mapNotNull { function ->
            function.extractDirection(
                logger = environment.logger,
                moduleName = moduleName,
                isMultiplatformWithSingleTarget = isMultiplatformWithSingleTarget
            )
        }

        if (!directionList.validateNoCollisions(environment.logger)) return emptyList()

        directionList.forEach { direction ->
            direction.createDirectionFile(environment.codeGenerator)
        }

        environment.codeGenerator.createRegistries(
            directionList = directionList,
            moduleName = moduleName
        )

        return emptyList()
    }
}
