package com.pedrobneto.easy.navigation.core.adaptive

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.scene.Scene
import com.pedrobneto.easy.navigation.core.model.NavigationRoute

internal infix fun Scene<NavigationRoute>.transitionTo(target: Scene<NavigationRoute>): ContentTransform {
    val targetIsDualPane = target is AdaptiveSceneStrategy.DualPaneScene
    val currentIsDualPane = this is AdaptiveSceneStrategy.DualPaneScene
    val currentIsAdaptivePane = this is AdaptiveSceneStrategy.AdaptivePaneScene

    return when {
        currentIsAdaptivePane && targetIsDualPane && target.isAfter(this) -> extraFadeIn()
        currentIsDualPane && targetIsDualPane && target.isReplacingExtra(this) -> none()
        currentIsDualPane && targetIsDualPane && target.isAfter(this) -> halfSlideIn()
        else -> fullSlideIn()
    }
}

internal infix fun Scene<NavigationRoute>.popTo(target: Scene<NavigationRoute>): ContentTransform {
    val targetIsDualPane = target is AdaptiveSceneStrategy.DualPaneScene
    val currentIsDualPane = this is AdaptiveSceneStrategy.DualPaneScene
    val targetIsAdaptivePane = target is AdaptiveSceneStrategy.AdaptivePaneScene

    return when {
        currentIsDualPane && targetIsDualPane && this.isAfter(target) -> halfSlideOut()
        currentIsDualPane && targetIsAdaptivePane && this.isAfter(target) -> extraFadeOut()
        else -> fullSlideOut()
    }
}

private fun halfSlideIn(): ContentTransform =
    slideInHorizontally { fullWidth -> fullWidth / 2 } togetherWith
            slideOutHorizontally { fullWidth -> -fullWidth / 2 }

private fun fullSlideIn(): ContentTransform =
    slideInHorizontally { fullWidth -> fullWidth } togetherWith
            slideOutHorizontally { fullWidth -> -fullWidth }

private fun extraFadeIn(): ContentTransform = none()

private fun halfSlideOut(): ContentTransform =
    slideInHorizontally { fullWidth -> -fullWidth / 2 } togetherWith
            slideOutHorizontally { fullWidth -> fullWidth / 2 }

private fun fullSlideOut(): ContentTransform =
    slideInHorizontally { fullWidth -> -fullWidth } togetherWith
            slideOutHorizontally { fullWidth -> fullWidth }

private fun extraFadeOut(): ContentTransform = none()

private fun none(): ContentTransform =
    fadeIn(initialAlpha = 1f) togetherWith fadeOut(targetAlpha = 1f)
