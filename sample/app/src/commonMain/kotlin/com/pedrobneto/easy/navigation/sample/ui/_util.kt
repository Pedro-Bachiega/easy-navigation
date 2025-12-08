package com.pedrobneto.easy.navigation.sample.ui

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.pedrobneto.easy.navigation.sample.model.Orientation
import com.pedrobneto.easy.navigation.sample.model.ScreenInfo
import com.pedrobneto.easy.navigation.sample.model.WindowSize

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun getCurrentScreenInfo(): State<ScreenInfo> {
    val windowInfo = LocalWindowInfo.current
    val adaptiveInfo = currentWindowAdaptiveInfo()

    var width by remember { mutableStateOf(windowInfo.containerDpSize.width) }
    var height by remember { mutableStateOf(windowInfo.containerDpSize.height) }

    val orientation by remember(width, height) {
        derivedStateOf {
            if (height.value < width.value) Orientation.Landscape else Orientation.Portrait
        }
    }

    val info by remember(width, height, orientation) {
        derivedStateOf {
            ScreenInfo(
                dpSize = windowInfo.containerDpSize,
                windowSize = adaptiveInfo.windowSizeClass.screenSize(orientation),
                orientation = orientation,
            )
        }
    }

    LaunchedEffect(windowInfo, adaptiveInfo) {
        width = windowInfo.containerSize.width.dp
        height = windowInfo.containerSize.height.dp
    }

    return remember(info) { mutableStateOf(info) }
}

private fun WindowSizeClass.screenSize(orientation: Orientation) = when (orientation) {
    Orientation.Landscape -> landscapeScreenSize
    Orientation.Portrait -> portraitScreenSize
}

private val WindowSizeClass.landscapeScreenSize: WindowSize
    get() = when {
        isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND) -> WindowSize.Large
        isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> WindowSize.Medium
        else -> WindowSize.Small
    }
private val WindowSizeClass.portraitScreenSize: WindowSize
    get() = when {
        isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND) -> WindowSize.Large
        isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) -> WindowSize.Medium
        else -> WindowSize.Small
    }
