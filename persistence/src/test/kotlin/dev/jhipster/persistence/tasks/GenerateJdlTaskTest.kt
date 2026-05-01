package dev.jhipster.persistence.tasks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
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

        task.ensureJdlInTarget(jdlFile, targetDir)
        val copiedJdl = targetDir.resolve("edster.jdl")
        assertTrue(copiedJdl.exists(), "Le JDL doit être copié dans le répertoire cible")
        assertEquals("application {}", copiedJdl.readText())
    }

    @Test
    fun `ensureJdlInTarget ne copie pas si le JDL est déjà à destination`() {
        val targetDir = parentDir.resolve("fresh-target").also { it.mkdirs() }
        val alreadyExistingJdl = targetDir.resolve("edster.jdl").also { it.writeText("entity Existing {}") }

        task.ensureJdlInTarget(alreadyExistingJdl, targetDir)
        assertEquals("entity Existing {}", alreadyExistingJdl.readText())
    }

    @Test
    fun `validateInputs lève une erreur si le fichier JDL est absent`() {
        val fakeJdl = parentDir.resolve("missing.jdl")

        val ex = assertThrows(IllegalArgumentException::class.java) {
            task.validateInputs(projectDir, fakeJdl)
        }
        assertTrue("JDL" in ex.message!!)
    }

    @Test
    fun `validateInputs crée le répertoire cible s'il n'existe pas`() {
        val freshDir = parentDir.resolve("nonexistent-target")
        assertTrue(!freshDir.exists())

        task.validateInputs(freshDir, jdlFile)
        assertTrue(freshDir.exists(), "validateInputs doit créer le répertoire projet")
    }

    @Test
    fun `gitignoreEntries contient les entrées attendues`() {
        val entries = task.gitignoreEntries
        assertTrue(".goose" in entries)
        assertTrue("README.pdf" in entries)
        assertTrue("README.html" in entries)
        assertTrue("README.docx" in entries)
        assertTrue("README.epub" in entries)
        assertTrue("README.fr.pdf" in entries)
        assertTrue("README.fr.html" in entries)
        assertTrue("README.fr.docx" in entries)
        assertTrue("README.fr.epub" in entries)
        assertEquals(9, entries.size)
    }

    @Test
    fun `applyGitignoreEntries ajoute les nouvelles entrées`() {
        val gitignore = projectDir.resolve(".gitignore").also {
            it.writeText("build/\n.gradle/\n")
        }

        task.applyGitignoreEntries(projectDir)

        val content = gitignore.readText()
        assertTrue(".goose" in content, ".goose doit être ajouté")
        assertTrue("README.pdf" in content, "README.pdf doit être ajouté")
        assertTrue("jhipster-persistence-plugin" in content, "Commentaire de section doit être présent")
    }

    @Test
    fun `applyGitignoreEntries n'ajoute pas de doublons`() {
        val gitignore = projectDir.resolve(".gitignore").also {
            it.writeText("build/\n.gradle/\nREADME.pdf\nREADME.html\n")
        }

        task.applyGitignoreEntries(projectDir)

        val content = gitignore.readText()
        val countPdf = content.lines().count { it == "README.pdf" }
        assertEquals(1, countPdf, "README.pdf ne doit apparaître qu'une seule fois")
        assertEquals(1, content.lines().count { it == "README.html" })
    }

    @Test
    fun `applyGitignoreEntries ne plante pas sans gitignore`() {
        task.applyGitignoreEntries(projectDir)
    }

    @Test
    fun `applyGitignoreEntries ne modifie pas si tout est déjà présent`() {
        val originalContent = buildString {
            appendLine("build/")
            appendLine(".gradle/")
            appendLine(".goose")
            appendLine("README.pdf")
            appendLine("README.html")
            appendLine("README.docx")
            appendLine("README.epub")
            appendLine("README.fr.pdf")
            appendLine("README.fr.html")
            appendLine("README.fr.docx")
            appendLine("README.fr.epub")
        }
        val gitignore = projectDir.resolve(".gitignore").also {
            it.writeText(originalContent)
        }

        val lastModifiedBefore = gitignore.lastModified()
        task.applyGitignoreEntries(projectDir)
        val lastModifiedAfter = gitignore.lastModified()

        assertEquals(lastModifiedBefore, lastModifiedAfter, "Le fichier ne doit pas être modifié")
    }

    @Test
    fun `patchLibsVersionsToml met à jour la version Kotlin`() {
        val gradleDir = projectDir.resolve("gradle").also { it.mkdirs() }
        val toml = gradleDir.resolve("libs.versions.toml").also {
            it.writeText("""
                [versions]
                kotlin = "2.2.10"
                spring-boot = "4.0.0"
            """.trimIndent())
        }

        task.patchLibsVersionsToml(projectDir, "2.3.20")

        val updated = toml.readText()
        assertTrue("kotlin = \"2.3.20\"" in updated)
        assertTrue("spring-boot" in updated)
    }

    @Test
    fun `patchLibsVersionsToml ne plante pas si le TOML est absent`() {
        task.patchLibsVersionsToml(projectDir, "2.3.20")
    }

    @Test
    fun `patchBuildGradle injecte jvmToolchain et compilerArgs`() {
        projectDir.resolve("build.gradle").writeText("plugins { id(\"kotlin\") }")

        task.patchBuildGradle(projectDir)

        val updated = projectDir.resolve("build.gradle").readText()
        assertTrue("jvmToolchain(24)" in updated)
        assertTrue("-Xjsr305=strict" in updated)
        assertTrue("kotlin.compilerOptions" in updated)
    }

    @Test
    fun `patchBuildGradle n'ajoute pas jvmToolchain si déjà présent`() {
        projectDir.resolve("build.gradle").writeText("""
            plugins { id("kotlin") }
            // jhipster-persistence: jvmToolchain
            kotlin.jvmToolchain(24)
        """.trimIndent())

        val before = projectDir.resolve("build.gradle").readText()
        task.patchBuildGradle(projectDir)
        val after = projectDir.resolve("build.gradle").readText()

        assertEquals(before, after)
    }

    @Test
    fun `patchBuildGradle ne plante pas si build_gradle est absent`() {
        task.patchBuildGradle(projectDir)
    }

    @Test
    fun `patchBuildSrc met à jour la version du plugin Kotlin`() {
        projectDir.resolve("buildSrc").mkdirs()
        projectDir.resolve("buildSrc/build.gradle").writeText("""
            dependencies { implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10") }
        """.trimIndent())

        task.patchBuildSrc(projectDir, "2.3.20")

        val updated = projectDir.resolve("buildSrc/build.gradle").readText()
        assertTrue("kotlin-gradle-plugin:2.3.20" in updated)
        assertTrue("2.2.10" !in updated)
    }

    @Test
    fun `patchBuildSrc ne plante pas si buildSrc est absent`() {
        task.patchBuildSrc(projectDir, "2.3.20")
    }

    @Test
    fun `configureKotlin orchestre les trois patchs`() {
        val gradleDir = projectDir.resolve("gradle").also { it.mkdirs() }
        gradleDir.resolve("libs.versions.toml").writeText("[versions]\nkotlin = \"2.2.10\"\n")
        projectDir.resolve("build.gradle").writeText("plugins { id(\"kotlin\") }")
        projectDir.resolve("buildSrc").mkdirs()
        projectDir.resolve("buildSrc/build.gradle").writeText(
            "dependencies { implementation(\"org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10\") }"
        )

        task.configureKotlin(projectDir, "2.3.20")

        assertTrue("kotlin = \"2.3.20\"" in gradleDir.resolve("libs.versions.toml").readText())
        assertTrue("jvmToolchain(24)" in projectDir.resolve("build.gradle").readText())
        assertTrue("kotlin-gradle-plugin:2.3.20" in projectDir.resolve("buildSrc/build.gradle").readText())
    }
}
