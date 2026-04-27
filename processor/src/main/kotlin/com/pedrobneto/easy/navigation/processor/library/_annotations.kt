package com.pedrobneto.easy.navigation.processor.library

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import kotlin.reflect.KClass

internal fun KSAnnotated.annotationsByType(annotation: KClass<out Annotation>): List<KSAnnotation> {
    val qualifiedName = annotation.qualifiedName.orEmpty()
    return annotations
        .filter { it.annotationType.resolve().declaration.qualifiedName?.asString() == qualifiedName }
        .toList()
}

internal fun KSAnnotated.hasAnnotation(annotation: KClass<out Annotation>): Boolean =
    annotationsByType(annotation).isNotEmpty()

internal fun KSAnnotated.hasAnnotation(qualifiedName: String): Boolean =
    annotations.any { it.annotationType.resolve().declaration.qualifiedName?.asString() == qualifiedName }

internal fun KSAnnotation.stringArgument(name: String): String? =
    arguments.firstOrNull { it.name?.asString() == name }?.value as? String

internal fun KSAnnotation.typeArgument(name: String): KSType? =
    arguments.firstOrNull { it.name?.asString() == name }?.value as? KSType

internal fun KSType.qualifiedName(): String? =
    declaration.qualifiedName?.asString()

internal fun KSDeclaration.qualifiedNameString(): String? =
    qualifiedName?.asString()
