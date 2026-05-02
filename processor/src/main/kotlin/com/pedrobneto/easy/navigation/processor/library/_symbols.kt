package com.pedrobneto.easy.navigation.processor.library

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.pedrobneto.easy.navigation.core.annotation.Deeplink
import com.pedrobneto.easy.navigation.core.annotation.ParentDeeplink
import com.pedrobneto.easy.navigation.core.annotation.ParentRoute
import com.pedrobneto.easy.navigation.core.annotation.Route

internal fun Resolver.findNavigationSymbols(): List<KSAnnotated> = listOfNotNull(
    Deeplink::class.qualifiedName,
    ParentDeeplink::class.qualifiedName,
    Route::class.qualifiedName,
    ParentRoute::class.qualifiedName,
).flatMap(::getSymbolsWithAnnotation)
    .distinctBy(KSAnnotated::stableName)
    .toList()

private fun KSAnnotated.stableName(): String =
    (this as? KSFunctionDeclaration)?.qualifiedName?.asString()
        ?: (this as? KSNode)?.location.toString()
