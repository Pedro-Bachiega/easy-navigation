package com.pedrobneto.easy.navigation.processor.application

import com.google.devtools.ksp.processing.CodeGenerator
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.io.OutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class RegistryFileTest {

    private val codeGenerator = mockk<CodeGenerator>(relaxed = true)
    private val outputStream = mockk<OutputStream>(relaxed = true)

    @Test
    fun `GIVEN registries WHEN creating global registry THEN it should generate a file with the registries`() {
        // GIVEN
        val registries = listOf("com.sample.Registry1", "com.sample.Registry2")
        val fileNameSlot = slot<String>()
        val fileContentSlot = slot<ByteArray>()
        every { codeGenerator.createNewFile(any(), any(), capture(fileNameSlot), any()) } returns outputStream
        every { outputStream.write(capture(fileContentSlot)) } returns Unit

        // WHEN
        codeGenerator.createGlobalRegistryFile(
            registries = registries,
            directions = emptyList()
        )

        // THEN
        assertEquals("GlobalDirectionRegistry", fileNameSlot.captured)

        val expectedContent = """
            package com.pedrobneto.easy.navigation.registry

            import com.pedrobneto.easy.navigation.core.model.DirectionRegistry
            import com.sample.Registry1
            import com.sample.Registry2

            data object GlobalDirectionRegistry : DirectionRegistry(
                directions = listOf(
                    Registry1,
                    Registry2
                ).flatMap(DirectionRegistry::directions)
            )
        """.trimIndent()
        assertEquals(expectedContent.normalizeLineEndings(), String(fileContentSlot.captured).normalizeLineEndings())
    }

    @Test
    fun `GIVEN directions WHEN creating global registry THEN it should generate a file with the directions`() {
        // GIVEN
        val directions = listOf("com.sample.Direction1", "com.sample.Direction2")
        val fileNameSlot = slot<String>()
        val fileContentSlot = slot<ByteArray>()
        every { codeGenerator.createNewFile(any(), any(), capture(fileNameSlot), any()) } returns outputStream
        every { outputStream.write(capture(fileContentSlot)) } returns Unit

        // WHEN
        codeGenerator.createGlobalRegistryFile(
            registries = emptyList(),
            directions = directions
        )

        // THEN
        assertEquals("GlobalDirectionRegistry", fileNameSlot.captured)

        val expectedContent = """
            package com.pedrobneto.easy.navigation.registry

            import com.pedrobneto.easy.navigation.core.model.DirectionRegistry
            import com.sample.Direction1
            import com.sample.Direction2

            data object GlobalDirectionRegistry : DirectionRegistry(
                directions = listOf(
                    Direction1,
                    Direction2
                )
            )
        """.trimIndent()
        assertEquals(expectedContent.normalizeLineEndings(), String(fileContentSlot.captured).normalizeLineEndings())
    }

    @Test
    fun `GIVEN registries and directions WHEN creating global registry THEN it should generate a file with both`() {
        // GIVEN
        val registries = listOf("com.sample.Registry1", "com.sample.Registry2")
        val directions = listOf("com.sample.Direction1", "com.sample.Direction2")
        val fileNameSlot = slot<String>()
        val fileContentSlot = slot<ByteArray>()
        every { codeGenerator.createNewFile(any(), any(), capture(fileNameSlot), any()) } returns outputStream
        every { outputStream.write(capture(fileContentSlot)) } returns Unit

        // WHEN
        codeGenerator.createGlobalRegistryFile(
            registries = registries,
            directions = directions
        )

        // THEN
        assertEquals("GlobalDirectionRegistry", fileNameSlot.captured)

        val expectedContent = """
            package com.pedrobneto.easy.navigation.registry

            import com.pedrobneto.easy.navigation.core.model.DirectionRegistry
            import com.sample.Direction1
            import com.sample.Direction2
            import com.sample.Registry1
            import com.sample.Registry2

            data object GlobalDirectionRegistry : DirectionRegistry(
                directions = listOf(
                    Registry1,
                    Registry2
                ).flatMap(DirectionRegistry::directions) + listOf(
                    Direction1,
                    Direction2
                )
            )
        """.trimIndent()
        assertEquals(expectedContent.normalizeLineEndings(), String(fileContentSlot.captured).normalizeLineEndings())
    }

    @Test
    fun `GIVEN no registries and no directions WHEN creating global registry THEN it should generate an empty file`() {
        // GIVEN
        val registries = emptyList<String>()
        val directions = emptyList<String>()
        val fileNameSlot = slot<String>()
        val fileContentSlot = slot<ByteArray>()
        every { codeGenerator.createNewFile(any(), any(), capture(fileNameSlot), any()) } returns outputStream
        every { outputStream.write(capture(fileContentSlot)) } returns Unit

        // WHEN
        codeGenerator.createGlobalRegistryFile(
            registries = registries,
            directions = directions
        )

        // THEN
        assertEquals("GlobalDirectionRegistry", fileNameSlot.captured)

        val expectedContent = """
            package com.pedrobneto.easy.navigation.registry

            import com.pedrobneto.easy.navigation.core.model.DirectionRegistry

            data object GlobalDirectionRegistry : DirectionRegistry(
                directions = emptyList()
            )
        """.trimIndent()
        assertEquals(expectedContent.normalizeLineEndings(), String(fileContentSlot.captured).normalizeLineEndings())
    }

    private fun String.normalizeLineEndings(): String = this.replace("\r\n", "\n").replace("\t", "    ")
}
