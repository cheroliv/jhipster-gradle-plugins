package dev.jhipster.persistence.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Lance `jhipster jdl <fichier.jdl> --force` depuis le répertoire cible.
 *
 * Flux d'exécution :
 *   1. Validation des prérequis (JDL présent, répertoire cible accessible)
 *   2. Activation de nvm + sélection de la version Node ([nvmAlias])
 *   3. Exécution de `jhipster jdl --force`
 *   4. Patch `.gitignore` (entrées spécifiques au projet)
 *   5. Configuration Kotlin [kotlinVersion] dans le projet généré
 *
 * Prérequis système :
 *   - nvm installé dans `~/.nvm`
 *   - jhipster installé globalement (`npm install -g generator-jhipster`)
 */
@DisableCachingByDefault(because = "Génération JHipster — résultat non déterministe, dépend de l'état du système")
abstract class GenerateJdlTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val targetProjectDir: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val jdlFile: RegularFileProperty

    @get:Input
    abstract val nvmAlias: Property<String>

    @get:Input
    abstract val kotlinVersion: Property<String>

    @TaskAction
    fun generate() {
        val projectDir = targetProjectDir.get().asFile
        val jdl        = jdlFile.get().asFile
        val nodeAlias  = nvmAlias.getOrElse("lts/jod")
        val kotlin     = kotlinVersion.getOrElse("2.3.20")

        logger.lifecycle("=== generateJdl : ${jdl.name} → ${projectDir.name} ===")
        logger.lifecycle("  Node : $nodeAlias | Kotlin cible : $kotlin")

        validateInputs(projectDir, jdl)
        ensureJdlInTarget(jdl, projectDir)
        runJHipster(projectDir, jdl.name, nodeAlias)
        applyGitignoreEntries(projectDir)
        configureKotlin(projectDir, kotlin)

        logger.lifecycle("✓ generateJdl terminé")
    }

    private fun validateInputs(projectDir: File, jdl: File) {
        require(jdl.exists()) { "Fichier JDL introuvable : ${jdl.absolutePath}" }
        projectDir.mkdirs()
    }

    private fun ensureJdlInTarget(jdl: File, projectDir: File) {
        val localJdl = projectDir.resolve(jdl.name)
        if (localJdl.canonicalPath != jdl.canonicalPath) {
            jdl.copyTo(localJdl, overwrite = true)
            logger.lifecycle("  → JDL copié dans ${projectDir.name}/${jdl.name}")
        }
    }

    private fun runJHipster(projectDir: File, jdlName: String, nodeAlias: String) {
        logger.lifecycle("  → jhipster jdl $jdlName --force  (Node $nodeAlias)")
        val script = """
            export NVM_DIR="${'$'}HOME/.nvm"
            [ -s "${'$'}NVM_DIR/nvm.sh" ] && \. "${'$'}NVM_DIR/nvm.sh"
            nvm use $nodeAlias || { echo "ERREUR : nvm alias '$nodeAlias' introuvable"; exit 1; }
            jhipster jdl $jdlName --force
        """.trimIndent()
        runBash(script, projectDir, "JHipster JDL generation")
        logger.lifecycle("  ✓ Génération JHipster terminée")
    }

    private val gitignoreEntries = listOf(
        ".goose", "README.pdf", "README.html", "README.docx", "README.epub",
        "README.fr.pdf", "README.fr.html", "README.fr.docx", "README.fr.epub"
    )

    private fun applyGitignoreEntries(projectDir: File) {
        val gitignore = projectDir.resolve(".gitignore")
        if (!gitignore.exists()) {
            logger.warn("  ⚠ .gitignore introuvable — patch ignoré")
            return
        }
        val existing = gitignore.readLines().map { it.trim() }.toSet()
        val toAppend = gitignoreEntries.filter { it !in existing }
        if (toAppend.isEmpty()) {
            logger.lifecycle("  ✓ .gitignore déjà à jour")
            return
        }
        gitignore.appendText(buildString {
            appendLine()
            appendLine("# jhipster-persistence-plugin")
            toAppend.forEach { appendLine(it) }
        })
        logger.lifecycle("  ✓ .gitignore mis à jour (+${toAppend.size} entrées)")
    }

    private fun configureKotlin(projectDir: File, kotlinVersion: String) {
        logger.lifecycle("  → Configuration Kotlin $kotlinVersion")
        patchLibsVersionsToml(projectDir, kotlinVersion)
        patchBuildGradle(projectDir)
        patchBuildSrc(projectDir, kotlinVersion)
        logger.lifecycle("  ✓ Kotlin $kotlinVersion configuré")
    }

    private fun patchLibsVersionsToml(projectDir: File, kotlinVersion: String) {
        val toml = projectDir.resolve("gradle/libs.versions.toml")
        if (!toml.exists()) return
        val original = toml.readText()
        val updated  = original.replace(
            Regex("""(kotlin\s*=\s*)"[^"]+""""),
            """${'$'}1"$kotlinVersion""""
        )
        if (updated != original) {
            toml.writeText(updated)
            logger.lifecycle("    ✓ libs.versions.toml : kotlin → $kotlinVersion")
        }
    }

    private fun patchBuildGradle(projectDir: File) {
        val buildGradle = projectDir.resolve("build.gradle")
        if (!buildGradle.exists()) return
        val marker  = "// jhipster-persistence: jvmToolchain"
        val content = buildGradle.readText()
        if (marker in content) return
        buildGradle.appendText("""

$marker
kotlin.jvmToolchain(24)
kotlin.compilerOptions.freeCompilerArgs.addAll("-Xjsr305=strict")
""")
        logger.lifecycle("    ✓ build.gradle : jvmToolchain(24) injecté")
    }

    private fun patchBuildSrc(projectDir: File, kotlinVersion: String) {
        val buildSrc = projectDir.resolve("buildSrc/build.gradle")
        if (!buildSrc.exists()) return
        val content = buildSrc.readText()
        val updated = content.replace(
            Regex("""(kotlin-gradle-plugin:)\d+\.\d+\.\d+"""),
            "${'$'}1$kotlinVersion"
        )
        if (updated != content) {
            buildSrc.writeText(updated)
            logger.lifecycle("    ✓ buildSrc/build.gradle : kotlin-gradle-plugin → $kotlinVersion")
        }
    }

    private fun runBash(script: String, workDir: File, stepName: String) {
        val process = ProcessBuilder("bash", "-c", script)
            .directory(workDir)
            .inheritIO()
            .start()
        val exit = process.waitFor()
        if (exit != 0) error("[$stepName] a échoué avec le code de sortie $exit")
    }
}