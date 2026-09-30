package com.pedrobneto.easy.navigation.core.transition

import com.pedrobneto.easy.navigation.core.adaptive.DefaultRegularSceneTransitions

/**
 * The animation policy used by [com.pedrobneto.easy.navigation.core.Navigation].
 *
 * Regular navigation scenes.
 */
data class NavigationTransitions(
    val regular: SceneTransitions = DefaultRegularSceneTransitions(),
)
