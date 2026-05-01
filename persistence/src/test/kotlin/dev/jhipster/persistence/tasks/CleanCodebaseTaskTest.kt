package dev.jhipster.persistence.tasks

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CleanCodebaseTaskTest {

    @TempDir
    lateinit var parentDir: File

    private lateinit var task: CleanCodebaseTask
    private lateinit var projectDir: File

    @BeforeEach
    fun setUp() {
        val workspaceDir = parentDir.resolve("workspace").also { it.mkdirs() }
        workspaceDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "workspace"""")
        workspaceDir.resolve("build.gradle.kts").writeText("""plugins { id("com.cheroliv.jhipster.persistence") }""")

        projectDir = parentDir.resolve("edster").also { it.mkdirs() }
        projectDir.resolve(".yo-rc.json").writeText("""{"generator-jhipster":{"baseName":"edster"}}""")

        val project = ProjectBuilder.builder()
            .withProjectDir(workspaceDir)
            .build()
        project.pluginManager.apply("com.cheroliv.jhipster.persistence")
        task = project.tasks.getByName("cleanCodebase") as CleanCodebaseTask
    }

    @Test
    fun `delete les répertoires générés par JHipster`() {
        val generatedDirs = listOf(
            ".gradle", "build", ".devcontainer", ".github", ".goose",
            ".husky", ".jhipster", "buildSrc", "gradle", "node_modules",
            "src", "webpack", ".vscode"
        )
        generatedDirs.forEach { projectDir.resolve(it).mkdirs() }

        projectDir.resolve("__codebase__").mkdirs()
        projectDir.resolve("__codebase__/Service.kt").writeText("// code métier")

        task.clean()

        generatedDirs.forEach { dir ->
            assertFalse(
                projectDir.resolve(dir).exists(),
                "$dir/ doit être supprimé"
            )
        }
        assertTrue(projectDir.resolve("__codebase__").exists(), "__codebase__/ doit survivre")
        assertTrue(
            projectDir.resolve("__codebase__/Service.kt").exists(),
            "Le contenu de __codebase__/ doit survivre"
        )
    }

    @Test
    fun `delete les fichiers générés par JHipster`() {
        val generatedFiles = listOf(
            ".editorconfig", ".gitignore", ".gitattributes",
            ".lintstagedrc.cjs", ".prettierignore", ".prettierrc",
            ".yo-rc.json", "build.gradle", "checkstyle.xml",
            "cypress.config.ts", "cypress-audits.config.ts",
            "eslint.config.mjs", "gradle.properties", "gradlew",
            "gradlew.bat", "jest.conf.js", "pnpmw", "pnpmw.cmd",
            "npmw", "npmw.cmd", "package.json", "package-lock.json",
            "pnpm-lock.yaml", "postcss.config.js", "README.md",
            "settings.gradle", "sonar-project.properties",
            "tsconfig.json", "tsconfig.test.json", "local.properties"
        )
        generatedFiles.forEach { projectDir.resolve(it).createNewFile() }

        task.clean()

        generatedFiles.forEach { file ->
            assertFalse(
                projectDir.resolve(file).exists(),
                "$file doit être supprimé"
            )
        }
    }

    @Test
    fun `ne lève pas d'erreur sur projet vide`() {
        task.clean()
    }
}
