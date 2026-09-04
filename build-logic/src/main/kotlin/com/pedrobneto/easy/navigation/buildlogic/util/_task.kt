package com.pedrobneto.easy.navigation.buildlogic.util

import org.gradle.api.Project
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.jvm.tasks.Jar

@Suppress("UnusedReceiverParameter")
internal fun Project.attachAllTasksIntoAssembleRelease() {
    afterEvaluate {
        val all = tasks.filter { task ->
            if (task is Jar || task is Javadoc) task.name.contains("debug", true)
            else false
        }.map { tasks.named(it.name) }

        tasks.findByName("assembleRelease")?.dependsOn(all)
    }
}
