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
import com.pedrobneto.easy.navigation.core.adaptive.AdaptivePane
import com.pedrobneto.easy.navigation.core.annotation.Deeplink
import com.pedrobneto.easy.navigation.core.annotation.ParentRoute
import com.pedrobneto.easy.navigation.core.annotation.Route
import com.pedrobneto.easy.navigation.model.FirstScreenRoute
import com.pedrobneto.easy.navigation.model.SecondScreenRoute

@AdaptivePane(0.5f)
@Composable
@Deeplink("/second")
@Route(SecondScreenRoute::class)
@ParentRoute(FirstScreenRoute::class)
internal fun SecondScreenComposable(
    modifier: Modifier = Modifier,
    secondScreenRoute: SecondScreenRoute
) = Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    val navigation = LocalNavigationController.current

    Spacer(modifier = Modifier.weight(1f))
    Text(text = secondScreenRoute.title)
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
            onClick = { navigation.navigateTo("/third?title=Third screen using deeplink&description=Some cool description") },
            content = { Text("Next using Deeplink") }
        )
    }
}
