package dev.jhipster.persistence

import dev.jhipster.persistence.tasks.CleanCodebaseTask
import dev.jhipster.persistence.tasks.DockerTask
import dev.jhipster.persistence.tasks.GenerateJdlTask
import dev.jhipster.persistence.tasks.SyncCodebaseTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

/**
 * Plugin JHipster Persistence — publié sur le Gradle Plugin Portal.
 *
 * ## Résolution par convention (aucune configuration DSL)
 *
 * Le plugin s'applique au projet **workspace** (le dossier qui contient
 * le projet JHipster comme sous-dossier frère). Il résout automatiquement
 * le projet cible en cherchant dans le dossier parent le premier
 * sous-dossier frère contenant `.yo-rc.json`.
 *
 * Structure attendue :
 * ```
 * workspace/               ← applique le plugin
 * ├── build.gradle.kts
 * └── edster/              ← projet JHipster (détecté via .yo-rc.json)
 *     ├── .yo-rc.json
 *     ├── edster.jdl
 *     ├── __codebase__/
 *     └── src/main/docker/
 * ```
 *
 * ## Tâches — groupe `jhipster-persistence`
 *
 * | Tâche           | Description                                               |
 * |-----------------|-----------------------------------------------------------|
 * | `cleanCodebase` | Supprime les répertoires et fichiers générés par JHipster |
 * | `generateJdl`   | `jhipster jdl --force` via nvm `lts/jod`                 |
 * | `syncCodebase`  | rsync `__codebase__/` → projet JHipster régénéré         |
 * | `regenerate`    | Pipeline : clean → generate → sync                       |
 * | `dockerDb`      | `docker compose up -d` PostgreSQL                        |
 * | `dockerUp`      | `docker compose up -d` stack complète                    |
 * | `dockerDown`    | `docker compose down`                                    |
 * | `dev`           | `dockerDb` + `bootRun` dans le projet cible              |
 */
class JHipsterPersistencePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val targetDir   = resolveJHipsterProject(project)
        val conventions = ProjectConventions(targetDir)

        project.logger.lifecycle(
            "[jhipster-persistence] Projet cible : ${targetDir.absolutePath}"
        )

        registerTasks(project, targetDir, conventions)
    }

    // ── Résolution du projet JHipster ─────────────────────────────────────────

    /**
     * Localise le projet JHipster frère du workspace.
     *
     * Priorité 1 : sous-dossier contenant `.yo-rc.json`
     * Priorité 2 : sous-dossier contenant `build.gradle` ou `build.gradle.kts`
     * Priorité 3 : erreur explicite
     */
    private fun resolveJHipsterProject(project: Project): File {
        val parentDir     = project.projectDir.parentFile
            ?: error(
                "[jhipster-persistence] Aucun dossier parent trouvé. " +
                        "Le workspace doit être dans un dossier contenant le projet JHipster."
            )
        val workspaceName = project.projectDir.name

        return parentDir.listFiles { f ->
            f.isDirectory &&
                    f.name != workspaceName &&
                    f.resolve(".yo-rc.json").exists()
        }?.firstOrNull()
            ?: parentDir.listFiles { f ->
                f.isDirectory &&
                        f.name != workspaceName &&
                        (f.resolve("build.gradle").exists() ||
                                f.resolve("build.gradle.kts").exists())
            }?.firstOrNull()
            ?: error(
                "[jhipster-persistence] Aucun projet JHipster trouvé dans '$parentDir'.\n" +
                        "Le projet cible doit contenir un fichier '.yo-rc.json'."
            )
    }

    // ── Enregistrement des tâches ─────────────────────────────────────────────

    private fun registerTasks(
        project: Project,
        targetDir: File,
        conv: ProjectConventions,
    ) {
        val cleanTask = project.tasks.register(
            "cleanCodebase",
            CleanCodebaseTask::class.java
        ) {
            it.group            = GROUP
            it.description      = "Supprime les répertoires et fichiers générés par JHipster"
            it.targetProjectDir.set(targetDir)
        }

        val generateTask = project.tasks.register(
            "generateJdl",
            GenerateJdlTask::class.java
        ) {
            it.group            = GROUP
            it.description      = "Régénère depuis ${conv.jdlFile.name} via jhipster jdl --force"
            it.targetProjectDir.set(targetDir)
            it.jdlFile.set(conv.jdlFile)
            it.nvmAlias.set(NVM_ALIAS)
            it.kotlinVersion.set(KOTLIN_VERSION)
            // mustRunAfter déclaré ici — dans le contexte de configuration de la tâche
            it.mustRunAfter(cleanTask)
        }

        val syncTask = project.tasks.register(
            "syncCodebase",
            SyncCodebaseTask::class.java
        ) {
            it.group            = GROUP
            it.description      = "Synchronise __codebase__/ → projet JHipster (rsync ou Java natif)"
            it.codebaseDir.set(conv.codebaseDir)
            it.targetProjectDir.set(targetDir)
            // mustRunAfter déclaré ici — dans le contexte de configuration de la tâche
            it.mustRunAfter(generateTask)
        }

        project.tasks.register("dockerUp", DockerTask::class.java) {
            it.group       = GROUP
            it.description = "docker compose up -d (stack complète)"
            it.dockerDir.set(conv.dockerDir)
            it.mode.set(DockerTask.Mode.UP_ALL)
        }

        project.tasks.register("dockerDown", DockerTask::class.java) {
            it.group       = GROUP
            it.description = "docker compose down"
            it.dockerDir.set(conv.dockerDir)
            it.mode.set(DockerTask.Mode.DOWN)
        }

        project.tasks.register("dockerDb", DockerTask::class.java) {
            it.group       = GROUP
            it.description = "docker compose up -d PostgreSQL (convention : postgresql.yml)"
            it.dockerDir.set(conv.dockerDir)
            it.mode.set(DockerTask.Mode.UP_DB)
        }

        // Pipeline : clean → generate → sync
        // L'ordre est garanti par mustRunAfter dans chaque tâche ci-dessus
        project.tasks.register("regenerate") {
            it.group       = GROUP
            it.description = "Pipeline complet : cleanCodebase → generateJdl → syncCodebase"
            it.dependsOn(cleanTask, generateTask, syncTask)
        }

        project.tasks.register("dev") {
            it.group       = GROUP
            it.description = "Mode développement : dockerDb + bootRun dans le projet cible"
            it.dependsOn(project.tasks.named("dockerDb"))
            it.doLast {
                project.logger.lifecycle(
                    "[jhipster-persistence] Lancement de bootRun dans ${targetDir.name}…"
                )
                val exit = ProcessBuilder("bash", "-c", "./gradlew bootRun")
                    .directory(targetDir)
                    .inheritIO()
                    .start()
                    .waitFor()
                if (exit != 0) error("bootRun a échoué (code de sortie : $exit)")
            }
        }
    }

    companion object {
        const val GROUP          = "jhipster-persistence"
        const val NVM_ALIAS      = "lts/jod"
        const val KOTLIN_VERSION = "2.3.20"
        const val JAVA_VERSION   = 24
    }
}