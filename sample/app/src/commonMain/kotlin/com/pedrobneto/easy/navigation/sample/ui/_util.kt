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
import androidx.compose.ui.graphics.Color
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

internal object SampleColors {
    val Canvas = Color(0xFFF6F7F2)
    val Surface = Color(0xFFFFFFFF)
    val DeepInk = Color(0xFF17202A)
    val MutedInk = Color(0xFF56616D)
    val Ocean = Color(0xFF006C67)
    val OnOcean = Color(0xFFFFFFFF)
    val Mint = Color(0xFFCFEDE5)
    val Coral = Color(0xFFC64E42)
    val OnCoral = Color(0xFFFFFFFF)
    val Sun = Color(0xFFFFD57A)
    val SoftBlue = Color(0xFFE6EEF6)
    val Night = Color(0xFF111827)
    val NightSurface = Color(0xFF1F2937)
    val NightVariant = Color(0xFF334155)
    val NightInk = Color(0xFFF8FAFC)
    val NightMuted = Color(0xFFCBD5E1)
}
