package com.pedrobneto.easy.navigation.core.transition

import androidx.navigation3.scene.Scene
import com.pedrobneto.easy.navigation.core.model.NavigationRoute

/**
 * The logical stack tops and scenes at both ends of a transition.
 *
 * [from] always exits and [to] always enters, including during pop. They may be equal when
 * the layout changes without changing the stack. Scenes expose the consumer's original
 * scene types, including any scene decorators. No context is created for initial presentation.
 */
data class RouteTransitionContext(
    val from: NavigationRoute,
    val to: NavigationRoute,
    val fromScene: Scene<NavigationRoute>,
    val toScene: Scene<NavigationRoute>,
    val operation: NavigationOperation,
)

/** Navigation intent, rather than an inference based on the number of entries in a scene. */
enum class NavigationOperation {
    Forward,
    /** SingleTop may push, replace, or remove existing instances before navigating. */
    SingleTop,
    NewStack,
    Pop,
    PredictivePop,
    SceneChange,
    /** External stack edits or multiple mutations coalesced before a scene was displayed. */
    Unknown,
}
