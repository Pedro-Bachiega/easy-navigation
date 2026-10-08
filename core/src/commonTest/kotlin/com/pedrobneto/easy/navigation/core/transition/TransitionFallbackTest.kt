package com.pedrobneto.easy.navigation.core.transition

import com.pedrobneto.easy.navigation.core.adaptive.DefaultRegularSceneTransitions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TransitionFallbackTest {
    private val route = object : SceneTransitions {}
    private val global = object : SceneTransitions {}

    @Test
    fun `partial policies omit all callbacks by default`() {
        assertNull(route.transitionSpec)
        assertNull(route.popTransitionSpec)
        assertNull(route.predictivePopTransitionSpec)
    }

    @Test
    fun `a route result stops evaluation before global and builtin`() {
        val evaluated = mutableListOf<SceneTransitions>()
        val result = resolvePolicies(route, global, {
            evaluated += it
            "custom"
        }) { error("Unexpected built-in fallback") }
        assertEquals("custom", result)
        assertEquals(listOf(route), evaluated)
    }

    @Test
    fun `null route result uses the configured global policy`() {
        val evaluated = mutableListOf<SceneTransitions>()
        val result = resolvePolicies(route, global, {
            evaluated += it
            if (it === global) "global" else null
        }) { error("Unexpected built-in fallback") }
        assertEquals("global", result)
        assertEquals(listOf(route, global), evaluated)
    }

    @Test
    fun `null route and global results reach builtin once`() {
        val evaluated = mutableListOf<SceneTransitions>()
        var fallbackCalls = 0
        val result = resolvePolicies(route, global, {
            evaluated += it
            null
        }) { fallbackCalls++; "builtin" }
        assertEquals("builtin", result)
        assertEquals(listOf(route, global), evaluated)
        assertEquals(1, fallbackCalls)
    }

    @Test
    fun `absent route policy starts with global`() {
        val evaluated = mutableListOf<SceneTransitions>()
        assertEquals("global", resolvePolicies(null, global, {
            evaluated += it
            "global"
        }) { error("Unexpected built-in fallback") })
        assertEquals(listOf(global), evaluated)
    }

    @Test
    fun `the same route and global object is not evaluated twice`() {
        var evaluations = 0
        assertEquals("builtin", resolvePolicies(global, global, {
            evaluations++
            null
        }) { "builtin" })
        assertEquals(1, evaluations)
    }

    @Test
    fun `explicit no animation stops fallback`() {
        val none = DefaultRegularSceneTransitions.none()
        val result = resolvePolicies(route, global, { none }) {
            error("Explicit no animation must not fall through")
        }
        assertEquals(none, result)
    }
}
