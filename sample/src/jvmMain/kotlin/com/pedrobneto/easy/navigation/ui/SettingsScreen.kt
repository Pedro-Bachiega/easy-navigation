package com.pedrobneto.easy.navigation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pedrobneto.easy.navigation.core.LocalNavigationController
import com.pedrobneto.easy.navigation.core.adaptive.SinglePane
import com.pedrobneto.easy.navigation.core.annotation.Deeplink
import com.pedrobneto.easy.navigation.core.annotation.Route
import com.pedrobneto.easy.navigation.model.SettingsRoute

@Deeplink("/settings")
@Route(SettingsRoute::class)
@SinglePane
@Composable
fun SettingsScreen() {
    val navigation = LocalNavigationController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navigation.safeNavigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingItem(
                title = "Enable Dark Mode",
                subtitle = "Turn on the dark theme for the app"
            )
            SettingItem(
                title = "Enable Notifications",
                subtitle = "Receive notifications for new items"
            )
        }
    }
}

@Composable
private fun SettingItem(title: String, subtitle: String) {
    var isChecked by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.subtitle1)
            Text(text = subtitle, style = MaterialTheme.typography.caption)
        }
        Checkbox(checked = isChecked, onCheckedChange = { isChecked = it })
    }
}

@Composable
@Preview
private fun SettingsScreenPreview() = MaterialTheme { SettingsScreen() }
