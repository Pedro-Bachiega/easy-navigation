package com.pedrobneto.easy.navigation.sample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pedrobneto.easy.navigation.core.Navigation
import com.pedrobneto.easy.navigation.core.NavigationController
import com.pedrobneto.easy.navigation.core.adaptive.AdaptiveSceneStrategy
import com.pedrobneto.easy.navigation.core.adaptive.rememberAdaptiveSceneStrategies
import com.pedrobneto.easy.navigation.core.extension.rememberNavBackStack
import com.pedrobneto.easy.navigation.core.model.LaunchStrategy
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import com.pedrobneto.easy.navigation.core.rememberNavigationController
import com.pedrobneto.easy.navigation.registry.AppDirectionRegistry
import com.pedrobneto.easy.navigation.sample.model.DetailsActivityRoute
import com.pedrobneto.easy.navigation.sample.model.DetailsFaresRoute
import com.pedrobneto.easy.navigation.sample.model.DetailsOverviewRoute
import com.pedrobneto.easy.navigation.sample.model.DetailsRoute
import com.pedrobneto.easy.navigation.sample.model.ExtraDetailsRoute
import com.pedrobneto.easy.navigation.sample.model.HomeRoute
import com.pedrobneto.easy.navigation.sample.model.Orientation
import com.pedrobneto.easy.navigation.sample.model.SettingsRoute
import com.pedrobneto.easy.navigation.sample.model.WindowSize

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun NavigationSample() {
    var isDarkMode by rememberSaveable { mutableStateOf(false) }

    // Generated per scope using @Scope annotation and the gradle plugin
//            val (initialRoute, registries) = remember {
//                FirstScopedRoute to listOf(SampleScopeDirectionRegistry)
//            }

    // Generated per module using the gradle plugin
    val (initialRoute, registries) = remember {
        HomeRoute to listOf(AppDirectionRegistry)
    }
    // The app owns the saveable stack and controller before handing them to Navigation.
    val backStack = rememberNavBackStack(
        initialRoute = initialRoute,
        registries = registries
    )
    val navigationController = rememberNavigationController(
        initialRoute = initialRoute,
        directionRegistries = registries,
        backStack = backStack
    )

    MaterialTheme(
        colorScheme = if (isDarkMode) sampleDarkColorScheme() else sampleLightColorScheme()
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            ControllerShelf(
                controller = navigationController,
                isDarkMode = isDarkMode,
                onDarkModeChange = { isDarkMode = it },
                onNavigateHome = {
                    navigationController.navigateTo(
                        route = HomeRoute,
                        strategy = LaunchStrategy.SingleTop(clearTop = true)
                    )
                },
                onNavigateUp = navigationController::safeNavigateUp
            )
            Navigation(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                initialRoute = initialRoute,
                directionRegistries = registries,
                controller = navigationController,
                sceneStrategies = rememberAdaptiveSceneStrategies(
                    isUsingAdaptiveLayout = isUsingAdaptiveLayout,
                    orientation = orientation
                )
            )
        }
    }
}

@Composable
private fun ControllerShelf(
    controller: NavigationController,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateUp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text("Controller-owned shell", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Depth ${controller.currentIndex + 1} - ${controller.currentRoute.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onNavigateUp,
                enabled = controller.canNavigateUp
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            IconButton(onClick = onNavigateHome) {
                Icon(Icons.Default.Home, contentDescription = "Home")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                modifier = Modifier.weight(1f),
                text = "Theme stays outside destinations; depth and current route restore with the stack.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Switch(
                checked = isDarkMode,
                onCheckedChange = onDarkModeChange
            )
        }
    }
}

private val NavigationRoute.label: String
    get() = when (this) {
        is HomeRoute -> "Home"
        is DetailsRoute -> "Details #$id"
        is DetailsOverviewRoute -> "Overview #$id"
        is DetailsActivityRoute -> "Activity #$id"
        is DetailsFaresRoute -> "Fares #$id"
        is ExtraDetailsRoute -> "Extra details"
        is SettingsRoute -> "Settings"
        else -> this::class.simpleName ?: "Route"
    }

private fun sampleLightColorScheme() = lightColorScheme(
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

private fun sampleDarkColorScheme() = darkColorScheme(
    primary = SampleColors.Mint,
    onPrimary = SampleColors.DeepInk,
    primaryContainer = SampleColors.Ocean,
    onPrimaryContainer = SampleColors.OnOcean,
    secondary = SampleColors.Sun,
    onSecondary = SampleColors.DeepInk,
    secondaryContainer = SampleColors.Coral,
    onSecondaryContainer = SampleColors.OnCoral,
    background = SampleColors.Night,
    onBackground = SampleColors.NightInk,
    surface = SampleColors.NightSurface,
    onSurface = SampleColors.NightInk,
    surfaceVariant = SampleColors.NightVariant,
    onSurfaceVariant = SampleColors.NightMuted,
)
