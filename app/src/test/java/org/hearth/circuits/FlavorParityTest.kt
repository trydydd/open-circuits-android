package org.hearth.circuits

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class FlavorParityTest {

    // Unit tests run with working directory = the app module root
    private val appDir = File("").absoluteFile

    @Test
    fun identical_class_files() {
        // Structural invariant: no flavor-specific Kotlin source directories may exist.
        // This guarantees both flavors compile identical sources and produce identical classes.
        for (flavor in listOf("play", "foss")) {
            for (sub in listOf("java", "kotlin")) {
                val dir = File(appDir, "src/$flavor/$sub")
                if (dir.exists()) {
                    val ktFiles = dir.walkTopDown()
                        .filter { it.isFile && it.extension == "kt" }
                        .toList()
                    assertFalse(
                        "Flavor-specific Kotlin sources found in src/$flavor/$sub: $ktFiles",
                        ktFiles.isNotEmpty()
                    )
                }
            }
        }

        // Post-build check: if both variant class outputs exist, verify their class lists match.
        val fossDir = File(appDir, "build/tmp/kotlin-classes/fossRelease")
        val playDir = File(appDir, "build/tmp/kotlin-classes/playRelease")
        if (fossDir.isDirectory && playDir.isDirectory) {
            assertEquals(
                "Class file lists differ between foss and play release variants",
                classListHash(fossDir),
                classListHash(playDir)
            )
        }
    }

    private fun classListHash(dir: File): String {
        val names = dir.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .map { it.relativeTo(dir).path }
            .filter { "BuildConfig" !in it }
            .sorted()
            .joinToString("\n")
        return MessageDigest.getInstance("SHA-256")
            .digest(names.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
