package com.pedrobneto.easy.navigation.processor.application

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class FileExtensionTest {

    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `GIVEN files WHEN filtering valid files THEN it should return only valid files`() {
        // GIVEN
        val kspDir = temporaryFolder.newFolder("build", "generated", "ksp", "jvm", "jvmMain")
        val validFile = File(kspDir, "MyDirectionRegistry.kt").apply { createNewFile() }
        File(kspDir, "NotADirectionRegistry.txt").apply { createNewFile() }
        File(kspDir, "GlobalDirectionRegistry.kt").apply { createNewFile() }

        val files = sequenceOf(validFile, File("invalid"))

        // WHEN
        val result = files.filterValidFiles().toList()

        // THEN
        assertEquals(1, result.size)
        assertEquals(validFile, result.first())
    }

    @Test
    fun `GIVEN files WHEN filtering by source set THEN it should return only files from that source set and commonMain`() {
        // GIVEN
        val jvmDir =
            temporaryFolder.newFolder("build", "generated", "ksp", "jvm", "jvmMain")
        val androidDir =
            temporaryFolder.newFolder("build", "generated", "ksp", "android", "androidDebug")
        val commonDir =
            temporaryFolder.newFolder("build", "generated", "ksp", "metadata", "commonMain")
        val jvmFile = File(jvmDir, "JvmDirectionRegistry.kt").apply { createNewFile() }
        val androidFile = File(androidDir, "AndroidDirectionRegistry.kt").apply { createNewFile() }
        val commonFile = File(commonDir, "CommonDirectionRegistry.kt").apply { createNewFile() }

        val files = sequenceOf(androidFile, jvmFile, commonFile)

        // WHEN
        val result = files.filteredBySourceSet("jvmMain").toList()

        // THEN
        assertEquals(2, result.size)
        assertContains(result, commonFile, "common file missing")
        assertContains(result, jvmFile, "jvm file missing")
        assertFalse { result.contains(androidFile) }
    }

    @Test
    fun `GIVEN all files in commonMin WHEN filtering by source set THEN it should return empty`() {
        // GIVEN
        val commonDir =
            temporaryFolder.newFolder("build", "generated", "ksp", "metadata", "commonMain")
        val commonFile1 = File(commonDir, "CommonDirectionRegistry.kt").apply { createNewFile() }
        val commonFile2 = File(commonDir, "AnotherDirectionRegistry.kt").apply { createNewFile() }

        val files = sequenceOf(commonFile1, commonFile2)

        // WHEN
        val result = files.filteredBySourceSet("jvmMain").toList()

        // THEN
        assertEquals(0, result.size)
    }

    @Test
    fun `GIVEN files WHEN extracting registries THEN it should return the correct registry names`() {
        // GIVEN
        val file1 = temporaryFolder.newFile("1DirectionRegistry.kt").apply {
            writeText(
                """
                package com.sample

                data object MyDirectionRegistry : DirectionRegistry(
                    // ...
                )
            """.trimIndent()
            )
        }
        val file2 = temporaryFolder.newFile("2DirectionRegistry.kt").apply {
            writeText(
                """
                package com.sample.other

                data object OtherDirectionRegistry : DirectionRegistry(
                    // ...
                )
            """.trimIndent()
            )
        }

        val files = listOf(file1, file2)

        // WHEN
        val result = files.extractRegistries()

        // THEN
        assertEquals(2, result.size)
        assertEquals("com.sample.MyDirectionRegistry", result[0])
        assertEquals("com.sample.other.OtherDirectionRegistry", result[1])
    }

    @Test
    fun `GIVEN files WHEN extracting directions THEN it should return the correct direction names`() {
        // GIVEN
        val file1 = temporaryFolder.newFile("1Direction.kt").apply {
            writeText(
                """
                package com.sample

                @GlobalScope
                data object MyDirection : NavigationDirection(
                    // ...
                )
            """.trimIndent()
            )
        }
        val file2 = temporaryFolder.newFile("2Direction.kt").apply {
            writeText(
                """
                package com.sample.other

                @GlobalScope
                data object OtherDirection : NavigationDirection(
                    // ...
                )
            """.trimIndent()
            )
        }
        val file3 = temporaryFolder.newFile("3Direction.kt").apply {
            writeText(
                """
                package com.sample.other

                data object NonGlobalDirection : NavigationDirection(
                    // ...
                )
            """.trimIndent()
            )
        }

        val files = listOf(file1, file2, file3)

        // WHEN
        val result = files.extractDirections()

        // THEN
        assertEquals(2, result.size)
        assertEquals("com.sample.MyDirection", result[0])
        assertEquals("com.sample.other.OtherDirection", result[1])
    }
}
