package com.pedrobneto.easy.navigation.processor.library.model

import com.google.devtools.ksp.symbol.KSFile

internal data class Direction(
    val ksFile: KSFile,
    val isGlobal: Boolean,
    val scopes: List<String>,
    val deeplinks: List<String>,
    val directionClassName: String,
    val routePackageName: String,
    val routeClassName: String,
    val parentRoutePackageName: String?,
    val parentRouteClassName: String?,
    val parentDeeplink: String?,
    val functionPackageName: String,
    val functionName: String,
    val routeParameterName: String?,
    val qualifiedDirectionName: String = "$routePackageName.$directionClassName"
)
