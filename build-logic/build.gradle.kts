plugins {
    id("java-gradle-plugin")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.jetbrains.kotlin.dsl)
    alias(libs.plugins.jetbrains.serialization)

    alias(libs.plugins.lint.detekt)
    alias(libs.plugins.lint.ktlint)
    id(libs.plugins.quality.jacoco.get().pluginId)
}

kotlin { jvmToolchain(21) }
jacoco { toolVersion = libs.versions.jacoco.get() }

group = "com.pedrobneto.easy.navigation.buildlogic"
version = "1.0.0"

dependencies {
    compileOnly(gradleApi())

    implementation(libs.jetbrains.serialization)

    implementation(libs.plugin.androidx.plugin)
    implementation(libs.plugin.lint.detekt)
    implementation(libs.plugin.lint.ktlint)
//    implementation(libs.plugin.jetbrains.dokka)
    implementation(libs.plugin.jetbrains.kotlin.plugin)
    implementation(libs.plugin.jetbrains.kover)
    implementation(libs.plugin.vanniktech.publish)
}

sourceSets {
    main {
        java { srcDirs("src/main/java") }
        kotlin { srcDirs("src/main/kotlin") }
    }
}

gradlePlugin {
    plugins {
        //region Android
        create("plugin-android-application") {
            id = "plugin-android-application"
            displayName = "Android Application Plugin"
            description = "\\o/"
            implementationClass = "com.pedrobneto.easy.navigation.buildlogic.android.ApplicationPlugin"
        }
        //endregion

        //region Browser
        create("plugin-browser-application") {
            id = "plugin-browser-application"
            displayName = "Browser Application Plugin"
            description = "\\o/"
            implementationClass = "com.pedrobneto.easy.navigation.buildlogic.browser.ApplicationPlugin"
        }
        //endregion

        //region Desktop
        create("plugin-desktop-application") {
            id = "plugin-desktop-application"
            displayName = "Desktop Application Plugin"
            description = "\\o/"
            implementationClass = "com.pedrobneto.easy.navigation.buildlogic.desktop.ApplicationPlugin"
        }
        //endregion

        //region Multiplatform
        create("plugin-multiplatform-library") {
            id = "plugin-multiplatform-library"
            displayName = "Multiplatform Library Plugin"
            description = "\\o/"
            implementationClass = "com.pedrobneto.easy.navigation.buildlogic.multiplatform.LibraryPlugin"
        }
        create("plugin-multiplatform-publish") {
            id = "plugin-multiplatform-publish"
            displayName = "Multiplatform Publish Plugin"
            description = "\\o/"
            implementationClass = "com.pedrobneto.easy.navigation.buildlogic.multiplatform.PublishPlugin"
        }
        //endregion

        //region Gradle
        create("plugin-gradle-publish") {
            id = "plugin-gradle-publish"
            displayName = "Gradle Publish Plugin"
            description = "\\o/"
            implementationClass = "com.pedrobneto.easy.navigation.buildlogic.gradle.PublishPlugin"
        }
        //endregion

        //region Generic
        create("plugin-compose") {
            id = "plugin-compose"
            displayName = "Easy Navigation Compose Plugin"
            description = "Compose Plugin"
            implementationClass = "com.pedrobneto.easy.navigation.buildlogic.ComposePlugin"
        }
        create("plugin-optimize") {
            id = "plugin-optimize"
            displayName = "Easy Navigation Optimization Plugin"
            description = "Optimize dependencies"
            implementationClass =
                "com.pedrobneto.easy.navigation.buildlogic.OptimizeDependenciesAndFilterTasksPlugin"
        }

        create("plugin-lint") {
            id = "plugin-lint"
            displayName = "Easy Navigation Lint Plugin"
            description = "Enables and configure lint for module"
            implementationClass = "com.pedrobneto.easy.navigation.buildlogic.LintPlugin"
        }

        create("plugin-test") {
            id = "plugin-test"
            displayName = "Easy Navigation Test Plugin"
            description = "Enables and configure test for module"
            implementationClass = "com.pedrobneto.easy.navigation.buildlogic.TestPlugin"
        }
        //endregion
    }
}
