package com.pedrobneto.easy.navigation.buildlogic.util

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.gradle.BaseExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.plugins.signing.SigningExtension
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import com.android.build.gradle.LibraryExtension as LibraryExtension2

// region JetBrains
internal val Project.kotlinAndroid: KotlinAndroidProjectExtension?
    get() = extensions.findByType(KotlinAndroidProjectExtension::class.java)
internal val Project.kotlinMultiplatform: KotlinMultiplatformExtension?
    get() = extensions.findByType(KotlinMultiplatformExtension::class.java)
// endregion JetBrains

internal val Project.libs: VersionCatalog
    @Throws(IllegalStateException::class)
    get() = extensions.findByType(VersionCatalogsExtension::class.java)?.named("libs")
        ?: error("Cannot find libraries in version catalog!")

internal val Project.ktLint: KtlintExtension
    @Throws(IllegalStateException::class)
    get() = extensions.findByType(KtlintExtension::class.java)
        ?: error("Project does not implement ktlint plugin!")

internal val Project.detekt: DetektExtension
    @Throws(IllegalStateException::class)
    get() = extensions.findByType(DetektExtension::class.java)
        ?: error("Project does not implement detekt plugin!")

internal val Project.jacoco: JacocoPluginExtension
    @Throws(IllegalStateException::class)
    get() = extensions.findByType(JacocoPluginExtension::class.java)
        ?: error("Project does not implement jacoco plugin!")

internal val Project.kover: KoverProjectExtension
    @Throws(IllegalStateException::class)
    get() = extensions.findByType(KoverProjectExtension::class.java)
        ?: error("Project does not implement kover plugin!")

internal val Project.androidLibrary: LibraryExtension?
    get() = extensions.findByType(LibraryExtension::class.java)

internal val Project.androidLibrary2: LibraryExtension2
    @Throws(IllegalStateException::class)
    get() = extensions.findByType(LibraryExtension2::class.java)
        ?: error("Project does not implement android-library plugin!")

internal val Project.androidApplication: ApplicationExtension?
    get() = extensions.findByType(ApplicationExtension::class.java)

internal val Project.android: BaseExtension
    @Throws(IllegalStateException::class)
    get() = extensions.findByType(BaseExtension::class.java)
        ?: error("Project does not implement android plugin!")

internal val Project.libraryComponent: LibraryAndroidComponentsExtension
    @Throws(IllegalStateException::class)
    get() = extensions.findByType(LibraryAndroidComponentsExtension::class.java)
        ?: error("Project does not implement android-library plugin!")

internal val Project.applicationComponent: ApplicationAndroidComponentsExtension
    @Throws(IllegalStateException::class)
    get() = extensions.findByType(ApplicationAndroidComponentsExtension::class.java)
        ?: error("Project does not implement android-application plugin!")

internal val Project.publishing: PublishingExtension
    @Throws(IllegalStateException::class)
    get() = extensions.findByType(PublishingExtension::class.java)
        ?: error("Project does not implement maven-publish plugin!")

internal val Project.vanniktechPublishing: MavenPublishBaseExtension
    @Throws(IllegalStateException::class)
    get() = extensions.findByType(MavenPublishBaseExtension::class.java)
        ?: error("Project does not implement vanniktech-publish plugin!")

internal val Project.sign: SigningExtension
    @Throws(IllegalStateException::class)
    get() = extensions.findByType(SigningExtension::class.java)
        ?: error("Project does not implement signing plugin!")

internal fun Project.applyPlugins(vararg id: String) {
    id.forEach { plugins.apply(libs.getPluginId(it)) }
}

internal fun Project.hasPlugins(vararg id: String) =
    id.all { plugins.hasPlugin(libs.getPluginId(it)) }
