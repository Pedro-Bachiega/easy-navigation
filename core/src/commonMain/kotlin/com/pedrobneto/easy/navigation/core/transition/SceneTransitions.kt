package com.pedrobneto.easy.navigation.core.transition

interface SceneTransitions {
    val transitionSpec: DefaultTransitionSpec
    val popTransitionSpec: DefaultTransitionSpec
    val predictivePopTransitionSpec: PredictiveTransitionSpec
}
