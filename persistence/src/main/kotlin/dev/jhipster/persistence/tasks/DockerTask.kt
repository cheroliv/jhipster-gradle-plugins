package dev.jhipster.persistence.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Gère les services Docker Compose du projet JHipster.
 *
 * | Mode    | Fichiers cherchés (par priorité)                              |
 * |---------|---------------------------------------------------------------|
 * | UP_ALL  | `docker-compose.yml`, `app.yml`                               |
 * | UP_DB   | `postgresql.yml`, `postgres.yml` → fallback service UP_ALL    |
 * | DOWN    | même fichier que UP_ALL                                        |
 *
 * Détecte automatiquement Docker Compose v2 (`docker compose`)
 * ou v1 (`docker-compose`).
 */
@DisableCachingByDefault(because = "Tâche Docker — opère sur des services externes non cachables")
abstract class DockerTask : DefaultTask() {

    enum class Mode {
        UP_ALL,
        UP_DB,
        DOWN
    }

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val dockerDir: DirectoryProperty

    @get:Input
    abstract val mode: Property<Mode>

    @TaskAction
    fun execute() {
        val dir = dockerDir.get().asFile

        if (!dir.exists()) {
            logger.warn(
                "[dockerTask] Répertoire Docker introuvable : ${dir.absolutePath}\n" +
                        "Lancez d'abord 'generateJdl' pour créer la structure du projet."
            )
            return
        }

        when (mode.get()) {
            Mode.UP_ALL -> up(dir, composeFileFor(dir))
            Mode.UP_DB  -> upDb(dir)
            Mode.DOWN   -> down(dir, composeFileFor(dir))
        }
    }

    private fun up(dir: File, composeFile: File?) {
        logger.lifecycle("=== dockerUp : stack complète ===")
        if (composeFile == null) {
            logger.warn("  ⚠ Aucun fichier docker-compose trouvé dans ${dir.absolutePath}")
            return
        }
        runCompose(dir, listOf("-f", composeFile.name, "up", "-d"))
        logger.lifecycle("✓ Stack démarrée depuis ${composeFile.name}")
    }

    private fun upDb(dir: File) {
        logger.lifecycle("=== dockerDb : PostgreSQL ===")

        val dbFile = listOf("postgresql.yml", "postgres.yml")
            .map { dir.resolve(it) }
            .firstOrNull { it.exists() }

        if (dbFile != null) {
            runCompose(dir, listOf("-f", dbFile.name, "up", "-d"))
            logger.lifecycle("✓ PostgreSQL démarré via ${dbFile.name}")
            return
        }

        val mainCompose = composeFileFor(dir)
        if (mainCompose != null) {
            logger.lifecycle("  → postgresql.yml non trouvé — service 'postgresql' depuis ${mainCompose.name}")
            runCompose(dir, listOf("-f", mainCompose.name, "up", "-d", "postgresql"))
            logger.lifecycle("✓ PostgreSQL démarré")
            return
        }

        logger.warn(
            "  ⚠ Aucun fichier Docker pour PostgreSQL trouvé dans ${dir.absolutePath}\n" +
                    "  Fichiers attendus : postgresql.yml ou docker-compose.yml"
        )
    }

    private fun down(dir: File, composeFile: File?) {
        logger.lifecycle("=== dockerDown ===")
        val args = if (composeFile != null) listOf("-f", composeFile.name, "down")
        else listOf("down")
        runCompose(dir, args)
        logger.lifecycle("✓ Stack arrêtée")
    }

    private fun composeFileFor(dir: File): File? =
        listOf("docker-compose.yml", "app.yml")
            .map { dir.resolve(it) }
            .firstOrNull { it.exists() }

    private fun runCompose(workDir: File, args: List<String>) {
        val command = buildList {
            if (isComposeV2()) addAll(listOf("docker", "compose"))
            else               add("docker-compose")
            addAll(args)
        }
        logger.lifecycle("  → ${command.joinToString(" ")}")
        val exit = ProcessBuilder(command)
            .directory(workDir)
            .inheritIO()
            .start()
            .waitFor()
        if (exit != 0) error("[dockerTask] docker compose a échoué (code $exit)")
    }

    private fun isComposeV2(): Boolean = try {
        ProcessBuilder("docker", "compose", "version")
            .redirectErrorStream(true)
            .start()
            .waitFor() == 0
    } catch (_: Exception) { false }
}