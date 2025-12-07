package com.pedrobneto.easy.navigation.ui.screen

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
import com.pedrobneto.easy.navigation.core.LocalNavigationController
import com.pedrobneto.easy.navigation.core.annotation.Deeplink
import com.pedrobneto.easy.navigation.core.annotation.Route
import com.pedrobneto.easy.navigation.core.annotation.Scope
import com.pedrobneto.easy.navigation.model.FirstScopedRoute
import com.pedrobneto.easy.navigation.model.SecondScopedRoute

@Composable
@Scope("SampleScope")
@Route(FirstScopedRoute::class)
internal fun FirstScopedComposable(modifier: Modifier = Modifier) = Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    val navigation = LocalNavigationController.current

    Spacer(modifier = Modifier.weight(1f))
    Text(text = "First Scoped Composable")
    Spacer(modifier = Modifier.weight(1f))
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            modifier = Modifier.weight(1f),
            onClick = { navigation.navigateTo(SecondScopedRoute) },
            content = { Text("Next using Route") }
        )
        Button(
            modifier = Modifier.weight(1f),
            onClick = { navigation.navigateTo("/second/scoped") },
            content = { Text("Next using Deeplink") }
        )
    }
}

@Composable
@Deeplink("/second/scoped")
@Scope("SampleScope")
@Route(SecondScopedRoute::class)
internal fun SecondScopedComposable(modifier: Modifier = Modifier) = Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    val navigation = LocalNavigationController.current

    Spacer(modifier = Modifier.weight(1f))
    Text(text = "Second Scoped Composable")
    Spacer(modifier = Modifier.weight(1f))
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            modifier = Modifier.weight(1f),
            onClick = { navigation.navigateUp() },
            content = { Text("Back using navigateUp") }
        )
        Button(
            modifier = Modifier.weight(1f),
            onClick = { navigation.popUpTo(FirstScopedRoute) },
            content = { Text("Back using pop") }
        )
    }
}
