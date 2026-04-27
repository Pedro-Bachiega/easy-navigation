package com.pedrobneto.easy.navigation.sample.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pedrobneto.easy.navigation.core.Navigation
import com.pedrobneto.easy.navigation.core.adaptive.AdaptiveSceneStrategy
import com.pedrobneto.easy.navigation.core.adaptive.rememberAdaptiveSceneStrategies
import com.pedrobneto.easy.navigation.registry.AppDirectionRegistry
import com.pedrobneto.easy.navigation.sample.model.HomeRoute
import com.pedrobneto.easy.navigation.sample.model.Orientation
import com.pedrobneto.easy.navigation.sample.model.WindowSize

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun NavigationSample() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = SampleColors.Ocean,
            onPrimary = SampleColors.OnOcean,
            primaryContainer = SampleColors.Mint,
            onPrimaryContainer = SampleColors.DeepInk,
            secondary = SampleColors.Coral,
            onSecondary = SampleColors.OnCoral,
            secondaryContainer = SampleColors.Sun,
            onSecondaryContainer = SampleColors.DeepInk,
            background = SampleColors.Canvas,
            onBackground = SampleColors.DeepInk,
            surface = SampleColors.Surface,
            onSurface = SampleColors.DeepInk,
            surfaceVariant = SampleColors.SoftBlue,
            onSurfaceVariant = SampleColors.MutedInk,
        )
    ) {
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

        // Generated per scope using @Scope annotation and the gradle plugin
//            val (initialRoute, registries) = remember {
//                FirstScopedRoute to listOf(SampleScopeDirectionRegistry)
//            }

        // Generated per module using the gradle plugin
        val (initialRoute, registries) = remember {
            HomeRoute to listOf(AppDirectionRegistry)
        }

        Navigation(
            modifier = Modifier.fillMaxSize(),
            initialRoute = initialRoute,
            directionRegistries = registries,
            sceneStrategies = rememberAdaptiveSceneStrategies(
                isUsingAdaptiveLayout = isUsingAdaptiveLayout,
                orientation = orientation
            )
        )
    }
}
