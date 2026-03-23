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
 * Synchronise le code métier depuis `__codebase__/` vers le projet JHipster régénéré.
 *
 * Équivalent de `scripts/sync.sh` :
 * ```sh
 * rm -R build && rm -R .gradle
 * rsync -avr __codebase__/build.gradle .
 * rsync -avr __codebase__/gradle/      ./gradle
 * rsync -avr __codebase__/src/         ./src
 * rsync -avr __codebase__/.github/     ./.github
 * rsync -avr __codebase__/buildSrc/    ./buildSrc
 * ```
 *
 * Comportement :
 *   - Utilise `rsync -avr` si disponible sur le système
 *   - Fallback sur une copie Java native (cross-platform)
 *   - Nettoie `build/` et `.gradle/` avant la synchronisation
 *   - Ignoré silencieusement si un artefact est absent de `__codebase__/`
 */
@DisableCachingByDefault(because = "Synchronisation de fichiers — dépend de l'état externe de __codebase__/")
abstract class SyncCodebaseTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val codebaseDir: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val targetProjectDir: DirectoryProperty

    private data class SyncEntry(
        val sourcePath: String,
        val destPath: String?,
        val isFile: Boolean
    )

    private val syncManifest = listOf(
        SyncEntry("build.gradle", null,      isFile = true),
        SyncEntry("gradle",       "gradle",  isFile = false),
        SyncEntry("src",          "src",     isFile = false),
        SyncEntry(".github",      ".github", isFile = false),
        SyncEntry("buildSrc",     "buildSrc",isFile = false)
    )

    @TaskAction
    fun sync() {
        val source = codebaseDir.get().asFile
        val target = targetProjectDir.get().asFile

        require(source.exists()) {
            "[syncCodebase] Répertoire __codebase__ introuvable : ${source.absolutePath}. " +
                    "Vérifiez que le code métier a bien été initialisé."
        }

        logger.lifecycle("=== syncCodebase : ${source.name} → ${target.name} ===")

        cleanBuildArtifacts(target)

        val useRsync = isRsyncAvailable()
        logger.lifecycle("  Stratégie : ${if (useRsync) "rsync" else "copie Java native"}")

        var synced = 0
        syncManifest.forEach { entry ->
            val srcPath = source.resolve(entry.sourcePath)
            if (!srcPath.exists()) {
                logger.lifecycle("  ⚠ Absent de __codebase__ : ${entry.sourcePath} — ignoré")
                return@forEach
            }
            if (useRsync) rsyncEntry(srcPath, target, entry)
            else          javaCopyEntry(srcPath, target, entry)
            synced++
        }

        logger.lifecycle("✓ syncCodebase : $synced/${syncManifest.size} artefact(s) synchronisé(s)")
    }

    private fun cleanBuildArtifacts(target: File) {
        listOf("build", ".gradle").forEach { name ->
            val dir = target.resolve(name)
            if (dir.exists() && dir.isDirectory) {
                dir.deleteRecursively()
                logger.lifecycle("  → Supprimé : $name/")
            }
        }
    }

    private fun isRsyncAvailable(): Boolean = try {
        ProcessBuilder("rsync", "--version")
            .redirectErrorStream(true)
            .start()
            .waitFor() == 0
    } catch (_: Exception) { false }

    private fun rsyncEntry(src: File, target: File, entry: SyncEntry) {
        val (srcArg, destArg) = if (entry.isFile) {
            src.absolutePath to "${target.absolutePath}/"
        } else {
            "${src.absolutePath}/" to "${target.resolve(entry.destPath!!).absolutePath}/"
        }
        val process = ProcessBuilder("rsync", "-avr", "--delete", srcArg, destArg)
            .directory(target)
            .redirectErrorStream(true)
            .start()
        val output   = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) error("[syncCodebase] rsync a échoué pour '${entry.sourcePath}' (code $exitCode):\n$output")
        logger.lifecycle("  ✓ rsync : ${entry.sourcePath}")
    }

    private fun javaCopyEntry(src: File, target: File, entry: SyncEntry) {
        if (entry.isFile) {
            src.copyTo(target.resolve(src.name), overwrite = true)
        } else {
            val destDir = target.resolve(entry.destPath!!)
            destDir.mkdirs()
            src.walkTopDown().forEach { file ->
                val relative = file.relativeTo(src)
                val dest     = destDir.resolve(relative)
                when {
                    file.isDirectory -> dest.mkdirs()
                    else             -> file.copyTo(dest, overwrite = true)
                }
            }
        }
        logger.lifecycle("  ✓ copié  : ${entry.sourcePath}")
    }
}