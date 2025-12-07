package com.pedrobneto.easy.navigation.processor.library.model

internal sealed class PaneStrategy {
    data class Adaptive(val ratio: Float = 1f) : PaneStrategy()
    data object Single : PaneStrategy()
    data class Extra(val hosts: List<PaneHost>) : PaneStrategy() {
        data class PaneHost(val route: QualifiedName, val ratio: Float = 0.5f)
    }
}
