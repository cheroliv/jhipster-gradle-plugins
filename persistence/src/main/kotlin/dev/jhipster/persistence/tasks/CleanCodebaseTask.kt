package dev.jhipster.persistence.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Supprime les répertoires et fichiers générés par JHipster.
 *
 * La base de données est gérée par Docker — aucune opération psql.
 * Équivalent de la partie fichiers de `scripts/clean.sh`.
 */
@DisableCachingByDefault(because = "Tâche de nettoyage — opère sur l'état du système de fichiers externe")
abstract class CleanCodebaseTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val targetProjectDir: DirectoryProperty

    private val generatedDirs = listOf(
        ".gradle", "build", ".devcontainer", ".github", ".goose",
        ".husky", ".jhipster", "buildSrc", "gradle", "node_modules",
        "src", "webpack", ".vscode"
    )

    private val generatedFiles = listOf(
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

    @TaskAction
    fun clean() {
        val projectDir = targetProjectDir.get().asFile

        logger.lifecycle("=== cleanCodebase → ${projectDir.name} ===")

        var deletedCount = 0
        deletedCount += deleteEntries(projectDir, generatedDirs,  isDir = true)
        deletedCount += deleteEntries(projectDir, generatedFiles, isDir = false)

        logger.lifecycle("✓ cleanCodebase : $deletedCount entrée(s) supprimée(s)")
    }

    private fun deleteEntries(
        projectDir: File,
        names: List<String>,
        isDir: Boolean
    ): Int {
        var count = 0
        names.forEach { name ->
            val entry = projectDir.resolve(name)
            if (!entry.exists()) return@forEach
            if (isDir  && !entry.isDirectory) return@forEach
            if (!isDir && !entry.isFile)      return@forEach

            val deleted = if (isDir) entry.deleteRecursively() else entry.delete()
            if (deleted) {
                logger.lifecycle("  ✓ Supprimé : $name${if (isDir) "/" else ""}")
                count++
            } else {
                logger.warn("  ⚠ Impossible de supprimer : $name")
            }
        }
        return count
    }
}