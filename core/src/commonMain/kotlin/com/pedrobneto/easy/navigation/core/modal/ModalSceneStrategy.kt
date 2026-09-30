package com.pedrobneto.easy.navigation.core.modal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.pedrobneto.easy.navigation.core.LocalNavigationController
import com.pedrobneto.easy.navigation.core.adaptive.AdaptiveSceneStrategy
import com.pedrobneto.easy.navigation.core.model.NavigationDirection
import com.pedrobneto.easy.navigation.core.model.NavigationRoute

/** Creates the modal strategy used by custom scene strategy lists. */
@Composable
fun rememberModalSceneStrategy(
    baseSceneStrategy: SceneStrategy<NavigationRoute> = AdaptiveSceneStrategy(
        isUsingAdaptiveLayout = false
    ),
): SceneStrategy<NavigationRoute> = remember(baseSceneStrategy) {
    ModalSceneStrategy(baseSceneStrategy)
}

/** A Navigation 3 overlay strategy that composes a modal over the scene calculated below it. */
class ModalSceneStrategy(
    private val baseSceneStrategy: SceneStrategy<NavigationRoute>,
) : SceneStrategy<NavigationRoute> {
    override fun SceneStrategyScope<NavigationRoute>.calculateScene(
        entries: List<NavEntry<NavigationRoute>>
    ): Scene<NavigationRoute>? {
        val entry = entries.lastOrNull() ?: return null
        if (!entry.isModal) return null

        val previousEntries = entries.dropLast(1)
        require(previousEntries.isNotEmpty()) {
            "Modal route cannot be displayed without a scene-base route. " +
                    "Navigate to it from another destination instead of using it as the root."
        }

        val baseScene = if (previousEntries.lastOrNull()?.isModal == true) {
            with(this@ModalSceneStrategy) {
                calculateScene(previousEntries)
            }
        } else {
            with(baseSceneStrategy) {
                calculateScene(previousEntries)
            }
        } ?: return null

        return ModalScene(
            key = entry.contentKey,
            entry = entry,
            baseScene = baseScene,
            previousEntries = previousEntries,
        )
    }

    private val NavEntry<NavigationRoute>.isModal: Boolean
        get() = metadata[NavigationDirection.METADATA_MODAL_KEY] == true
}

private class ModalScene(
    override val key: Any,
    private val entry: NavEntry<NavigationRoute>,
    private val baseScene: Scene<NavigationRoute>,
    override val previousEntries: List<NavEntry<NavigationRoute>>,
    override val overlaidEntries: List<NavEntry<NavigationRoute>> = previousEntries,
) : OverlayScene<NavigationRoute> {
    override val entries: List<NavEntry<NavigationRoute>> = baseScene.entries + entry

    override val content: @Composable (() -> Unit) = {
        val controller = LocalNavigationController.current
        val modalScope = remember(controller, entry.contentKey) { ModalScope(controller) }

        DisposableEffect(controller, modalScope) {
            controller.registerModalScope(modalScope)
            onDispose { controller.unregisterModalScope(modalScope) }
        }

        Box(Modifier.fillMaxSize()) {
            baseScene.content()
            CompositionLocalProvider(LocalModalScope provides modalScope) {
                entry.Content()
            }
        }
    }

    override fun equals(other: Any?): Boolean = other is ModalScene &&
            key == other.key &&
            entry == other.entry &&
            baseScene == other.baseScene &&
            previousEntries == other.previousEntries &&
            overlaidEntries == other.overlaidEntries

    override fun hashCode(): Int =
        listOf(key, entry, baseScene, previousEntries, overlaidEntries)
            .fold(1) { result, value -> result * 31 + value.hashCode() }
}
