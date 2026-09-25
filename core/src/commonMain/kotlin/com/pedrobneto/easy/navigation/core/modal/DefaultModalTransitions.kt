package com.pedrobneto.easy.navigation.core.modal

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.pedrobneto.easy.navigation.core.transition.ModalTransitions

class DefaultModalTransitions : ModalTransitions {
    override val contentEnter: EnterTransition = fadeIn()
    override val contentExit: ExitTransition = fadeOut()
    override val scrimEnter: EnterTransition = fadeIn()
    override val scrimExit: ExitTransition = fadeOut()
}
