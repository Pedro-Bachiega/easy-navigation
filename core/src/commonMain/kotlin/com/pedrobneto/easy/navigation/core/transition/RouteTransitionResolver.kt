package com.pedrobneto.easy.navigation.core.transition

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent
import com.pedrobneto.easy.navigation.core.adaptive.DefaultRegularSceneTransitions
import com.pedrobneto.easy.navigation.core.model.NavigationRoute

internal enum class TransitionKind { Forward, Pop, PredictivePop }

/** One resolver per display. Only the active predictive pair is retained, never a scene history. */
internal class RouteTransitionResolver {
    private var predictive: PredictiveTransition? = null

    fun resolve(
        scope: AnimatedContentTransitionScope<Scene<NavigationRoute>>,
        global: SceneTransitions,
        requestedKind: TransitionKind,
        @NavigationEvent.SwipeEdge swipeEdge: Int = NavigationEvent.EDGE_NONE,
    ): ContentTransform {
        val from = scope.initialState as RouteScene
        val to = scope.targetState as RouteScene
        val resolved = prepare(from, to, requestedKind, swipeEdge)
            ?: return DefaultRegularSceneTransitions.none()
        return resolveTransition(
            scope, resolved.context, resolved.kind, resolved.swipeEdge, resolved.policy, global,
        )
    }

    internal fun prepare(
        from: RouteScene,
        to: RouteScene,
        requestedKind: TransitionKind,
        @NavigationEvent.SwipeEdge swipeEdge: Int = NavigationEvent.EDGE_NONE,
    ): ResolvedRouteTransition? {
        if (from == to) {
            predictive = null
            return null
        }

        val activePredictive = predictive?.takeIf { it.matches(from, to) }
        val context = when {
            requestedKind == TransitionKind.PredictivePop -> {
                transitionContext(from, to, predictive = true).also {
                    predictive = PredictiveTransition(from, to, it, swipeEdge)
                }
            }
            activePredictive != null -> activePredictive.context
            else -> {
                predictive = null
                transitionContext(from, to)
            }
        }
        val kind = transitionKind(context.operation, requestedKind)
        val edge = if (requestedKind == TransitionKind.PredictivePop) {
            swipeEdge
        } else {
            activePredictive?.swipeEdge ?: swipeEdge
        }
        val route = if (kind == TransitionKind.Forward) context.to else context.from
        return ResolvedRouteTransition(context, kind, edge, route.transitions())
    }
}

internal class ResolvedRouteTransition(
    val context: RouteTransitionContext,
    val kind: TransitionKind,
    @NavigationEvent.SwipeEdge val swipeEdge: Int,
    val policy: SceneTransitions?,
)

internal fun transitionContext(
    from: RouteScene,
    to: RouteScene,
    predictive: Boolean = false,
): RouteTransitionContext {
    val change = to.change
    val operation = when {
        predictive -> NavigationOperation.PredictivePop
        from.stack == to.stack -> NavigationOperation.SceneChange
        change != null && change !== from.change &&
            change.before == from.stack && change.after == to.stack -> change.operation
        else -> NavigationOperation.Unknown
    }
    return RouteTransitionContext(from.route, to.route, from.scene, to.scene, operation)
}

internal fun transitionKind(
    operation: NavigationOperation,
    requestedKind: TransitionKind,
): TransitionKind = when (operation) {
    NavigationOperation.PredictivePop -> TransitionKind.PredictivePop
    NavigationOperation.Pop -> TransitionKind.Pop
    NavigationOperation.Forward, NavigationOperation.SingleTop, NavigationOperation.NewStack ->
        TransitionKind.Forward
    NavigationOperation.SceneChange, NavigationOperation.Unknown -> requestedKind
}

internal fun resolveTransition(
    scope: AnimatedContentTransitionScope<Scene<NavigationRoute>>,
    context: RouteTransitionContext,
    kind: TransitionKind,
    @NavigationEvent.SwipeEdge swipeEdge: Int,
    route: SceneTransitions?,
    global: SceneTransitions,
): ContentTransform {
    fun SceneTransitions.evaluate(): ContentTransform? = when (kind) {
        TransitionKind.Forward -> transitionSpec?.invoke(scope, context)
        TransitionKind.Pop -> popTransitionSpec?.invoke(scope, context)
        TransitionKind.PredictivePop -> predictivePopTransitionSpec?.invoke(scope, context, swipeEdge)
    }

    // Each policy is evaluated at most once, even when the global policy is the built-in object
    // or a route explicitly returns the global policy. Predictive pop never inherits regular pop.
    return resolvePolicies(route, global, { it.evaluate() }) {
        when (kind) {
            TransitionKind.Forward -> DefaultRegularSceneTransitions.run {
                context.fromScene transitionTo context.toScene
            }
            TransitionKind.Pop -> DefaultRegularSceneTransitions.run {
                context.fromScene popTo context.toScene
            }
            TransitionKind.PredictivePop -> DefaultRegularSceneTransitions.predictivePop(scope, swipeEdge)
        }
    }
}

internal fun <T : Any> resolvePolicies(
    route: SceneTransitions?,
    global: SceneTransitions,
    evaluate: (SceneTransitions) -> T?,
    builtin: () -> T,
): T = route?.let(evaluate)
    ?: (if (global !== route) evaluate(global) else null)
    ?: builtin()

private class PredictiveTransition(
    private val from: RouteScene,
    private val to: RouteScene,
    val context: RouteTransitionContext,
    val swipeEdge: Int,
) {
    fun matches(source: RouteScene, target: RouteScene): Boolean =
        from.key == source.key && to.key == target.key &&
            from.stack == source.stack && to.stack == target.stack
}
