package com.pedrobneto.easy.navigation.sample.android

import android.app.Activity
import android.os.Bundle
import androidx.compose.ui.platform.ComposeView
import com.pedrobneto.easy.navigation.sample.ui.NavigationSample

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ComposeView(this).apply { setContent { NavigationSample() } })
    }
}
