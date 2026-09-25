package com.pedrobneto.easy.navigation.core.transition

import com.pedrobneto.easy.navigation.core.adaptive.DefaultRegularSceneTransitions
import com.pedrobneto.easy.navigation.core.modal.DefaultModalTransitions

/**
 * The animation policy used by [com.pedrobneto.easy.navigation.core.Navigation].
 *
 * Regular scenes and modal overlays are configured together here, even though Navigation 3
 * renders them through different mechanisms internally.
 */
data class NavigationTransitions(
    val regular: SceneTransitions = DefaultRegularSceneTransitions(),
    val modal: ModalTransitions = DefaultModalTransitions(),
)
