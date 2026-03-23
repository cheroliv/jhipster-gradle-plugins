package dev.jhipster.persistence

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests fonctionnels du plugin via GradleTestKit.
 *
 * ## Structure reproduite
 *
 * La structure sur le filesystem réel est :
 * ```
 * parentDir/           ← dossier parent quelconque
 * ├── workspace/       ← projet Gradle qui applique le plugin  (= pluginDir dans les tests)
 * └── edster/          ← projet JHipster frère  (détecté via .yo-rc.json)
 * ```
 *
 * GradleTestKit reçoit `pluginDir` comme `projectDir`.
 * `edster/` est créé comme frère de `pluginDir` dans `parentDir`.
 */
class JHipsterPersistencePluginTest {

    @TempDir
    lateinit var parentDir: File   // simule le dossier parent commun

    private lateinit var pluginDir: File   // workspace qui applique le plugin
    private lateinit var edsterDir: File   // projet JHipster frère

    @BeforeEach
    fun setUp() {
        // workspace/ — projet Gradle qui applique le plugin
        pluginDir = parentDir.resolve("workspace").also { it.mkdirs() }
        pluginDir.resolve("settings.gradle.kts")
            .writeText("""rootProject.name = "workspace"""")
        pluginDir.resolve("build.gradle.kts")
            .writeText("""plugins { id("com.cheroliv.jhipster.persistence") }""")

        // edster/ — projet JHipster frère (même niveau que workspace/)
        edsterDir = parentDir.resolve("edster").also { it.mkdirs() }
        edsterDir.resolve(".yo-rc.json")
            .writeText("""{"generator-jhipster":{"baseName":"edster"}}""")
        edsterDir.resolve("edster.jdl")
            .writeText("entity Workspace { name String required }")
        edsterDir.resolve("build.gradle")
            .writeText("// généré par JHipster")
        edsterDir.resolve("__codebase__").mkdirs()
        edsterDir.resolve("src/main/docker").mkdirs()
    }

    @Suppress("NonAsciiCharacters")
    @Test
    fun `le plugin expose les tâches du groupe jhipster-persistence`() {
        val result = GradleRunner.create()
            .withProjectDir(pluginDir)
            .withPluginClasspath()
            .withArguments("tasks", "--group=jhipster-persistence")
            .build()

        listOf(
            "cleanCodebase",
            "generateJdl",
            "syncCodebase",
            "regenerate",
            "dockerDb",
            "dockerUp",
            "dockerDown",
            "dev"
        ).forEach { task ->
            assertTrue(
                task in result.output,
                "Tâche '$task' absente du groupe jhipster-persistence\nOutput:\n${result.output}"
            )
        }
    }

    @Test
    fun `le plugin résout le projet JHipster par convention`() {
        val result = GradleRunner.create()
            .withProjectDir(pluginDir)
            .withPluginClasspath()
            .withArguments("tasks")
            .build()

        assertTrue(
            "jhipster-persistence" in result.output,
            "Le groupe jhipster-persistence doit apparaître dans tasks\nOutput:\n${result.output}"
        )
    }

    @Test
    fun `la tâche cleanCodebase supprime les fichiers générés par JHipster`() {
        // Crée les artefacts générés que cleanCodebase doit supprimer
        edsterDir.resolve("build").mkdirs()
        edsterDir.resolve("node_modules").mkdirs()
        edsterDir.resolve("src").mkdirs()
        edsterDir.resolve("package.json").createNewFile()
        edsterDir.resolve("gradlew").createNewFile()

        // __codebase__/ doit survivre au clean
        val codebaseFile = edsterDir.resolve("__codebase__/Service.kt")
        codebaseFile.writeText("// code métier")

        val result = GradleRunner.create()
            .withProjectDir(pluginDir)
            .withPluginClasspath()
            .withArguments("cleanCodebase")
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":cleanCodebase")?.outcome)

        // Artefacts JHipster supprimés
        assertTrue(!edsterDir.resolve("build").exists(),        "build/ doit être supprimé")
        assertTrue(!edsterDir.resolve("node_modules").exists(), "node_modules/ doit être supprimé")
        assertTrue(!edsterDir.resolve("src").exists(),          "src/ doit être supprimé")
        assertTrue(!edsterDir.resolve("package.json").exists(), "package.json doit être supprimé")
        assertTrue(!edsterDir.resolve("gradlew").exists(),      "gradlew doit être supprimé")

        // __codebase__/ intact
        assertTrue(edsterDir.resolve("__codebase__").exists(),  "__codebase__/ doit rester intact")
        assertTrue(codebaseFile.exists(),                       "Le code métier dans __codebase__/ doit survivre")
    }
}