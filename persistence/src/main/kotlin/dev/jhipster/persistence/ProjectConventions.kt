package dev.jhipster.persistence

import java.io.File

/**
 * Conventions de chemins pour un projet JHipster.
 *
 * Toutes les valeurs sont dérivées de [projectDir] sans configuration explicite.
 * Le nom du projet (et donc du fichier JDL) est le nom du dossier cible.
 *
 * ```
 * edster/                        ← projectDir
 * ├── .yo-rc.json
 * ├── edster.jdl                 ← jdlFile    (projectDir.name + ".jdl")
 * ├── __codebase__/              ← codebaseDir
 * │   ├── build.gradle
 * │   ├── gradle/
 * │   ├── src/
 * │   ├── buildSrc/
 * │   └── .github/
 * └── src/main/docker/           ← dockerDir
 *     ├── postgresql.yml         ← convention DB (dockerDbFile)
 *     └── docker-compose.yml     ← stack complète (dockerComposeFile)
 * ```
 */
class ProjectConventions(val projectDir: File) {

    val projectName: String = projectDir.name

    val codebaseDir: File = projectDir.resolve("__codebase__")

    val jdlFile: File = projectDir.resolve("$projectName.jdl")

    val dockerDir: File = projectDir.resolve("src/main/docker")

    // Fichiers Docker découverts par convention (ordre de priorité)
    val dockerComposeFile: File?
        get() = listOf("docker-compose.yml", "app.yml")
            .map { dockerDir.resolve(it) }
            .firstOrNull { it.exists() }

    val dockerDbFile: File?
        get() = listOf("postgresql.yml", "postgres.yml")
            .map { dockerDir.resolve(it) }
            .firstOrNull { it.exists() }

    fun validate(): List<String> = buildList {
        if (!projectDir.exists())
            add("Le répertoire du projet JHipster n'existe pas : ${projectDir.absolutePath}")
        if (!codebaseDir.exists())
            add("Le répertoire __codebase__ est absent : ${codebaseDir.absolutePath}")
        if (!jdlFile.exists())
            add("Le fichier JDL est absent : ${jdlFile.absolutePath}")
    }
}
