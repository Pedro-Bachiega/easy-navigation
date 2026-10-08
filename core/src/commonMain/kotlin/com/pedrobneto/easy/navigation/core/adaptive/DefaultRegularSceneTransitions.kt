package com.pedrobneto.easy.navigation.core.adaptive

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.defaultPredictivePopTransitionSpec
import androidx.navigationevent.NavigationEvent
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import com.pedrobneto.easy.navigation.core.transition.DefaultTransitionSpec
import com.pedrobneto.easy.navigation.core.transition.PredictiveTransitionSpec
import com.pedrobneto.easy.navigation.core.transition.SceneTransitions
import com.pedrobneto.easy.navigation.core.transition.originalScene
import kotlin.math.roundToInt

/** Stateless built-in policy and reusable animation helpers. */
object DefaultRegularSceneTransitions : SceneTransitions {
    override val transitionSpec: DefaultTransitionSpec = { context ->
        context.fromScene transitionTo context.toScene
    }
    override val popTransitionSpec: DefaultTransitionSpec = { context ->
        context.fromScene popTo context.toScene
    }
    override val predictivePopTransitionSpec: PredictiveTransitionSpec = { _, swipeEdge ->
        predictivePop(this, swipeEdge)
    }

    private val defaultPredictiveSpec = defaultPredictivePopTransitionSpec<NavigationRoute>()

    /** Forward fallback preserving the built-in adaptive pane behavior. */
    infix fun Scene<NavigationRoute>.transitionTo(target: Scene<NavigationRoute>): ContentTransform {
        if (this !== originalScene || target !== target.originalScene) {
            return originalScene transitionTo target.originalScene
        }
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

    /** Pop fallback preserving the built-in adaptive pane behavior. */
    infix fun Scene<NavigationRoute>.popTo(target: Scene<NavigationRoute>): ContentTransform {
        if (this !== originalScene || target !== target.originalScene) {
            return originalScene popTo target.originalScene
        }
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

    /** Slide the entire scene forward, independently of the adaptive pane layout. */
    fun fullSlideIn(): ContentTransform =
        slideInHorizontally { fullWidth -> fullWidth } togetherWith
                slideOutHorizontally { fullWidth -> -fullWidth }

    private fun extraFadeIn(): ContentTransform = none()

    private fun ratioSlideOut(current: AdaptiveSceneStrategy.DualPaneScene): ContentTransform =
        slideInHorizontally { fullWidth -> -(fullWidth * current.entryRatio).roundToInt() } togetherWith
                slideOutHorizontally { fullWidth -> (fullWidth * current.entryRatio).roundToInt() }

    /** Slide the entire scene back, independently of the adaptive pane layout. */
    fun fullSlideOut(): ContentTransform =
        slideInHorizontally { fullWidth -> -fullWidth } togetherWith
                slideOutHorizontally { fullWidth -> fullWidth }

    private fun extraFadeOut(): ContentTransform = none()

    /** Explicitly disable animation; unlike null, this does not request a fallback. */
    fun none(): ContentTransform = EnterTransition.None togetherWith ExitTransition.None

    /** Predictive fallback driven by Navigation 3's gesture animation. */
    fun predictivePop(
        scope: AnimatedContentTransitionScope<Scene<NavigationRoute>>,
        @NavigationEvent.SwipeEdge swipeEdge: Int,
    ): ContentTransform = defaultPredictiveSpec.invoke(scope, swipeEdge)
}
