package com.pedrobneto.easy.navigation.processor.application

import br.com.arch.toolkit.lumber.DebugTree
import br.com.arch.toolkit.lumber.Lumber
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import java.io.File

internal class ApplicationProcessor(private val environment: SymbolProcessorEnvironment) :
    SymbolProcessor {

    private val navigationClassPackage = "com.pedrobneto.easy.navigation.registry"

    private var invoked = false

    init {
        Lumber.plant(DebugTree())
    }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> = runCatching {
        if (invoked) return@runCatching emptyList<KSAnnotated>()
        invoked = true

        val rootDir = environment.options["easy-navigation.rootDir"]?.let(::File)
        if (rootDir == null) {
            Lumber.tag("NavigationProcessor").error("Missing easy-navigation.rootDir option")
            return@runCatching emptyList()
        }

        if (!rootDir.exists()) return@runCatching emptyList()

        val sourceSet = Regex("/src/(?<sourceSet>\\w+)/(kotlin|java)")
            .find(resolver.getAllFiles().first().filePath)
            ?.groups
            ?.get("sourceSet")
            ?.value
            ?: "commonMain"

        val allRegistryFiles = rootDir.walkTopDown().filter { file ->
            file.isFile &&
                    file.name.endsWith("DirectionRegistry.kt") &&
                    file.name != "GlobalDirectionRegistry.kt" &&
                    file.path.replace('\\', '/').matches(Regex(".+build/generated/ksp/.+"))
        }

        val shouldOnlyGenerateCommonMain = allRegistryFiles.all {
            it.path.matches(Regex(".+build/generated/ksp/[^/]+/commonMain/.+"))
        }

        val shouldSkipSourceSet = (shouldOnlyGenerateCommonMain && sourceSet != "commonMain") ||
                (!shouldOnlyGenerateCommonMain && sourceSet == "commonMain")
        if (shouldSkipSourceSet) return@runCatching emptyList()

        val filesToProcess = allRegistryFiles.filter {
            it.path.matches(Regex(".+build/generated/ksp/[^/]+/(commonMain|$sourceSet)/.+"))
        }
        val registriesFromFiles = filesToProcess.mapNotNull { file ->
            file.useLines { lines ->
                var filePackage: String? = null
                var className: String? = null

                lines.forEach { line ->
                    when {
                        line.matches(Regex("^package (.*)$")) -> {
                            filePackage = line.removePrefix("package ")
                        }

                        line.matches(Regex("^data object (.*)DirectionRegistry : DirectionRegistry\\(")) -> {
                            className = line.removePrefix("data object ")
                                .removeSuffix(": DirectionRegistry(")
                        }
                    }
                }

                if (filePackage == null || className == null) return@mapNotNull null

                "$filePackage.$className"
            }
        }
            .distinct()
            .sorted()
            .toList()

        if (registriesFromFiles.isEmpty()) {
            Lumber.tag("ApplicationProcessor")
                .warn("No registries discovered from generated sources")
        }

        environment.codeGenerator.createGlobalRegistryFile(
            packageName = navigationClassPackage,
            registries = registriesFromFiles,
        )

        emptyList()
    }.getOrElse { exception ->
        Lumber.tag("NavigationProcessor").error(exception, "Error while processing")
        emptyList()
    }
}
