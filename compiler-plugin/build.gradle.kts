import com.vanniktech.maven.publish.KotlinJvm

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    signing
    alias(libs.plugins.vanniktech.publish)
}

apply(from = "$rootDir/versioning.gradle.kts")

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi",
            "-opt-in=org.jetbrains.kotlin.fir.symbols.SymbolInternals",
            "-Xcontext-parameters",
        )
    }
}

mavenPublishing { configure(KotlinJvm()) }

dependencies {
    compileOnly(libs.jetbrains.kotlin.compiler)
    implementation(libs.kotlinpoet)
    testImplementation(libs.jetbrains.kotlin.compiler)
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.4")
}

tasks.test {
    useJUnitPlatform()
    testLogging { exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}
