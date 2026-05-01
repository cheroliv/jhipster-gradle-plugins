package dev.jhipster.persistence

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProjectConventionsTest {

    @Test
    fun `résout le jdlFile à partir du nom du répertoire`(@TempDir tempDir: File) {
        val expectedJdl = "${tempDir.name}.jdl"
        tempDir.resolve(expectedJdl).createNewFile()
        val conventions = ProjectConventions(tempDir)

        assertEquals(expectedJdl, conventions.jdlFile.name)
        assertEquals(tempDir.name, conventions.projectName)
    }

    @Test
    fun `résout codebaseDir et dockerDir`(@TempDir tempDir: File) {
        tempDir.resolve("__codebase__").mkdirs()
        tempDir.resolve("src/main/docker").mkdirs()
        val conventions = ProjectConventions(tempDir)

        assertEquals("__codebase__", conventions.codebaseDir.name)
        assertTrue(conventions.dockerDir.isDirectory)
    }

    @Test
    fun `validate détecte les artefacts manquants`(@TempDir tempDir: File) {
        tempDir.resolve("__codebase__").mkdirs()
        val conventions = ProjectConventions(tempDir)

        val errors = conventions.validate()
        assertTrue(errors.isNotEmpty(), "Doit détecter le JDL manquant")
        assertTrue(errors.any { "JDL" in it }, "Doit mentionner le JDL manquant")
    }

    @Test
    fun `validate OK quand tout est présent`(@TempDir tempDir: File) {
        tempDir.resolve("__codebase__").mkdirs()
        tempDir.resolve("${tempDir.name}.jdl").createNewFile()
        val conventions = ProjectConventions(tempDir)

        val errors = conventions.validate()
        assertFalse(errors.isNotEmpty())
    }

    @Test
    fun `dockerComposeFile null quand absent`(@TempDir tempDir: File) {
        tempDir.resolve("src/main/docker").mkdirs()
        val conventions = ProjectConventions(tempDir)

        assertEquals(null, conventions.dockerComposeFile)
    }

    @Test
    fun `dockerComposeFile trouvé quand présent`(@TempDir tempDir: File) {
        tempDir.resolve("src/main/docker").mkdirs()
        tempDir.resolve("src/main/docker/docker-compose.yml").createNewFile()
        val conventions = ProjectConventions(tempDir)

        assertEquals("docker-compose.yml", conventions.dockerComposeFile!!.name)
    }

    @Test
    fun `dockerDbFile priorité postgresql_yml`(@TempDir tempDir: File) {
        tempDir.resolve("src/main/docker").mkdirs()
        tempDir.resolve("src/main/docker/postgresql.yml").createNewFile()
        val conventions = ProjectConventions(tempDir)

        assertEquals("postgresql.yml", conventions.dockerDbFile!!.name)
    }
}
