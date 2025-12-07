package com.pedrobneto.easy.navigation.model

import androidx.compose.ui.unit.DpSize

data class ScreenInfo(
    val dpSize: DpSize = DpSize.Zero,
    val windowSize: WindowSize = WindowSize.Small,
    val orientation: Orientation = Orientation.Landscape
) {
    val isValid = dpSize != DpSize.Zero
}

enum class Orientation { Landscape, Portrait }

enum class WindowSize {
    Small, Medium, Large;

    fun isAtLeast(other: WindowSize) = this.ordinal >= other.ordinal
}
