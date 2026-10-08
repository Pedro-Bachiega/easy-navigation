package com.pedrobneto.easy.navigation.core.transition

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class RouteTransitionTest {
    private val resolver = RouteTransitionResolver()
    private val home = TestRoute("home")
    private val details = TestRoute("details")
    private val login = TestRoute("login")

    @Test
    fun `initial and restored presentation do not consult route policies`() {
        val scene = snapshot(listOf(home))
        assertNull(resolver.prepare(scene, scene, TransitionKind.Forward))
        assertEquals(0, home.policyReads)
    }

    @Test
    fun `push consults only the destination policy`() {
        val from = snapshot(listOf(home))
        val change = change(from.stack, listOf(home, details), NavigationOperation.Forward)
        val to = snapshot(change.after, change)
        val resolved = assertNotNull(resolver.prepare(from, to, TransitionKind.Forward))

        assertSame(details.policy, resolved.policy)
        assertEquals(0, home.policyReads)
        assertEquals(1, details.policyReads)
        assertSame(home, resolved.context.from)
        assertSame(details, resolved.context.to)
        assertEquals(NavigationOperation.Forward, resolved.context.operation)
    }

    @Test
    fun `new stack retains the removed source and overrides a framework pop classification`() {
        val from = snapshot(listOf(home, details))
        val change = change(from.stack, listOf(home), NavigationOperation.NewStack)
        val to = snapshot(change.after, change)
        val resolved = assertNotNull(resolver.prepare(from, to, TransitionKind.Pop))

        assertSame(details, resolved.context.from)
        assertSame(home, resolved.context.to)
        assertSame(home.policy, resolved.policy)
        assertEquals(NavigationOperation.NewStack, resolved.context.operation)
        assertEquals(TransitionKind.Forward, resolved.kind)
    }

    @Test
    fun `logout to a different root retains both routes`() {
        val from = snapshot(listOf(home, details))
        val change = change(from.stack, listOf(login), NavigationOperation.NewStack)
        val resolved = assertNotNull(resolver.prepare(from, snapshot(change.after, change), TransitionKind.Forward))

        assertSame(details, resolved.context.from)
        assertSame(login, resolved.context.to)
        assertSame(login.policy, resolved.policy)
    }

    @Test
    fun `multi pop consults only the departing top`() {
        val from = snapshot(listOf(home, login, details))
        val change = change(from.stack, listOf(home), NavigationOperation.Pop)
        val resolved = assertNotNull(resolver.prepare(from, snapshot(change.after, change), TransitionKind.Pop))

        assertSame(details.policy, resolved.policy)
        assertEquals(0, home.policyReads)
        assertSame(details, resolved.context.from)
        assertSame(home, resolved.context.to)
    }

    @Test
    fun `parent replacement remains a pop even when the framework calls forward`() {
        val from = snapshot(listOf(details))
        val change = change(from.stack, listOf(home), NavigationOperation.Pop)
        val resolved = assertNotNull(resolver.prepare(from, snapshot(change.after, change), TransitionKind.Forward))
        assertEquals(TransitionKind.Pop, resolved.kind)
        assertSame(details.policy, resolved.policy)
    }

    @Test
    fun `single top preserves the old route arguments`() {
        val old = TestRoute("details:1")
        val updated = TestRoute("details:2")
        val from = snapshot(listOf(home, old))
        val change = change(from.stack, listOf(home, updated), NavigationOperation.SingleTop)
        val resolved = assertNotNull(resolver.prepare(from, snapshot(change.after, change), TransitionKind.Forward))
        assertSame(old, resolved.context.from)
        assertSame(updated, resolved.context.to)
        assertEquals(NavigationOperation.SingleTop, resolved.context.operation)
    }

    @Test
    fun `layout changes have equal routes and expose original scenes`() {
        val from = snapshot(listOf(home), key = "single")
        val to = snapshot(listOf(home), key = "dual")
        val resolved = assertNotNull(resolver.prepare(from, to, TransitionKind.Forward))

        assertSame(home, resolved.context.from)
        assertSame(home, resolved.context.to)
        assertSame(from.scene, resolved.context.fromScene)
        assertSame(to.scene, resolved.context.toScene)
        assertEquals(NavigationOperation.SceneChange, resolved.context.operation)
        assertEquals(1, home.policyReads)
    }

    @Test
    fun `external edits and coalesced navigation do not reuse unrelated intent`() {
        val from = snapshot(listOf(home))
        val latest = change(listOf(home, details), listOf(login), NavigationOperation.NewStack)
        assertEquals(NavigationOperation.Unknown, transitionContext(from, snapshot(latest.after, latest)).operation)

        val stale = change(listOf(home), listOf(login), NavigationOperation.NewStack)
        val source = snapshot(stale.before, stale)
        assertEquals(NavigationOperation.Unknown, transitionContext(source, snapshot(stale.after, stale)).operation)
    }

    @Test
    fun `predictive cancellation retains source policy context and edge`() {
        val from = snapshot(listOf(home, details))
        val projected = snapshot(listOf(home))
        val seeking = assertNotNull(resolver.prepare(from, projected, TransitionKind.PredictivePop, NavigationEvent.EDGE_LEFT))
        val cancelling = assertNotNull(resolver.prepare(from, projected, TransitionKind.Forward))

        assertSame(seeking.context, cancelling.context)
        assertEquals(NavigationOperation.PredictivePop, cancelling.context.operation)
        assertEquals(TransitionKind.PredictivePop, cancelling.kind)
        assertEquals(NavigationEvent.EDGE_LEFT, cancelling.swipeEdge)
        assertSame(details.policy, cancelling.policy)

        assertNull(resolver.prepare(from, from, TransitionKind.Forward))
        val pop = change(from.stack, projected.stack, NavigationOperation.Pop)
        val next = assertNotNull(resolver.prepare(from, snapshot(pop.after, pop), TransitionKind.Pop))
        assertEquals(NavigationOperation.Pop, next.context.operation)
    }

    @Test
    fun `predictive completion retains its policy after stack mutation`() {
        val from = snapshot(listOf(home, details))
        val projected = snapshot(listOf(home))
        val seeking = assertNotNull(resolver.prepare(from, projected, TransitionKind.PredictivePop, NavigationEvent.EDGE_RIGHT))
        val pop = change(from.stack, projected.stack, NavigationOperation.Pop)
        val completing = assertNotNull(resolver.prepare(from, snapshot(pop.after, pop), TransitionKind.Pop))
        assertSame(seeking.context, completing.context)
        assertEquals(TransitionKind.PredictivePop, completing.kind)
        assertEquals(NavigationEvent.EDGE_RIGHT, completing.swipeEdge)
    }

    @Test
    fun `interrupted predictive pair does not contaminate a new destination`() {
        val from = snapshot(listOf(home, details))
        resolver.prepare(from, snapshot(listOf(home)), TransitionKind.PredictivePop, NavigationEvent.EDGE_LEFT)
        val change = change(from.stack, listOf(login), NavigationOperation.NewStack)
        val resolved = assertNotNull(resolver.prepare(from, snapshot(change.after, change), TransitionKind.Forward))
        assertEquals(NavigationOperation.NewStack, resolved.context.operation)
        assertEquals(TransitionKind.Forward, resolved.kind)
        assertSame(login.policy, resolved.policy)
    }
}

internal class TestRoute(val name: String) : NavigationRoute {
    val policy = object : SceneTransitions {}
    var policyReads = 0
    override fun transitions(): SceneTransitions = policy.also { policyReads++ }
}

internal data class TestScene(
    override val key: Any,
    override val entries: List<NavEntry<NavigationRoute>>,
    override val previousEntries: List<NavEntry<NavigationRoute>> = emptyList(),
) : Scene<NavigationRoute> {
    override val content: @Composable () -> Unit = {}
}

internal fun snapshot(
    routes: List<NavigationRoute>,
    change: NavigationTransitionChange? = null,
    key: Any = routes.last(),
): RouteScene = RouteScene(TestScene(key, routes.map { NavEntry(it) {} }), routes, change)

private fun change(
    before: List<NavigationRoute>,
    after: List<NavigationRoute>,
    operation: NavigationOperation,
): NavigationTransitionChange = NavigationTransitionChange(before, after, operation)
