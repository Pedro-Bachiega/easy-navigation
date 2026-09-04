package com.pedrobneto.easy.navigation.buildlogic.util

import org.gradle.api.Project

internal val Project.artifactPrefixProperty: String?
    get() = properties["ARTIFACT_PREFIX"] as? String

internal val Project.versionNameProperty: String?
    get() = properties["VERSION_NAME"] as? String
