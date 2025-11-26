package com.pedrobneto.navigation.processor.application

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

    private val navigationClassPackage = "com.pedrobneto.navigation.registry"

    private var invoked = false

    init {
        Lumber.plant(DebugTree())
    }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> = runCatching {
        if (invoked) return@runCatching emptyList<KSAnnotated>()
        invoked = true

        val rootDir = environment.options["navigation.rootDir"]?.let(::File)
        if (rootDir == null) {
            Lumber.tag("NavigationProcessor").error("Missing navigation.rootDir option")
            return@runCatching emptyList()
        }

        if (!rootDir.exists()) return@runCatching emptyList()

        val registriesFromFiles = rootDir.walkTopDown()
            .filter { file ->
                file.isFile &&
                        file.name.endsWith("DirectionRegistry.kt") &&
                        file.name != "GlobalDirectionRegistry.kt" &&
                        file.path.contains("build/generated/ksp") &&
                        file.path.replace('\\', '/').contains("/navigation/registry/")
            }
            .mapNotNull { file ->
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
                .debug("No registries discovered from generated sources")
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
