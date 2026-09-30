package com.pedrobneto.easy.navigation.sample.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pedrobneto.easy.navigation.core.annotation.Route
import com.pedrobneto.easy.navigation.core.modal.LocalModalScope
import com.pedrobneto.easy.navigation.core.modal.Modal
import com.pedrobneto.easy.navigation.sample.model.ModalDemoRoute
import com.pedrobneto.easy.navigation.sample.model.SpringModalDemoRoute
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Modal
@Route(ModalDemoRoute::class)
@Composable
internal fun ModalDemoScreen() {
    val modal = LocalModalScope.current
    val scope = rememberCoroutineScope()
    val scrimVisibility = remember { MutableTransitionState(false) }
    val contentVisibility = remember { MutableTransitionState(false) }
    var closing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scrimVisibility.targetState = true
        contentVisibility.targetState = true
    }

    fun dismiss() {
        if (closing) return

        closing = true
        scope.launch {
            scrimVisibility.targetState = false
            contentVisibility.targetState = false

            snapshotFlow {
                scrimVisibility.isIdle && !scrimVisibility.currentState &&
                        contentVisibility.isIdle && !contentVisibility.currentState
            }.first()

            modal.navigateUp()
        }
    }

    SideEffect {
        modal.setSystemBackRequestHandler(::dismiss)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visibleState = scrimVisibility,
            enter = fadeIn(animationSpec = tween(durationMillis = 180)),
            exit = fadeOut(animationSpec = tween(durationMillis = 180)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = ::dismiss),
            )
        }

        AnimatedVisibility(
            visibleState = contentVisibility,
            enter = fadeIn(animationSpec = tween(durationMillis = 180)),
            exit = fadeOut(animationSpec = tween(durationMillis = 180)),
        ) {
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Modal route", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "The previous scene stays underneath this composable. " +
                                "This component owns its scrim and dismissal behavior."
                    )
                    Button(onClick = ::dismiss) {
                        Text("Close explicitly")
                    }
                }
            }
        }
    }
}

@Modal
@Route(SpringModalDemoRoute::class)
@Composable
internal fun SpringModalDemoScreen() {
    val modal = LocalModalScope.current
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val containerHeight = with(density) { maxHeight.toPx() }
        val scrimAlpha = remember { Animatable(0f) }
        val contentOffset = remember(containerHeight) { Animatable(containerHeight) }
        val contentScale = remember { Animatable(0.82f) }

        LaunchedEffect(containerHeight) {
            coroutineScope {
                launch { scrimAlpha.animateTo(0.48f, tween(durationMillis = 220)) }
                launch {
                    contentOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = 320,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                }
                launch {
                    contentScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                    )
                }
            }
        }

        fun dismiss() {
            if (closing) return

            closing = true
            scope.launch {
                coroutineScope {
                    launch {
                        scrimAlpha.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 180),
                        )
                    }
                    launch {
                        contentOffset.animateTo(
                            targetValue = containerHeight,
                            animationSpec = tween(
                                durationMillis = 280,
                                easing = FastOutSlowInEasing,
                            ),
                        )
                    }
                    launch {
                        contentScale.animateTo(
                            targetValue = 0.82f,
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                        )
                    }
                }
                modal.navigateUp()
            }
        }

        SideEffect {
            modal.setSystemBackRequestHandler(::dismiss)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha.value))
                .clickable(onClick = ::dismiss),
        )

        Card(
            modifier = Modifier
                .padding(24.dp)
                .offset {
                    IntOffset(x = 0, y = contentOffset.value.roundToInt())
                }
                .graphicsLayer {
                    scaleX = contentScale.value
                    scaleY = contentScale.value
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Confirmar compra", style = MaterialTheme.typography.headlineSmall)
                Text("Deseja continuar com esta compra?")
                Button(onClick = ::dismiss, enabled = !closing) {
                    Text("Fechar")
                }
            }
        }
    }
}
