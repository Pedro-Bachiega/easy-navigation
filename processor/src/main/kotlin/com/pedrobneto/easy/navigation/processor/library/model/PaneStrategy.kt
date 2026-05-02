package com.pedrobneto.easy.navigation.processor.library.model

internal sealed class PaneStrategy {
    data class Adaptive(val ratio: Float = DEFAULT_RATIO) : PaneStrategy() {
        companion object {
            const val DEFAULT_RATIO = 1f
        }
    }

    data object Single : PaneStrategy()

    data class Extra(val hosts: List<PaneHost>) : PaneStrategy() {
        data class PaneHost(val route: QualifiedName, val ratio: Float = DEFAULT_RATIO) {
            companion object {
                const val DEFAULT_RATIO = .5f
            }
        }
    }
}
