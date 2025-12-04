package com.pedrobneto.easy.navigation.processor.application

import java.io.File

private val File.isDirectionRegistry: Boolean get() = name.endsWith("DirectionRegistry.kt")
private val File.isNavigationDirection: Boolean get() = name.endsWith("Direction.kt")

private val File.hasValidName: Boolean
    get() = (isDirectionRegistry || isNavigationDirection) && name != "GlobalDirectionRegistry.kt"

private val File.hasValidPath: Boolean
    get() = path.replace('\\', '/').matches(Regex(".+build/generated/ksp/.+"))

private fun File.isInSourceSet(sourceSet: String) =
    path.matches(Regex(".+build/generated/ksp/[^/]+/$sourceSet/.+"))

internal fun Sequence<File>.filterValidFiles(): Sequence<File> = filter { file ->
    file.isFile && file.hasValidName && file.hasValidPath
}

internal fun Sequence<File>.filteredBySourceSet(sourceSet: String): Sequence<File> {
    val shouldOnlyGenerateCommonMain = all { it.isInSourceSet("commonMain") }

    val shouldSkipSourceSet = (shouldOnlyGenerateCommonMain && sourceSet != "commonMain") ||
            (!shouldOnlyGenerateCommonMain && sourceSet == "commonMain")
    if (shouldSkipSourceSet) return emptySequence()

    return filter { it.isInSourceSet("(commonMain|$sourceSet)") }
}

internal fun List<File>.extractRegistries(): List<String> = mapNotNull { file ->
    if (!file.isDirectionRegistry) return@mapNotNull null

    var filePackage: String? = null
    var className: String? = null

    file.readLines().forEach { line ->
        when {
            line.matches(Regex("^@Scope\\(\"([^\"]+)\"\\)")) -> return@mapNotNull null
            line.matches(Regex("^package (.+)$")) -> filePackage = line.removePrefix("package ")
            line.matches(Regex("^data object (.+)DirectionRegistry : DirectionRegistry\\(")) -> {
                className = line.removePrefix("data object ")
                    .removeSuffix(" : DirectionRegistry(")
            }
        }
    }

    if (filePackage == null || className == null) return@mapNotNull null

    "$filePackage.$className"
}.distinct().sorted()

internal fun List<File>.extractDirections(): List<String> = mapNotNull { file ->
    if (!file.isNavigationDirection) return@mapNotNull null

    var hasGlobalScope = false
    var filePackage: String? = null
    var className: String? = null

    file.readLines().forEach { line ->
        when {
            line.matches(Regex("^@GlobalScope$")) -> hasGlobalScope = true
            line.matches(Regex("^package (.*)$")) -> filePackage = line.removePrefix("package ")
            line.matches(Regex("^data object (.*)Direction : NavigationDirection\\(")) -> {
                className = line.removePrefix("data object ")
                    .removeSuffix(" : NavigationDirection(")
            }
        }
    }

    if (!hasGlobalScope || filePackage == null || className == null) return@mapNotNull null

    "$filePackage.$className"
}.distinct().sorted()
