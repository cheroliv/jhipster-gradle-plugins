package dev.jhipster.assistant

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Plugin JHipster Assistant — SNAPSHOT, non publié.
 *
 * Placeholder : l'implémentation sera ajoutée après la stabilisation
 * et publication de [dev.jhipster.persistence.JHipsterPersistencePlugin].
 *
 * Fonctionnalités prévues :
 * - RAG sur la base de code complète (codebase, generated, jdl, config)
 * - RAG sur documentation officielle (JHipster, Kotlin, Gradle, Arrow)
 * - Conversation AsciiDoc persistée avec mémoire via RAG
 * - Serveur MCP stdio + HTTP SSE (OpenCoder, Claude Desktop)
 * - Tools MCP : readFile, writeFile, patchFile, listFiles, searchCode
 */
class JHipsterAssistantPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.logger.lifecycle(
            "[jhipster-assistant] Plugin chargé — implémentation à venir."
        )
    }
}
