package com.pedrobneto.easy.navigation.buildlogic

import com.android.build.api.dsl.CommonExtension
import com.pedrobneto.easy.navigation.buildlogic.util.projectJavaVersion

internal fun CommonExtension.commonSetup() {
    buildFeatures.buildConfig = true

    androidResources.noCompress.add("")

    compileOptions.sourceCompatibility = projectJavaVersion
    compileOptions.targetCompatibility = projectJavaVersion

    packaging.resources.excludes.add("META-INF/LICENSE")
    packaging.resources.pickFirsts.add("protobuf.meta")
    packaging.jniLibs.keepDebugSymbols.addAll(setOf("*/mips/*.so", "*/mips64/*.so"))
}
