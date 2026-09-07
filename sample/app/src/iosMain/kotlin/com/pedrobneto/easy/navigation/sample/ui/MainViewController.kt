package com.pedrobneto.easy.navigation.sample.ui

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    NavigationSample()
}
