package com.pedrobneto.easy.navigation.core.modal

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.pedrobneto.easy.navigation.core.NavigationController
import com.pedrobneto.easy.navigation.core.annotation.SafeNavigationApi
import com.pedrobneto.easy.navigation.core.annotation.UnsafeNavigationApi
import com.pedrobneto.easy.navigation.core.model.LaunchStrategy
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import kotlin.reflect.KClass

/** Navigation operations available to a modal destination. */
class ModalScope internal constructor(
    @PublishedApi internal val controller: NavigationController,
) {
    internal val route: NavigationRoute = controller.currentRoute
    private var systemBackRequestHandler: (() -> Unit)? = null

    val canNavigateUp: Boolean get() = controller.canNavigateUp
    val currentRoute: NavigationRoute get() = controller.currentRoute
    val currentIndex: Int get() = controller.currentIndex

    fun navigateTo(route: NavigationRoute, strategy: LaunchStrategy = LaunchStrategy.Default) =
        controller.navigateTo(route, strategy)

    @UnsafeNavigationApi
    fun navigateTo(deeplink: String, strategy: LaunchStrategy = LaunchStrategy.Default) =
        controller.navigateTo(deeplink, strategy)

    @UnsafeNavigationApi
    inline fun <reified T> navigateTo(
        deeplink: String,
        payload: T,
        strategy: LaunchStrategy = LaunchStrategy.Default,
    ) = controller.navigateTo(deeplink, payload, strategy)

    @SafeNavigationApi
    inline fun <reified T> safeNavigateTo(
        deeplink: String,
        payload: T,
        strategy: LaunchStrategy = LaunchStrategy.Default,
    ): Boolean = controller.safeNavigateTo(deeplink, payload, strategy)

    @SafeNavigationApi
    fun safeNavigateTo(
        deeplink: String,
        strategy: LaunchStrategy = LaunchStrategy.Default,
    ): Boolean = controller.safeNavigateTo(deeplink, strategy)

    @UnsafeNavigationApi
    fun navigateUp() = controller.navigateUp()

    @SafeNavigationApi
    fun safeNavigateUp(): Boolean = controller.safeNavigateUp()

    @UnsafeNavigationApi
    fun popUpTo(route: NavigationRoute, inclusive: Boolean = false) =
        controller.popUpTo(route, inclusive)

    @UnsafeNavigationApi
    fun popUpTo(routeClass: KClass<out NavigationRoute>, inclusive: Boolean = false) =
        controller.popUpTo(routeClass, inclusive)

    @SafeNavigationApi
    fun safePopUpTo(route: NavigationRoute, inclusive: Boolean = false): Boolean =
        controller.safePopUpTo(route, inclusive)

    /**
     * Registers the modal component's response to a system back request.
     *
     * The handler should apply the component's own close policy. If no handler is registered,
     * system back falls back to the controller's regular navigate-up behavior.
     */
    fun setSystemBackRequestHandler(handler: (() -> Unit)?) {
        systemBackRequestHandler = handler
    }

    internal fun dispatchSystemBackRequest(): Boolean {
        val handler = systemBackRequestHandler ?: return false
        handler()
        return true
    }
}

/**
 * Provides the navigation controller scoped to a modal destination.
 *
 * Modal components should finish their own exit animation before calling an operation that
 * removes or replaces the modal destination.
 */
val LocalModalScope: ProvidableCompositionLocal<ModalScope> =
    staticCompositionLocalOf { error("Modal navigation is only available inside a @Modal destination.") }
