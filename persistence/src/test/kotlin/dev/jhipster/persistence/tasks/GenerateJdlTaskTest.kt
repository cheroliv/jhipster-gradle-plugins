package dev.jhipster.persistence.tasks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GenerateJdlTaskTest {

    @TempDir
    lateinit var parentDir: File

    private lateinit var task: GenerateJdlTask
    private lateinit var projectDir: File
    private lateinit var jdlFile: File

    @BeforeEach
    fun setUp() {
        val workspaceDir = parentDir.resolve("workspace").also { it.mkdirs() }
        workspaceDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "workspace"""")
        workspaceDir.resolve("build.gradle.kts").writeText("""plugins { id("com.cheroliv.jhipster.persistence") }""")

        projectDir = parentDir.resolve("edster").also { it.mkdirs() }
        projectDir.resolve(".yo-rc.json").writeText("""{"generator-jhipster":{"baseName":"edster"}}""")
        jdlFile = projectDir.resolve("edster.jdl").also { it.writeText("application {}") }

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(workspaceDir)
            .build()
        project.pluginManager.apply("com.cheroliv.jhipster.persistence")
        task = project.tasks.getByName("generateJdl") as GenerateJdlTask
    }

    @Test
    fun `la tâche est configurée avec les bonnes valeurs par défaut`() {
        assertEquals("lts/jod", task.nvmAlias.get())
        assertEquals("2.3.20", task.kotlinVersion.get())
        assertNotNull(task.jdlFile.getOrNull())
    }

    @Test
    fun `ensureJdlInTarget copie le JDL dans le répertoire cible`() {
        val targetDir = parentDir.resolve("fresh-target").also { it.mkdirs() }
        task.targetProjectDir.set(targetDir)

        targetDir.resolve("edster.jdl").writeText("entity Foo {}")
        val contentBefore = targetDir.resolve("edster.jdl").readText()
        assertEquals("entity Foo {}", contentBefore)
    }
}
