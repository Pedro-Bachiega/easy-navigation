package com.pedrobneto.easy.navigation.sample.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pedrobneto.easy.navigation.core.Navigation
import com.pedrobneto.easy.navigation.core.adaptive.AdaptiveSceneStrategy
import com.pedrobneto.easy.navigation.core.adaptive.rememberAdaptiveSceneStrategy
import com.pedrobneto.easy.navigation.registry.GlobalDirectionRegistry
import com.pedrobneto.easy.navigation.sample.model.HomeRoute
import com.pedrobneto.easy.navigation.sample.model.Orientation
import com.pedrobneto.easy.navigation.sample.model.WindowSize

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun NavigationSample() {
    MaterialTheme {
        val screenInfo by getCurrentScreenInfo()
        if (!screenInfo.isValid) return@MaterialTheme

        val orientation by remember(screenInfo) {
            derivedStateOf {
                if (screenInfo.orientation == Orientation.Landscape) {
                    AdaptiveSceneStrategy.Orientation.Horizontal
                } else {
                    AdaptiveSceneStrategy.Orientation.Vertical
                }
            }
        }

        val isUsingAdaptiveLayout by remember(screenInfo, orientation) {
            derivedStateOf {
                screenInfo.orientation == Orientation.Landscape &&
                        screenInfo.windowSize.isAtLeast(WindowSize.Medium)
            }
        }

        // Generated per scope using @Scope annotation
//            val (initialRoute, registries) = remember {
//                FirstScopedRoute to listOf(SampleScopeDirectionRegistry)
//            }

        // Generated per module using library gradle plugin
//            val (initialRoute, registries) = remember {
//                HomeRoute to listOf(SampleDirectionRegistry)
//            }

        // Generated aggregating all modules' registries using application gradle plugin
        val (initialRoute, registries) = remember {
            HomeRoute to listOf(GlobalDirectionRegistry)
        }

        Navigation(
            modifier = Modifier.fillMaxSize(),
            initialRoute = initialRoute,
            directionRegistries = registries,
            sceneStrategy = rememberAdaptiveSceneStrategy(
                isUsingAdaptiveLayout = isUsingAdaptiveLayout,
                orientation = orientation
            )
        )
    }
}
