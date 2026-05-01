package dev.jhipster.persistence.tasks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SyncCodebaseTaskTest {

    @TempDir
    lateinit var parentDir: File

    private lateinit var task: SyncCodebaseTask
    private lateinit var projectDir: File
    private lateinit var codebaseDir: File

    @BeforeEach
    fun setUp() {
        val workspaceDir = parentDir.resolve("workspace").also { it.mkdirs() }
        workspaceDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "workspace"""")
        workspaceDir.resolve("build.gradle.kts").writeText("""plugins { id("com.cheroliv.jhipster.persistence") }""")

        projectDir = parentDir.resolve("edster").also { it.mkdirs() }
        projectDir.resolve(".yo-rc.json").writeText("""{"generator-jhipster":{"baseName":"edster"}}""")
        codebaseDir = projectDir.resolve("__codebase__").also { it.mkdirs() }

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(workspaceDir)
            .build()
        project.pluginManager.apply("com.cheroliv.jhipster.persistence")
        task = project.tasks.getByName("syncCodebase") as SyncCodebaseTask
    }

    @Test
    fun `sync les fichiers build_gradle depuis __codebase__`() {
        codebaseDir.resolve("build.gradle").writeText("// code métier custom")
        projectDir.resolve("build.gradle").writeText("// généré par JHipster")

        task.sync()

        val synced = projectDir.resolve("build.gradle")
        assertTrue(synced.exists(), "build.gradle doit exister après le sync")
        assertEquals("// code métier custom", synced.readText())
    }

    @Test
    fun `sync les répertoires codebase vers le projet`() {
        codebaseDir.resolve("src/main/kotlin").mkdirs()
        codebaseDir.resolve("src/main/kotlin/Service.kt").writeText("// business code")
        projectDir.resolve("src/main/kotlin").mkdirs()
        projectDir.resolve("src/main/kotlin/Generated.kt").writeText("// generated")

        task.sync()

        assertTrue(projectDir.resolve("src/main/kotlin/Service.kt").exists(), "Service.kt doit être présent")
    }

    @Test
    fun `nettoie build et dot_gradle avant le sync`() {
        codebaseDir.resolve("src").mkdirs()
        codebaseDir.resolve("src/Example.kt").writeText("// example")
        projectDir.resolve("build").mkdirs()
        projectDir.resolve("build/some-artifact.txt").createNewFile()
        projectDir.resolve(".gradle").mkdirs()
        projectDir.resolve(".gradle/version").createNewFile()

        task.sync()

        assertFalse(projectDir.resolve("build").exists(), "build/ doit être nettoyé")
        assertFalse(projectDir.resolve(".gradle").exists(), ".gradle/ doit être nettoyé")
    }

    @Test
    fun `ignore les entrées absentes de __codebase__`() {
        task.sync()
    }

}
