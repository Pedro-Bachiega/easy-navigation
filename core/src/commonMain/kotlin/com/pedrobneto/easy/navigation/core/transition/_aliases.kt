package com.pedrobneto.easy.navigation.core.transition

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent
import com.pedrobneto.easy.navigation.core.model.NavigationRoute

typealias DefaultTransitionSpec = AnimatedContentTransitionScope<Scene<NavigationRoute>>.() -> ContentTransform
typealias PredictiveTransitionSpec = AnimatedContentTransitionScope<Scene<NavigationRoute>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform
