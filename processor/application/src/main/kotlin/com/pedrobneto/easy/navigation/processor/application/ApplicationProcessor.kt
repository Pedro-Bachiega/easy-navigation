package com.pedrobneto.easy.navigation.processor.application

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import java.io.File

internal class ApplicationProcessor(private val environment: SymbolProcessorEnvironment) :
    SymbolProcessor {

    private var invoked = false

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) return emptyList()
        invoked = true

        val rootDir = environment.options["easy-navigation.rootDir"]
            ?.let(::File)
            ?.takeIf(File::exists)
            ?: error("easy-navigation.rootDir not set")

        val sourceSet = Regex("/src/(?<sourceSet>\\w+)/(kotlin|java)")
            .find(resolver.getAllFiles().first().filePath)
            ?.groups
            ?.get("sourceSet")
            ?.value
            ?: "commonMain"

        val filesToProcess = rootDir.walkTopDown()
            .filterValidFiles()
            .filteredBySourceSet(sourceSet)
            .toList()
            .ifEmpty { return emptyList() }

        environment.codeGenerator.createGlobalRegistryFile(
            registries = filesToProcess.extractRegistries(),
            directions = filesToProcess.extractDirections()
        )

        return emptyList()
    }
}
