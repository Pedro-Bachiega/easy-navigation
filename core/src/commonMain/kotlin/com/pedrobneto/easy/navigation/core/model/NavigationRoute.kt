package com.pedrobneto.easy.navigation.core.model

import androidx.navigation3.runtime.NavKey
import com.pedrobneto.easy.navigation.core.transition.SceneTransitions

/**
 * Represents a route in the navigation graph.
 *
 * This is an interface that should be extended by all routes in the application.
 */
interface NavigationRoute : NavKey {
    /**
     * Optional animation policy owned by this route.
     *
     * Used when opening this route, or closing it through pop or predictive pop. Missing
     * callbacks and null results fall back to the policy configured on Navigation.
     * This method is not invoked for the initial or restored presentation.
     */
    fun transitions(): SceneTransitions? = null
}
