package com.pedrobneto.easy.navigation.processor.library

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated

internal class LibraryProcessor(private val environment: SymbolProcessorEnvironment) :
    SymbolProcessor {

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

        val directionList = symbols.mapNotNull { function ->
            environment.codeGenerator.createDirection(
                logger = environment.logger,
                function = function,
                moduleName = moduleName,
                isMultiplatformWithSingleTarget = isMultiplatformWithSingleTarget
            )
        }

        environment.codeGenerator.createRegistries(
            directionList = directionList,
            moduleName = moduleName
        )

        return emptyList()
    }
}
