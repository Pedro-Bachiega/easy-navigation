package com.pedrobneto.easy.navigation.plugin

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations

/** Complete snapshots make add/remove/rename and aggregated registries safe under incremental builds. */
@CacheableTask
abstract class GenerateNavigationTask @Inject constructor(private val exec: ExecOperations) : DefaultTask() {
    @get:Classpath abstract val generatorClasspath: ConfigurableFileCollection
    @get:Classpath abstract val dependencyClasspath: ConfigurableFileCollection
    @get:InputFiles @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection
    @get:Input abstract val sourceRoots: MapProperty<String, List<String>>
    @get:Input abstract val sourceParents: MapProperty<String, List<String>>
    @get:Input abstract val trees: ListProperty<String>
    @get:Input abstract val moduleName: Property<String>
    @get:Internal abstract val projectDirectory: DirectoryProperty
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val base = projectDirectory.get().asFile
        val files = sourceFiles.files.filter { it.extension == "kt" }.sortedBy { it.path }
        val arguments = mutableListOf("--output", outputDirectory.get().asFile.path, "--module", moduleName.get())
        sourceParents.get().toSortedMap().forEach { (name, parents) ->
            arguments.addAll(listOf("--source-set", "$name=${parents.sorted().joinToString(",")}"))
        }
        dependencyClasspath.files.sortedBy { it.path }.forEach { arguments.addAll(listOf("--dependency", it.path)) }
        trees.get().sorted().forEach { arguments.addAll(listOf("--tree", it)) }
        sourceRoots.get().toSortedMap().forEach { (name, roots) ->
            val directories = roots.map { base.resolve(it).canonicalFile.toPath() }
            files.filter { file -> directories.any { file.canonicalFile.toPath().startsWith(it) } }.forEach {
                arguments.addAll(listOf("--source", "$name=${it.path}"))
            }
        }
        exec.javaexec {
            classpath = generatorClasspath
            mainClass.set("com.pedrobneto.easy.navigation.compiler.GenerationMain")
            args(arguments)
        }.assertNormalExitValue()
    }
}
