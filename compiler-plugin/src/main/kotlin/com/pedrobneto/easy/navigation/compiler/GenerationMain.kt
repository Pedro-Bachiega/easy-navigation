package com.pedrobneto.easy.navigation.compiler

import java.io.File

/** Runs in an isolated Gradle worker JVM; compiler dependencies never enter the Gradle plugin classloader. */
object GenerationMain {
    @JvmStatic
    fun main(arguments: Array<String>) {
        val options = arguments.toList().chunked(2).groupBy({ it[0] }, { it[1] })
        val output = File(options.getValue("--output").single())
        val module = options.getValue("--module").single()
        val parents = options["--source-set"].orEmpty().associate {
            it.substringBefore('=') to it.substringAfter('=', "").split(',').filter(String::isNotEmpty)
        }
        val sources = options["--source"].orEmpty().groupBy({ it.substringBefore('=') }, { File(it.substringAfter('=')) })
        val trees = options.getValue("--tree").map {
            val parts = it.split(':', limit = 3)
            GenerationTree(parts[0], parts[1].ifEmpty { null }, parts[2].split(',').filter(String::isNotEmpty))
        }
        generate(output, module, parents, sources, trees)
    }
}

internal data class GenerationTree(val name: String, val root: String?, val leaves: List<String>)

internal fun generate(
    output: File,
    module: String,
    parents: Map<String, List<String>>,
    sources: Map<String, List<File>>,
    trees: List<GenerationTree>,
) {
    val graph = SourceGraph(parents, trees.flatMap { it.leaves }, null)
    val destinations = SourceReader().read(sources, graph)
    val staging = File(output.parentFile, "${output.name}.staging")
    staging.deleteRecursively()
    staging.mkdirs()
    try {
        val writer = SourceWriter(staging)
        destinations.forEach(writer::direction)
        val mainSets = trees.filter { it.name == "main" }.flatMap { it.leaves.flatMap(graph::ancestors) }.toSet()
        trees.forEach { tree ->
            val sets = tree.leaves.flatMap(graph::ancestors).toSet().let {
                if (tree.name == "main") it else it - mainSets
            }
            val owned = destinations.filter { it.sourceSet in sets }
            if (owned.isEmpty()) return@forEach
            val scopes = owned.flatMap { it.scopes }.distinct().sorted()
            val moduleName = registryPrefix(module) + if (tree.name == "main") "" else registryPrefix(tree.name)
            val registries = buildList<Pair<String, String?>> {
                if (owned.any { it.scopes.isEmpty() }) add("${moduleName}DirectionRegistry" to null)
                scopes.forEach { add("${scopePrefix(it)}DirectionRegistry" to it) }
            }
            require(registries.map { it.first }.distinct().size == registries.size) { "Easy Navigation: registry name collision in module $module." }
            registries.forEach { (name, _) -> require(name.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) { "Easy Navigation: invalid registry name '$name'." } }
            tree.leaves.forEach { leaf ->
                val visible = owned.filter { it.sourceSet in graph.ancestors(leaf) }
                validateCollisions(visible)
                registries.forEach { (name, scope) ->
                    val included = visible.filter { if (scope == null) it.scopes.isEmpty() else scope in it.scopes }
                    writer.registry(leaf, name, included, scope, if (tree.root == null || tree.root == leaf) RegistryKind.NORMAL else RegistryKind.ACTUAL)
                }
            }
            tree.root?.takeIf { root -> tree.leaves.none { it == root } }?.let { root ->
                registries.forEach { (name, scope) -> writer.registry(root, name, emptyList(), scope, RegistryKind.EXPECT) }
            }
        }
        // Only replace successful generation. Removed annotations also remove obsolete files.
        output.deleteRecursively()
        check(staging.renameTo(output)) { "Cannot install generated sources at $output" }
    } finally {
        staging.deleteRecursively()
    }
}

internal fun validateCollisions(destinations: List<Destination>) {
    destinations.groupBy { it.route }.filterValues { it.size > 1 }.forEach { (route, entries) ->
        error("${entries.joinToString { it.location }}: Easy Navigation: route $route is bound to multiple destinations.")
    }
    destinations.groupBy { it.direction }.filterValues { it.size > 1 }.forEach { (direction, entries) ->
        error("${entries.joinToString { it.location }}: Easy Navigation: generated direction $direction collides.")
    }
}
