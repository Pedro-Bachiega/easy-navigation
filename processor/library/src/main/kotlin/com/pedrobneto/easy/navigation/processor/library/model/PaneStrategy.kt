package com.pedrobneto.easy.navigation.processor.library.model

internal sealed class PaneStrategy {
    data class Adaptive(val ratio: Float = 0.5f) : PaneStrategy()
    data object Single : PaneStrategy()
    data class Extra(val host: QualifiedName, val ratio: Float = 0.5f) : PaneStrategy()
}
