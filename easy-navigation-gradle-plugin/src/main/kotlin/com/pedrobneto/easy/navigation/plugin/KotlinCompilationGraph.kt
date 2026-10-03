@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.pedrobneto.easy.navigation.plugin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.hierarchy.orNull
import org.jetbrains.kotlin.gradle.plugin.launchInStage

/** Internal KGP APIs are isolated here and covered by the exact Kotlin version pin and consumer CI. */
internal fun Project.withFinalizedNavigationGraph(action: (Map<KotlinCompilation<*>, String>) -> Unit) {
    launchInStage(KotlinPluginLifecycle.Stage.AfterFinaliseRefinesEdges) {
        val extension = kotlinExtension
        val targets = when (extension) {
            is KotlinMultiplatformExtension -> extension.targets.toList()
            is KotlinSingleTargetExtension<*> -> listOf(extension.target)
            else -> error("Easy Navigation requires a Kotlin project.")
        }
        val classifications = targets.filter { it.platformType != KotlinPlatformType.common }
            .flatMap { it.compilations.toList() }
            .associateWith { KotlinSourceSetTree.orNull(it)?.name ?: it.name }
        action(classifications)
    }
}
