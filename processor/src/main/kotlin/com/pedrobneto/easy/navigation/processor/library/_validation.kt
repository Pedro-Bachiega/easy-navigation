package com.pedrobneto.easy.navigation.processor.library

import com.google.devtools.ksp.processing.KSPLogger
import com.pedrobneto.easy.navigation.processor.library.model.Direction
private val kotlinIdentifier = Regex("[A-Za-z_][A-Za-z0-9_]*")

internal fun List<Direction>.validateNoCollisions(logger: KSPLogger): Boolean {
    var valid = true

    groupBy(Direction::routeQualifiedName)
        .filterValues { it.size > 1 }
        .forEach { (route, directions) ->
            valid = false
            directions.forEach {
                logger.error("Route $route is bound to multiple navigation destinations.", it.ksFile)
            }
        }

    groupBy { it.routePackageName to it.directionClassName }
        .filterValues { it.size > 1 }
        .forEach { (name, directions) ->
            valid = false
            directions.forEach {
                logger.error("Generated direction ${name.second} collides in package ${name.first}.", it.ksFile)
            }
        }

    val registryNames = flatMap { direction ->
        direction.scopes.map { scope -> scope to "${scope.toRegistryPrefix()}DirectionRegistry".trim() }
    }

    registryNames
        .filterNot { (_, fileName) -> fileName.isKotlinIdentifier() }
        .forEach { (scope, fileName) ->
            valid = false
            logger.error("Scope '$scope' produces invalid registry name '$fileName'. Use a Kotlin identifier-safe scope.")
        }

    registryNames
        .groupBy { it.second }
        .filterValues { it.map(Pair<String, String>::first).distinct().size > 1 }
        .forEach { (fileName, scopes) ->
            valid = false
            logger.error("Generated registry name $fileName is shared by scopes ${scopes.map { it.first }.distinct()}.")
        }

    return valid
}

internal fun String.isKotlinIdentifier(): Boolean = matches(kotlinIdentifier)
