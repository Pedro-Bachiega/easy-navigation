package com.pedrobneto.easy.navigation.plugin

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

private const val SUPPORTED_KOTLIN = "2.4.20"
private const val ARTIFACT = "easy-navigation-compiler-plugin"
private const val GROUP = "io.github.pedro-bachiega"
private val PLUGIN_VERSION: String by lazy {
    LibraryGradlePlugin::class.java.classLoader.getResourceAsStream("easy-navigation-plugin.properties")
        ?.use { java.util.Properties().apply { load(it) }.getProperty("version") }
        ?.takeIf(String::isNotBlank) ?: error("Could not load Easy Navigation Gradle plugin version.")
}

class LibraryGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) = with(target) {
        val generator = configurations.create("easyNavigationGenerator") {
            isCanBeConsumed = false
            isCanBeResolved = true
            description = "Isolated Kotlin source generator for Easy Navigation"
        }
        dependencies.add(generator.name, "$GROUP:$ARTIFACT:$PLUGIN_VERSION")
        dependencies.add(generator.name, "org.jetbrains.kotlin:kotlin-compiler-embeddable:$SUPPORTED_KOTLIN")
        val generate = tasks.register("generateEasyNavigation", GenerateNavigationTask::class.java) {
            group = "easy navigation"
            description = "Generates Kotlin directions and platform registries without KSP"
            generatorClasspath.from(generator)
            moduleName.set(project.name)
            projectDirectory.set(layout.projectDirectory)
            outputDirectory.set(layout.buildDirectory.dir("generated/easyNavigation/kotlin"))
        }

        tasks.matching { it.name == "prepareKotlinIdeaImport" }.configureEach {
            dependsOn(generate)
        }

        var kotlinConfigured = false
        listOf("org.jetbrains.kotlin.multiplatform", "org.jetbrains.kotlin.jvm", "org.jetbrains.kotlin.android").forEach { id ->
            pluginManager.withPlugin(id) {
                if (kotlinConfigured) return@withPlugin
                kotlinConfigured = true
                check(getKotlinPluginVersion() == SUPPORTED_KOTLIN) {
                    "Easy Navigation supports Kotlin $SUPPORTED_KOTLIN; found ${getKotlinPluginVersion()}. Align Kotlin, Compose Compiler and Serialization plugin versions."
                }
                // Default hierarchy edges are created after ordinary afterEvaluate callbacks.
                // Read the KGP graph only after refines edges have been finalized.
                withFinalizedNavigationGraph { compilationTrees ->
                    val extension = kotlinExtension
                    val targets = when (extension) {
                        is KotlinMultiplatformExtension -> extension.targets.toList()
                        is KotlinSingleTargetExtension<*> -> listOf(extension.target)
                        else -> error("Easy Navigation requires a Kotlin project.")
                    }
                    val compilations = compilationTrees.keys.toList()
                    val sourceSets = extension.sourceSets.toList()
                    val output = layout.buildDirectory.dir("generated/easyNavigation/kotlin").get().asFile
                    val originalDirectories = sourceSets.associate { sourceSet ->
                        sourceSet.name to sourceSet.kotlin.srcDirs.filterNot { it.toPath().startsWith(output.toPath()) }
                    }
                    val graph = sourceSets.associate { sourceSet -> sourceSet.name to sourceSet.dependsOn.map { it.name }.sorted() }
                    fun ancestors(name: String): Set<String> = setOf(name) + graph[name].orEmpty().flatMap { ancestors(it) }
                    val mainSets = compilations.filter { compilationTrees[it] == "main" }.flatMap { ancestors(it.defaultSourceSet.name) }.toSet()
                    val trees = compilations.groupBy { compilationTrees.getValue(it) }.map { (name, members) ->
                        val leaves = members.map { it.defaultSourceSet.name }.distinct().sorted()
                        val shared = leaves.map(::ancestors).reduce { a, b -> a intersect b }.let {
                            if (name == "main") it else it - mainSets
                        }
                        val root = if (extension is KotlinMultiplatformExtension) {
                            shared.filter { candidate -> graph[candidate].orEmpty().none { it in shared } }.singleOrNull()
                        } else null
                        "$name:${root.orEmpty()}:${leaves.joinToString(",")}"
                    }
                    generate.configure {
                        dependencyClasspath.from(targets.flatMap { it.compilations.toList() }.filter {
                            (compilationTrees[it] == "main" && it.target.platformType in setOf(KotlinPlatformType.jvm, KotlinPlatformType.androidJvm)) ||
                                (it.target.platformType == KotlinPlatformType.common && it.defaultSourceSet.name in mainSets && it.defaultSourceSet.dependsOn.isEmpty())
                        }.map { it.compileDependencyFiles })
                        sourceParents.set(graph)
                        sourceRoots.set(originalDirectories.mapValues { (_, dirs) -> dirs.map { it.relativeTo(projectDir).invariantSeparatorsPath }.sorted() })
                        this.trees.set(trees)
                        sourceFiles.from(originalDirectories.values.flatten().map { fileTree(it) { include("**/*.kt") } })
                    }
                    sourceSets.forEach { sourceSet ->
                        sourceSet.kotlin.srcDir(generate.flatMap { it.outputDirectory.dir(sourceSet.name) })
                    }
                }
            }
        }
        afterEvaluate {
            check(kotlinConfigured) { "Easy Navigation requires a Kotlin JVM, Android or Multiplatform plugin." }
        }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true
    override fun getCompilerPluginId(): String = "io.github.pedro-bachiega.easy-navigation"
    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(GROUP, ARTIFACT, PLUGIN_VERSION)

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        kotlinCompilation.compileTaskProvider.configure { dependsOn(project.tasks.named("generateEasyNavigation")) }
        return project.provider { emptyList() }
    }
}
