package com.pedrobneto.easy.navigation.core.transition

/**
 * Optional callbacks for each navigation mode. A missing callback or null result delegates to
 * the next policy. Predictive pop is independent from regular pop.
 */
interface SceneTransitions {
    val transitionSpec: DefaultTransitionSpec? get() = null
    val popTransitionSpec: DefaultTransitionSpec? get() = null
    val predictivePopTransitionSpec: PredictiveTransitionSpec? get() = null
}
