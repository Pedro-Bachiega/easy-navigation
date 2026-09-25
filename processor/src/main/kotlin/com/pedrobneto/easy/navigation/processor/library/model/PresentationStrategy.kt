package com.pedrobneto.easy.navigation.processor.library.model

internal sealed interface PresentationStrategy {
    data class Modal(
        val dismissible: Boolean,
        val transitionsQualifiedName: String? = null,
        val transitionsIsObject: Boolean = false,
    ) : PresentationStrategy
    data class Pane(val strategy: PaneStrategy) : PresentationStrategy
}
