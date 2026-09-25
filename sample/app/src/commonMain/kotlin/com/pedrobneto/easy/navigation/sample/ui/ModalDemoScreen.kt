package com.pedrobneto.easy.navigation.sample.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pedrobneto.easy.navigation.core.LocalNavigationController
import com.pedrobneto.easy.navigation.core.annotation.Route
import com.pedrobneto.easy.navigation.core.modal.Modal
import com.pedrobneto.easy.navigation.core.transition.ModalTransitions
import com.pedrobneto.easy.navigation.sample.model.ModalDemoRoute
import com.pedrobneto.easy.navigation.sample.model.SpringModalDemoRoute

@Modal
@Route(ModalDemoRoute::class)
@Composable
internal fun ModalDemoScreen() {
    val navigation = LocalNavigationController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = navigation::navigateUp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(24.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Modal route", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "The previous scene stays underneath this composable. " +
                            "The library supplies the transparent overlay layer."
                )
                Button(onClick = navigation::navigateUp) {
                    Text("Close explicitly")
                }
            }
        }
    }
}

@Modal(transitions = SpringModalTransitions::class)
@Route(SpringModalDemoRoute::class)
@Composable
internal fun SpringModalDemoScreen() {
    val navigation = LocalNavigationController.current
    val cardVisibility = remember { MutableTransitionState(false) }

    LaunchedEffect(Unit) {
        cardVisibility.targetState = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = navigation::navigateUp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visibleState = cardVisibility,
            enter = scaleIn(
                initialScale = .7f,
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            ),
            exit = scaleOut(
                targetScale = .7f,
                animationSpec = tween(150)
            )
        ) {
            Card(
                modifier = Modifier.padding(24.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Spring modal", style = MaterialTheme.typography.headlineSmall)
                    Text("This modal grows into place with a spring animation.")
                    Button(onClick = navigation::navigateUp) {
                        Text("Close explicitly")
                    }
                }
            }
        }
    }
}

internal class SpringModalTransitions : ModalTransitions {
    override val contentEnter: EnterTransition = fadeIn(animationSpec = tween(180))
    override val contentExit: ExitTransition = fadeOut(animationSpec = tween(150))

    override val scrimEnter: EnterTransition = fadeIn(animationSpec = tween(180))
    override val scrimExit: ExitTransition = fadeOut(animationSpec = tween(150))
}
