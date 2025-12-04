package com.pedrobneto.easy.navigation.processor.application

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
        if (shouldSkipSourceSet) return emptyList()

        val filesToProcess = allRegistryFiles.filter {
            it.path.matches(Regex(".+build/generated/ksp/[^/]+/(commonMain|$sourceSet)/.+"))
        }
        val registriesFromFiles = filesToProcess.mapNotNull { file ->
            file.useLines { lines ->
                var filePackage: String? = null
                var className: String? = null

                lines.forEach { line ->
                    when {
                        line.matches(Regex("^@Scope\\(\"([^\"]+)\"\\)")) -> {
                            return@mapNotNull null
                        }

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

        environment.codeGenerator.createGlobalRegistryFile(
            packageName = navigationClassPackage,
            registries = registriesFromFiles,
        )

        return emptyList()
    }
}
