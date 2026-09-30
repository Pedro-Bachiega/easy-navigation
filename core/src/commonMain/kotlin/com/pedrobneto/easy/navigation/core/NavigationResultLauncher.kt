@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.pedrobneto.easy.navigation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSerializable
import com.pedrobneto.easy.navigation.core.annotation.SafeNavigationApi
import com.pedrobneto.easy.navigation.core.annotation.UnsafeNavigationApi
import com.pedrobneto.easy.navigation.core.model.LaunchStrategy
import com.pedrobneto.easy.navigation.core.model.NavigationDeeplink
import com.pedrobneto.easy.navigation.core.model.NavigationResult
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

/**
 * Remembers a launcher that opens a destination and receives its confirmed or cancelled result.
 * The callback is reattached when the caller's composition is restored. [T] must have a
 * `kotlinx.serialization` serializer so a pending result can survive process recreation.
 */
@Composable
inline fun <reified T> rememberNavigationResultLauncher(
    noinline onResult: (NavigationResult<T>) -> Unit,
): NavigationResultLauncher<T> {
    val controller = LocalNavigationController.current
    val launcherIdState = rememberSerializable(
        controller,
        stateSerializer = serializer<Long>(),
        init = { mutableStateOf(controller.allocateResultLauncherId()) },
    )
    val launcherId = launcherIdState.value
    val callbackState = rememberUpdatedState(onResult)
    val resultSerializer = serializer<T>()
    val launcher = remember(controller, launcherId, resultSerializer) {
        NavigationResultLauncher<T>(controller, launcherId)
    }

    DisposableEffect(controller, launcherId, resultSerializer) {
        controller.registerResultCallback(launcherId, resultSerializer) { result ->
            callbackState.value(result)
        }
        onDispose { controller.unregisterResultCallback(launcherId) }
    }

    return launcher
}

/** A typed launcher for navigation destinations that return a result of type [T]. */
class NavigationResultLauncher<T> @PublishedApi internal constructor(
    @PublishedApi internal val controller: NavigationController,
    @PublishedApi internal val launcherId: Long,
) {
    /** Navigates to [route] and returns `false` if this launcher already has a pending request. */
    fun navigateForResult(
        route: NavigationRoute,
        strategy: LaunchStrategy = LaunchStrategy.Default,
    ): Boolean = controller.navigateForResult(route, strategy, launcherId)

    /**
     * Resolves [deeplink] and navigates to the matching route.
     *
     * @throws IllegalArgumentException if the deeplink is malformed or has no matching direction.
     */
    @UnsafeNavigationApi
    fun navigateForResult(
        deeplink: String,
        strategy: LaunchStrategy = LaunchStrategy.Default,
    ): Boolean {
        if (controller.isResultLauncherPending(launcherId)) return false
        val route = NavigationDeeplink(deeplink).resolve(controller.json, controller.directions)
        return controller.navigateForResult(route, strategy, launcherId)
    }

    /**
     * Resolves [deeplink] and merges [payload] with its path and query arguments.
     *
     * @throws IllegalArgumentException if the deeplink is malformed, has no matching direction,
     * or the payload cannot be encoded as an object.
     */
    @UnsafeNavigationApi
    inline fun <reified P> navigateForResult(
        deeplink: String,
        payload: P,
        strategy: LaunchStrategy = LaunchStrategy.Default,
    ): Boolean {
        if (controller.isResultLauncherPending(launcherId)) return false
        val route = NavigationDeeplink(deeplink).resolve(controller.json, controller.directions, payload)
        return controller.navigateForResult(route, strategy, launcherId)
    }

    /** Safely resolves [deeplink], returning `false` for invalid links or an occupied launcher. */
    @SafeNavigationApi
    fun safeNavigateForResult(
        deeplink: String,
        strategy: LaunchStrategy = LaunchStrategy.Default,
    ): Boolean = runCatching { navigateForResult(deeplink, strategy) }.getOrDefault(false)

    /** Safely resolves [deeplink] and merges [payload], returning `false` on failure. */
    @SafeNavigationApi
    inline fun <reified P> safeNavigateForResult(
        deeplink: String,
        payload: P,
        strategy: LaunchStrategy = LaunchStrategy.Default,
    ): Boolean = runCatching { navigateForResult(deeplink, payload, strategy) }.getOrDefault(false)
}
