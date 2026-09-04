package com.pedrobneto.easy.navigation.buildlogic

import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

internal fun KotlinProjectExtension.setupOptIns() {
    sourceSets.all {
//        languageSettings.enableLanguageFeature("ExplicitBackingFields")
    }
}
