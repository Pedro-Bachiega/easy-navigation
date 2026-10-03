package com.pedrobneto.easy.navigation.compiler

internal const val CORE = "com.pedrobneto.easy.navigation.core"
internal const val REGISTRY_PACKAGE = "com.pedrobneto.easy.navigation.registry"
internal const val SUPPORTED_KOTLIN = "2.4.20"
internal const val PLUGIN_ID = "io.github.pedro-bachiega.easy-navigation"

internal data class Destination(
    val sourceSet: String,
    val location: String,
    val route: String,
    val routePackage: String,
    val routeName: String,
    val function: String,
    val parameter: String?,
    val scopes: List<String>,
    val global: Boolean,
    val deeplinks: List<String>,
    val parentRoute: String?,
    val parentDeeplink: String?,
    val pane: Pane,
    val modal: Boolean,
) {
    val directionName: String get() = "${routeName}Direction"
    val direction: String get() = listOf(routePackage, directionName).filter(String::isNotEmpty).joinToString(".")
}

internal sealed interface Pane {
    data class Adaptive(val ratio: Float = 1f) : Pane
    data object Single : Pane
    data class Extra(val hosts: List<Pair<String, Float>>) : Pane
}

internal data class SourceGraph(
    val parents: Map<String, List<String>>,
    val leaves: List<String>,
    val root: String?,
) {
    fun ancestors(name: String): Set<String> {
        val result = linkedSetOf<String>()
        fun visit(current: String) {
            check(result.add(current)) { "Cyclic or duplicate source-set edge at $current" }
            parents[current].orEmpty().forEach { if (it !in result) visit(it) }
        }
        visit(name)
        return result
    }
}

internal fun registryPrefix(value: String): String = value
    .replace(Regex("[-_]([a-zA-Z])")) { it.groupValues[1].uppercase(java.util.Locale.ROOT) }
    .replaceFirstChar { it.titlecase(java.util.Locale.ROOT) }

internal fun scopePrefix(value: String): String = value.replaceFirstChar { it.titlecase(java.util.Locale.ROOT) }
