package com.pedrobneto.easy.navigation.processor.library

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSFile
import com.pedrobneto.easy.navigation.processor.library.model.Direction
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo
import java.util.Locale

private val directionRegistryClass = ClassName("com.pedrobneto.easy.navigation.core.model", "DirectionRegistry")
private val scopeAnnotation = ClassName("com.pedrobneto.easy.navigation.core.annotation", "Scope")

internal fun normalizeModuleNameForRegistry(name: String) = name.replace(Regex("_([a-zA-Z]+)$"), "")
    .replace(Regex("[-_]([a-zA-Z])")) { it.groupValues.last().uppercase() }
    .trimEnd('_')
    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

internal fun CodeGenerator.createRegistries(directionList: List<Direction>, moduleName: String) {
    val navigationClassPackage = "com.pedrobneto.easy.navigation.registry"

    val directionListByScope = directionList.flatMap(Direction::scopes)
        .associateWith { scope -> directionList.filter { scope in it.scopes } }
    val globalDirections = directionList.filter { it.scopes.isEmpty() }

    directionListByScope.forEach { (scope, directions) ->
        createModuleRegistryFile(
            scope = scope,
            packageName = navigationClassPackage,
            fileName = "${scope.toRegistryPrefix()}DirectionRegistry".trim(),
            sources = directions.map(Direction::ksFile),
            directions = directions.map(Direction::qualifiedDirectionName),
        )
    }

    if (globalDirections.isNotEmpty()) {
        createModuleRegistryFile(
            packageName = navigationClassPackage,
            fileName = "${normalizeModuleNameForRegistry(moduleName)}DirectionRegistry".trim(),
            sources = globalDirections.map(Direction::ksFile),
            directions = globalDirections.map(Direction::qualifiedDirectionName),
        )
    }
}

internal fun CodeGenerator.createModuleRegistryFile(
    scope: String? = null,
    packageName: String,
    fileName: String,
    sources: List<KSFile>,
    directions: List<String>,
) {
    val directionClasses = directions.map(ClassName::bestGuess)
    val type = TypeSpec.objectBuilder(fileName)
        .addModifiers(KModifier.DATA)
        .superclass(directionRegistryClass)
        .addSuperclassConstructorParameter("directions = %L", directionClasses.listCode())
        .apply {
            scope?.let {
                addAnnotation(
                    AnnotationSpec.builder(scopeAnnotation)
                        .addMember("%S", it)
                        .build()
                )
            }
        }
        .build()

    FileSpec.builder(packageName, fileName)
        .addType(type)
        .build()
        .writeTo(
            codeGenerator = this,
            dependencies = Dependencies(aggregating = true, *sources.toTypedArray())
        )
}

private fun List<ClassName>.listCode(): CodeBlock {
    if (isEmpty()) return CodeBlock.of("emptyList()")
    return CodeBlock.builder()
        .add("listOf(")
        .indent()
        .apply {
            forEachIndexed { index, direction ->
                add("\n%T", direction)
                if (index < lastIndex) add(",")
            }
        }
        .unindent()
        .add("\n)")
        .build()
}

internal fun String.toRegistryPrefix(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
