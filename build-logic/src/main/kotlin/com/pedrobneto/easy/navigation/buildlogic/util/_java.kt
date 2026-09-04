package com.pedrobneto.easy.navigation.buildlogic.util

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

internal val projectJavaVersion: JavaVersion = JavaVersion.VERSION_21
internal val projectJavaTarget: JvmTarget = JvmTarget.JVM_21
internal const val projectJavaVersionCode: Int = 21
internal const val projectJavaVersionName: String = projectJavaVersionCode.toString()
