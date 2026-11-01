package com.pedrobneto.easy.navigation.core.transition

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent
import com.pedrobneto.easy.navigation.core.model.NavigationRoute

/** Return null to delegate to the next animation policy. */
typealias DefaultTransitionSpec = AnimatedContentTransitionScope<Scene<NavigationRoute>>.(RouteTransitionContext) -> ContentTransform?

/** Predictive pop has an independent fallback, and retains the gesture's swipe edge. */
typealias PredictiveTransitionSpec = AnimatedContentTransitionScope<Scene<NavigationRoute>>.(RouteTransitionContext, @NavigationEvent.SwipeEdge Int) -> ContentTransform?
