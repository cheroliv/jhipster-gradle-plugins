package dev.jhipster.persistence.tasks

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DockerTaskTest {

    @TempDir
    lateinit var parentDir: File

    private lateinit var projectDir: File
    private lateinit var dockerDir: File

    @BeforeEach
    fun setUp() {
        val workspaceDir = parentDir.resolve("workspace").also { it.mkdirs() }
        workspaceDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "workspace"""")
        workspaceDir.resolve("build.gradle.kts").writeText("""plugins { id("com.cheroliv.jhipster.persistence") }""")

        projectDir = parentDir.resolve("edster").also { it.mkdirs() }
        projectDir.resolve(".yo-rc.json").writeText("""{"generator-jhipster":{"baseName":"edster"}}""")
        dockerDir = projectDir.resolve("src/main/docker").also { it.mkdirs() }
    }

    @Test
    fun `dockerUp task expose le bon mode`() {
        dockerDir.resolve("docker-compose.yml").createNewFile()

        val project = ProjectBuilder.builder()
            .withProjectDir(parentDir.resolve("workspace"))
            .build()
        project.pluginManager.apply("com.cheroliv.jhipster.persistence")
        val task = project.tasks.getByName("dockerUp") as DockerTask

        assertEquals(DockerTask.Mode.UP_ALL, task.mode.get())
    }

    @Test
    fun `dockerDown task expose le bon mode`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(parentDir.resolve("workspace"))
            .build()
        project.pluginManager.apply("com.cheroliv.jhipster.persistence")
        val task = project.tasks.getByName("dockerDown") as DockerTask

        assertEquals(DockerTask.Mode.DOWN, task.mode.get())
    }

    @Test
    fun `dockerDb task expose le bon mode`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(parentDir.resolve("workspace"))
            .build()
        project.pluginManager.apply("com.cheroliv.jhipster.persistence")
        val task = project.tasks.getByName("dockerDb") as DockerTask

        assertEquals(DockerTask.Mode.UP_DB, task.mode.get())
    }

    @Test
    fun `les trois modes de l'énumération Mode sont distincts`() {
        val modes = DockerTask.Mode.entries.toSet()
        assertEquals(3, modes.size)
        assertTrue(DockerTask.Mode.UP_ALL != DockerTask.Mode.UP_DB)
        assertTrue(DockerTask.Mode.UP_ALL != DockerTask.Mode.DOWN)
        assertTrue(DockerTask.Mode.UP_DB != DockerTask.Mode.DOWN)
    }

    @Test
    fun `composeFileFor trouve docker-compose_yml en priorité`() {
        dockerDir.resolve("docker-compose.yml").createNewFile()
        dockerDir.resolve("app.yml").createNewFile()

        val project = ProjectBuilder.builder()
            .withProjectDir(parentDir.resolve("workspace"))
            .build()
        project.pluginManager.apply("com.cheroliv.jhipster.persistence")
        val task = project.tasks.getByName("dockerUp") as DockerTask

        val result = task.composeFileFor(dockerDir)
        assertNotNull(result)
        assertEquals("docker-compose.yml", result!!.name)
    }

    @Test
    fun `composeFileFor trouve app_yml si docker-compose_yml est absent`() {
        dockerDir.resolve("app.yml").createNewFile()

        val project = ProjectBuilder.builder()
            .withProjectDir(parentDir.resolve("workspace"))
            .build()
        project.pluginManager.apply("com.cheroliv.jhipster.persistence")
        val task = project.tasks.getByName("dockerUp") as DockerTask

        val result = task.composeFileFor(dockerDir)
        assertNotNull(result)
        assertEquals("app.yml", result!!.name)
    }

    @Test
    fun `composeFileFor retourne null si aucun fichier n'est trouvé`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(parentDir.resolve("workspace"))
            .build()
        project.pluginManager.apply("com.cheroliv.jhipster.persistence")
        val task = project.tasks.getByName("dockerUp") as DockerTask

        val result = task.composeFileFor(dockerDir)
        assertNull(result)
    }

    @Test
    fun `execute ne plante pas si le répertoire docker est absent`() {
        val emptyDockerDir = parentDir.resolve("nonexistent-docker")

        val project = ProjectBuilder.builder()
            .withProjectDir(parentDir.resolve("workspace"))
            .build()
        project.pluginManager.apply("com.cheroliv.jhipster.persistence")
        val task = project.tasks.getByName("dockerUp") as DockerTask
        task.dockerDir.set(emptyDockerDir)

        task.execute()
    }

    @Test
    fun `up ne plante pas sans fichier compose`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(parentDir.resolve("workspace"))
            .build()
        project.pluginManager.apply("com.cheroliv.jhipster.persistence")
        val task = project.tasks.getByName("dockerUp") as DockerTask

        task.up(dockerDir, null)
    }

    @Test
    fun `dockerDir est résolu par défaut`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(parentDir.resolve("workspace"))
            .build()
        project.pluginManager.apply("com.cheroliv.jhipster.persistence")
        val task = project.tasks.getByName("dockerUp") as DockerTask

        assertNotNull(task.dockerDir.getOrNull())
    }
}
