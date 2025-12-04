package com.pedrobneto.easy.navigation.processor.library

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import org.junit.Before
import java.io.OutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(KspExperimental::class)
class RegistryProcessTest {

    private val codeGenerator = mockk<CodeGenerator>(relaxed = true)
    private val outputStream = mockk<OutputStream>(relaxed = true)

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
        val fileContentSlot = slot<ByteArray>()
        every {
            codeGenerator.createNewFile(
                any(),
                any(),
                capture(fileNameSlot),
                any()
            )
        } returns outputStream
        every { outputStream.write(capture(fileContentSlot)) } returns Unit

        // WHEN
        codeGenerator.createModuleRegistryFile(
            packageName = "com.sample.registry",
            fileName = "SampleRegistry",
            sources = listOf(ksFile),
            directions = directions
        )

        // THEN
        assertEquals("SampleRegistry", fileNameSlot.captured)

        val expectedContent = """
            package com.sample.registry

            import com.pedrobneto.easy.navigation.core.model.DirectionRegistry
            import com.sample.Direction1
            import com.sample.Direction2

            data object SampleRegistry : DirectionRegistry(
                directions = listOf(
                    Direction1,
                    Direction2
                )
            )
        """.trimIndent()
        assertEquals(
            expectedContent.normalizeLineEndings(),
            String(fileContentSlot.captured).normalizeLineEndings()
        )
    }

    @Test
    fun `GIVEN directions and scope WHEN creating module registry THEN it should generate a file with scope`() {
        // GIVEN
        val ksFile = mockk<KSFile>()
        val directions = listOf("com.sample.Direction1")
        val fileNameSlot = slot<String>()
        val fileContentSlot = slot<ByteArray>()
        every {
            codeGenerator.createNewFile(
                any(),
                any(),
                capture(fileNameSlot),
                any()
            )
        } returns outputStream
        every { outputStream.write(capture(fileContentSlot)) } returns Unit

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

        val expectedContent = """
            package com.sample.registry

            import com.pedrobneto.easy.navigation.core.annotation.Scope
            import com.pedrobneto.easy.navigation.core.model.DirectionRegistry
            import com.sample.Direction1

            @Scope("myScope")
            data object ScopedRegistry : DirectionRegistry(
                directions = listOf(
                    Direction1
                )
            )
        """.trimIndent()
        assertEquals(
            expectedContent.normalizeLineEndings(),
            String(fileContentSlot.captured).normalizeLineEndings()
        )
    }

    private fun String.normalizeLineEndings(): String =
        this.replace("\r\n", "\n").replace("\t", "    ")
}
