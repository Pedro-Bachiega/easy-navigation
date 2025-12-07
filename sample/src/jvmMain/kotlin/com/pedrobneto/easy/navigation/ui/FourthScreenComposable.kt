package com.pedrobneto.easy.navigation.ui

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
import com.pedrobneto.easy.navigation.core.adaptive.ExtraPane
import com.pedrobneto.easy.navigation.core.annotation.Deeplink
import com.pedrobneto.easy.navigation.core.annotation.ParentDeeplink
import com.pedrobneto.easy.navigation.core.annotation.Route
import com.pedrobneto.easy.navigation.core.model.LaunchStrategy
import com.pedrobneto.easy.navigation.model.FourthScreenRoute
import com.pedrobneto.easy.navigation.model.ThirdScreenRoute

@Composable
@Deeplink("/fourth/{pathVariable}")
@ExtraPane(host = ThirdScreenRoute::class, ratio = 0.6f)
@Route(FourthScreenRoute::class)
@ParentDeeplink("/third?title=Used navigateUp with @ParentDeeplink&description=Used navigateUp with @ParentDeeplink")
internal fun FourthScreenComposable(
    modifier: Modifier = Modifier,
    fourthScreenRoute: FourthScreenRoute
) = Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    val navigation = LocalNavigationController.current

    Spacer(modifier = Modifier.weight(1f))
    Text(text = "Title: ${fourthScreenRoute.title}")
    Text(text = "Description: ${fourthScreenRoute.description}")
    Text(text = "Path Variable: ${fourthScreenRoute.pathVariable}")
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
            onClick = {
                navigation.navigateTo(deeplink = "/first", strategy = LaunchStrategy.NewStack)
            },
            content = { Text("Back using Deeplink") }
        )
    }
}
