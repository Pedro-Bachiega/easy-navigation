package com.pedrobneto.easy.navigation.processor.library

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.pedrobneto.easy.navigation.core.annotation.Deeplink
import com.pedrobneto.easy.navigation.core.annotation.ParentDeeplink
import com.pedrobneto.easy.navigation.core.annotation.ParentRoute
import com.pedrobneto.easy.navigation.core.annotation.Route

internal fun Resolver.findSymbols(): List<KSFunctionDeclaration> = listOfNotNull(
    Deeplink::class.qualifiedName,
    ParentDeeplink::class.qualifiedName,
    Route::class.qualifiedName,
    ParentRoute::class.qualifiedName,
).flatMap(::getSymbolsWithAnnotation)
    .filterIsInstance<KSFunctionDeclaration>()
    .distinctBy(KSFunctionDeclaration::qualifiedName)
    .toList()
