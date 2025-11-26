import com.vanniktech.maven.publish.KotlinJvm
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    signing
    alias(libs.plugins.vanniktech.publish)
}

apply(from = "$rootDir/versioning.gradle.kts")

tasks.withType<AbstractPublishToMaven>().configureEach {
    dependsOn(tasks.withType<Sign>())
}

tasks.withType<KotlinCompile> {
    compilerOptions.jvmTarget = JvmTarget.JVM_21
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

publishing {
    repositories { mavenLocal() }
}

mavenPublishing {
    configure(KotlinJvm())
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions.freeCompilerArgs.add("-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
}

dependencies {
    implementation(project(":annotation"))

    // Arch Toolkit
    implementation(libs.toolkit.arch.lumber)
    // Google
    implementation(libs.google.ksp)
}
