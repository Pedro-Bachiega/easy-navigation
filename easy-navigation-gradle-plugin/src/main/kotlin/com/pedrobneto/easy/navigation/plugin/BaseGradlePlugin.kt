package com.pedrobneto.easy.navigation.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.util.Locale
import kotlin.reflect.full.declaredMemberProperties

abstract class BaseGradlePlugin : Plugin<Project> {
    protected abstract val processor: String

    override fun apply(target: Project) = with(target) {
        val kspExtension = extensions.findByName("ksp")
            ?: error("ksp not implemented for module $name")
        val argMethod = kspExtension.javaClass
            .getMethod("arg", String::class.java, String::class.java)
        argMethod.invoke(kspExtension, "easy-navigation.rootDir", rootDir.path)

        project.afterEvaluate {
            val isSingleTarget = kotlinExtension.let {
                when (it) {
                    is KotlinSingleTargetExtension<*> -> listOf(it.target)
                    is KotlinMultiplatformExtension -> it.targets
                    else -> error("Unexpected 'kotlin' extension $it")
                }
            }.toList().size == 2

            if (isSingleTarget) {
                argMethod.invoke(kspExtension, "isMultiplatformWithSingleTarget", "true")
            } else {
                val isUsingKSP2 = kspExtension.javaClass.kotlin.declaredMemberProperties.find {
                    it.name == "useKsp2"
                }?.call(kspExtension).let {
                    (it as Property<*>?)?.get() as Boolean?
                } ?: project.findProperty("ksp.useKSP2")?.toString()?.toBoolean() ?: false

                if (isUsingKSP2) {
                    tasks.named { name -> name.startsWith("ksp") }.configureEach {
                        if (name != "kspCommonMainKotlinMetadata") {
                            dependsOn("kspCommonMainKotlinMetadata")
                        }
                    }
                } else {
                    tasks.withType(KotlinCompilationTask::class.java).configureEach {
                        if (name != "kspCommonMainKotlinMetadata") {
                            dependsOn("kspCommonMainKotlinMetadata")
                        }
                    }
                }
            }
        }

        val dependency =
            "io.github.pedro-bachiega:easy-navigation-$processor-processor:0.0.1-alpha06"
        when (val kotlinExtension = kotlinExtension) {
            is KotlinSingleTargetExtension<*> -> {
                dependencies.add("ksp", dependency)
            }

            is KotlinMultiplatformExtension -> {
                kotlinExtension.targets.configureEach {
                    if (platformType == KotlinPlatformType.common) {
                        dependencies.add("kspCommonMainMetadata", dependency)
                        return@configureEach
                    }

                    val capitalizedTargetName = targetName.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
                    }

                    dependencies.add("ksp$capitalizedTargetName", dependency)

                    if (name == "android") {
                        dependencies.add("ksp", dependency)
                    }

                    if (compilations.any { it.name == "test" }) {
                        dependencies.add("ksp${capitalizedTargetName}Test", dependency)
                    }
                }

                kotlinExtension.sourceSets.named { name -> name == "commonMain" }.configureEach {
                    kotlin.srcDir(
                        "${layout.buildDirectory.get()}/generated/ksp/metadata/commonMain/kotlin"
                    )
                }
            }
        }
    }
}

internal class ApplicationGradlePlugin : BaseGradlePlugin() {
    override val processor: String = "application"
}

internal class LibraryGradlePlugin : BaseGradlePlugin() {
    override val processor: String = "library"
}
