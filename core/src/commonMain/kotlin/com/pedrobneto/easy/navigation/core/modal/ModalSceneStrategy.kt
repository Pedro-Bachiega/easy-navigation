package com.pedrobneto.easy.navigation.core.modal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.pedrobneto.easy.navigation.core.adaptive.AdaptiveSceneStrategy
import com.pedrobneto.easy.navigation.core.model.NavigationDirection
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import com.pedrobneto.easy.navigation.core.transition.ModalTransitions
import kotlinx.coroutines.flow.first

/** Creates the modal strategy used by custom scene strategy lists. */
@Composable
fun rememberModalSceneStrategy(
    baseSceneStrategy: SceneStrategy<NavigationRoute> = AdaptiveSceneStrategy(
        isUsingAdaptiveLayout = false
    ),
    modalTransitions: ModalTransitions = DefaultModalTransitions()
): SceneStrategy<NavigationRoute> = remember(baseSceneStrategy, modalTransitions) {
    ModalSceneStrategy(baseSceneStrategy, modalTransitions)
}

/** A Navigation 3 overlay strategy that composes a modal over the scene calculated below it. */
class ModalSceneStrategy(
    private val baseSceneStrategy: SceneStrategy<NavigationRoute>,
    private val modalTransitions: ModalTransitions = DefaultModalTransitions(),
) : SceneStrategy<NavigationRoute> {
    override fun SceneStrategyScope<NavigationRoute>.calculateScene(
        entries: List<NavEntry<NavigationRoute>>
    ): Scene<NavigationRoute>? {
        val entry = entries.lastOrNull() ?: return null
        val config = entry.modalConfig ?: return null
        val previousEntries = entries.dropLast(1)

        require(previousEntries.isNotEmpty()) {
            "Modal route cannot be displayed without a scene-base route. " +
                    "Navigate to it from another destination instead of using it as the root."
        }

        val baseScene = if (previousEntries.lastOrNull()?.modalConfig != null) {
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
            dismissible = config.dismissible,
            onDismiss = onBack,
            transitions = config.transitions ?: modalTransitions,
        )
    }

    private val NavEntry<NavigationRoute>.modalConfig: ModalConfig?
        get() = metadata[NavigationDirection.METADATA_MODAL_KEY] as? ModalConfig
}

private class ModalScene(
    override val key: Any,
    private val entry: NavEntry<NavigationRoute>,
    private val baseScene: Scene<NavigationRoute>,
    override val previousEntries: List<NavEntry<NavigationRoute>>,
    override val overlaidEntries: List<NavEntry<NavigationRoute>> = previousEntries,
    private val dismissible: Boolean,
    private val onDismiss: () -> Unit,
    private val transitions: ModalTransitions,
) : OverlayScene<NavigationRoute> {
    override val entries: List<NavEntry<NavigationRoute>> = baseScene.entries + entry

    private val contentVisibility = MutableTransitionState(false)
    private val scrimVisibility = MutableTransitionState(false)

    override val content: @Composable (() -> Unit) = {
        LaunchedEffect(Unit) {
            contentVisibility.targetState = true
            scrimVisibility.targetState = true
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            baseScene.content()
            AnimatedVisibility(
                visibleState = scrimVisibility,
                enter = transitions.scrimEnter,
                exit = transitions.scrimExit,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = dismissible, onClick = onDismiss),
                )
            }
            AnimatedVisibility(
                visibleState = contentVisibility,
                enter = transitions.contentEnter,
                exit = transitions.contentExit,
            ) {
                entry.Content()
            }
        }
    }

    override suspend fun onRemove() {
        contentVisibility.targetState = false
        scrimVisibility.targetState = false
        snapshotFlow {
            contentVisibility.isIdle &&
                    scrimVisibility.isIdle &&
                    !contentVisibility.currentState &&
                    !scrimVisibility.currentState
        }.first { it }
    }

    override fun equals(other: Any?): Boolean = other is ModalScene &&
            key == other.key &&
            entry == other.entry &&
            baseScene == other.baseScene &&
            previousEntries == other.previousEntries &&
            overlaidEntries == other.overlaidEntries &&
            dismissible == other.dismissible &&
            transitions == other.transitions

    override fun hashCode(): Int =
        listOf(
            key,
            entry,
            baseScene,
            previousEntries,
            overlaidEntries,
            dismissible,
            transitions,
        ).fold(1) { result, value -> result * 31 + value.hashCode() }
}
