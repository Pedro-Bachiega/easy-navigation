package com.pedrobneto.navigation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pedrobneto.navigation.core.LocalNavigationController
import com.pedrobneto.navigation.core.annotation.NavigationEntry
import com.pedrobneto.navigation.model.FirstScreenRoute
import com.pedrobneto.navigation.model.SecondScreenRoute

@Composable
@NavigationEntry(route = FirstScreenRoute::class, deeplinks = ["/first"])
internal fun FirstScreenComposable(modifier: Modifier = Modifier) = Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    val navigation = LocalNavigationController.current

    Spacer(modifier = Modifier.weight(1f))
    Text(text = "First Screen")
    Spacer(modifier = Modifier.weight(1f))
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            modifier = Modifier.weight(1f),
            onClick = { navigation.navigateTo(SecondScreenRoute(title = "Second Screen title")) },
            content = { Text("Next using Route") }
        )
        Button(
            modifier = Modifier.weight(1f),
            onClick = { navigation.navigateTo("/third?title=Third screen title&description=Third screen description") },
            content = { Text("Third screen using Deeplink") }
        )
        Button(
            modifier = Modifier.weight(1f),
            onClick = { navigation.navigateTo("/fourth/Some cool value?title=Fourth screen title&description=Fourth screen description") },
            content = { Text("Fourth screen using Deeplink") }
        )
    }
}
