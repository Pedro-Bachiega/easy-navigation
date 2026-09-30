package com.pedrobneto.easy.navigation.processor.library.model

internal sealed interface PresentationStrategy {
    data object Modal : PresentationStrategy
    data class Pane(val strategy: PaneStrategy) : PresentationStrategy
}
