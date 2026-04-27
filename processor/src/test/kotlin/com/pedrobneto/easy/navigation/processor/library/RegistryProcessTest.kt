package com.pedrobneto.easy.navigation.processor.library

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import org.junit.Before
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(KspExperimental::class)
class RegistryProcessTest {

    private val codeGenerator = mockk<CodeGenerator>(relaxed = true)
    @Before
    fun setup() {
        mockkStatic("com.google.devtools.ksp.UtilsKt")
    }

    @Test
    fun `GIVEN directions WHEN creating module registry THEN it should generate a file`() {
        // GIVEN
        val ksFile = mockk<KSFile>()
        val directions = listOf("com.sample.Direction1", "com.sample.Direction2")
        val fileNameSlot = slot<String>()
        val outputStream = ByteArrayOutputStream()
        every {
            codeGenerator.createNewFile(
                any(),
                any(),
                capture(fileNameSlot),
                any()
            )
        } returns outputStream

        // WHEN
        codeGenerator.createModuleRegistryFile(
            packageName = "com.sample.registry",
            fileName = "SampleRegistry",
            sources = listOf(ksFile),
            directions = directions
        )

        // THEN
        assertEquals("SampleRegistry", fileNameSlot.captured)

        val content = outputStream.toString().normalizeLineEndings()
        assertContains(content, "data object SampleRegistry : DirectionRegistry(")
        assertContains(content, "directions = listOf(")
        assertContains(content, "Direction1")
        assertContains(content, "Direction2")
    }

    @Test
    fun `GIVEN directions and scope WHEN creating module registry THEN it should generate a file with scope`() {
        // GIVEN
        val ksFile = mockk<KSFile>()
        val directions = listOf("com.sample.Direction1")
        val fileNameSlot = slot<String>()
        val outputStream = ByteArrayOutputStream()
        every {
            codeGenerator.createNewFile(
                any(),
                any(),
                capture(fileNameSlot),
                any()
            )
        } returns outputStream

        // WHEN
        codeGenerator.createModuleRegistryFile(
            scope = "myScope",
            packageName = "com.sample.registry",
            fileName = "ScopedRegistry",
            sources = listOf(ksFile),
            directions = directions
        )

        // THEN
        assertEquals("ScopedRegistry", fileNameSlot.captured)

        val content = outputStream.toString().normalizeLineEndings()
        assertContains(content, "@Scope(\"myScope\")")
        assertContains(content, "data object ScopedRegistry : DirectionRegistry(")
        assertContains(content, "Direction1")
    }

    private fun String.normalizeLineEndings(): String =
        this.replace("\r\n", "\n").replace("\t", "    ")

    private fun assertContains(actual: String, expected: String) {
        kotlin.test.assertTrue(
            actual.contains(expected),
            "Expected generated content to contain:\n$expected\n\nActual:\n$actual"
        )
    }
}
