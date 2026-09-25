package com.pedrobneto.easy.navigation.core.adaptive

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.defaultPredictivePopTransitionSpec
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import com.pedrobneto.easy.navigation.core.transition.DefaultTransitionSpec
import com.pedrobneto.easy.navigation.core.transition.PredictiveTransitionSpec
import com.pedrobneto.easy.navigation.core.transition.SceneTransitions
import kotlin.math.roundToInt

class DefaultRegularSceneTransitions : SceneTransitions {
    override val transitionSpec: DefaultTransitionSpec = { initialState transitionTo targetState }
    override val popTransitionSpec: DefaultTransitionSpec = { initialState popTo targetState }
    override val predictivePopTransitionSpec: PredictiveTransitionSpec =
        defaultPredictivePopTransitionSpec()

    internal infix fun Scene<NavigationRoute>.transitionTo(target: Scene<NavigationRoute>): ContentTransform {
        val targetIsDualPane = target is AdaptiveSceneStrategy.DualPaneScene
        val currentIsDualPane = this is AdaptiveSceneStrategy.DualPaneScene
        val currentIsAdaptivePane = this is AdaptiveSceneStrategy.AdaptivePaneScene

        return when {
            currentIsAdaptivePane && targetIsDualPane && target.isAfter(this) -> extraFadeIn()
            currentIsDualPane && targetIsDualPane && target.isReplacingExtra(this) -> none()
            currentIsDualPane && targetIsDualPane && target.isAfter(this) -> {
                ratioSlideIn(target)
            }

            else -> fullSlideIn()
        }
    }

    internal infix fun Scene<NavigationRoute>.popTo(target: Scene<NavigationRoute>): ContentTransform {
        val targetIsDualPane = target is AdaptiveSceneStrategy.DualPaneScene
        val currentIsDualPane = this is AdaptiveSceneStrategy.DualPaneScene
        val targetIsAdaptivePane = target is AdaptiveSceneStrategy.AdaptivePaneScene

        return when {
            currentIsDualPane && targetIsDualPane && this.isAfter(target) -> {
                ratioSlideOut(this)
            }

            currentIsDualPane && targetIsAdaptivePane && this.isAfter(target) -> extraFadeOut()
            else -> fullSlideOut()
        }
    }

    private fun ratioSlideIn(target: AdaptiveSceneStrategy.DualPaneScene): ContentTransform =
        slideInHorizontally { fullWidth -> (fullWidth * target.entryRatio).roundToInt() } togetherWith
                slideOutHorizontally { fullWidth -> -(fullWidth * target.entryRatio).roundToInt() }

    private fun fullSlideIn(): ContentTransform =
        slideInHorizontally { fullWidth -> fullWidth } togetherWith
                slideOutHorizontally { fullWidth -> -fullWidth }

    private fun extraFadeIn(): ContentTransform = none()

    private fun ratioSlideOut(current: AdaptiveSceneStrategy.DualPaneScene): ContentTransform =
        slideInHorizontally { fullWidth -> -(fullWidth * current.entryRatio).roundToInt() } togetherWith
                slideOutHorizontally { fullWidth -> (fullWidth * current.entryRatio).roundToInt() }

    private fun fullSlideOut(): ContentTransform =
        slideInHorizontally { fullWidth -> -fullWidth } togetherWith
                slideOutHorizontally { fullWidth -> fullWidth }

    private fun extraFadeOut(): ContentTransform = none()

    private fun none(): ContentTransform =
        fadeIn(initialAlpha = 1f) togetherWith fadeOut(targetAlpha = 1f)
}