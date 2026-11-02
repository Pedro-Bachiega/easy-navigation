package com.pedrobneto.easy.navigation.plugin

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KotlinCompatibilityTest {
    @Test
    fun `accepts stable Kotlin 2 4 releases across patch versions`() {
        listOf("2.4.0", "2.4.10", "2.4.20", "2.4.21", "2.4.100").forEach { version ->
            assertTrue(isSupportedKotlinVersion(version), version)
        }
    }

    @Test
    fun `rejects other release lines and prereleases`() {
        listOf(
            "1.4.20", "2.3.20", "2.5.0", "3.4.0", "2.40.0", "2.4", "2.4.x",
            "2.4.20-RC", "2.4.20-Beta1", "2.4.20-dev-123", "",
        ).forEach { version ->
            assertFalse(isSupportedKotlinVersion(version), version)
        }
    }
}
