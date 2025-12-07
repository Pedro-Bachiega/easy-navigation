package com.pedrobneto.easy.navigation.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pedrobneto.easy.navigation.core.Navigation
import com.pedrobneto.easy.navigation.core.adaptive.AdaptiveSceneStrategy
import com.pedrobneto.easy.navigation.model.HomeRoute
import com.pedrobneto.easy.navigation.model.Orientation
import com.pedrobneto.easy.navigation.model.WindowSize
import com.pedrobneto.easy.navigation.registry.GlobalDirectionRegistry

@Composable
fun NavigationSample() {
    MaterialTheme {
        val screenInfo by getCurrentScreenInfo()
        if (!screenInfo.isValid) return@MaterialTheme

        val isUsingAdaptiveLayout by remember(screenInfo) {
            derivedStateOf { screenInfo.windowSize.isAtLeast(WindowSize.Medium) }
        }

        val orientation by remember(screenInfo) {
            derivedStateOf {
                if (screenInfo.orientation == Orientation.Landscape) {
                    AdaptiveSceneStrategy.Orientation.Horizontal
                } else {
                    AdaptiveSceneStrategy.Orientation.Vertical
                }
            }
        }

        // Generated per scope using @Scope annotation
//            val (initialRoute, registries) = remember {
//                FirstScopedRoute to listOf(SampleScopeDirectionRegistry)
//            }

        // Generated per module using library gradle plugin
//            val (initialRoute, registries) = remember {
//                FirstScreenRoute to listOf(SampleDirectionRegistry)
//            }

        // Generated aggregating all modules' registries using application gradle plugin
        val (initialRoute, registries) = remember {
            HomeRoute to listOf(GlobalDirectionRegistry)
        }

        Navigation(
            modifier = Modifier.fillMaxSize(),
            initialRoute = initialRoute,
            directionRegistries = registries,
            sceneStrategy = AdaptiveSceneStrategy(
                isUsingAdaptiveLayout = isUsingAdaptiveLayout,
                orientation = orientation
            )
        )
    }
}
