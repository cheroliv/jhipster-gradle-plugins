/**
 * Sous-projet : assistant
 * Artefact    : dev.jhipster.assistant
 *
 * Plugin d'assistance au code — dépendances lourdes isolées ici.
 * Statut : SNAPSHOT — non publié.
 */
plugins {
    id("jhipster.gradle-plugin-conventions")
    alias(libs.plugins.kotlin.serialization)
}

group = "com.cheroliv"

version = libs.versions.assistant.get()

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.jdk8)
    implementation(libs.kotlinx.serialization.json)

    // RAG
    implementation(libs.langchain4j.core)
    implementation(libs.langchain4j.pgvector)
    implementation(libs.langchain4j.embeddings.minilm)
    implementation(libs.langchain4j.ollama)
    implementation(libs.langchain4j.gemini)
    implementation(libs.langchain4j.mistral)

    // Infrastructure pgvector
    implementation(libs.docker.java.core)
    implementation(libs.docker.java.httpclient5)
    implementation(libs.postgresql.jdbc)

    // MCP SDK + transport HTTP SSE
    implementation(libs.mcp.sdk.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.sse)

    testImplementation(libs.junit.jupiter)
    testImplementation(gradleTestKit())
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit5)
}

gradlePlugin {
    website = "https://github.com/cheroliv/jhipster-gradle-plugins"
    vcsUrl  = "https://github.com/cheroliv/jhipster-gradle-plugins"

    plugins {
        create("jhipsterAssistant") {
            id                  = "dev.jhipster.assistant"
            implementationClass = "dev.jhipster.assistant.JHipsterAssistantPlugin"
            displayName         = "JHipster Assistant Plugin"
            description         = """
                Assistance au code Kotlin JHipster via RAG pgvector et LLM multi-provider.
                Conversation AsciiDoc persistée, serveur MCP pour OpenCoder et Claude Desktop.
                Documentation officielle indexée (JHipster, Kotlin, Gradle, Arrow).
            """.trimIndent()
            tags = listOf(
                "jhipster", "kotlin", "rag", "mcp", "ai",
                "langchain4j", "pgvector", "assistant"
            )
        }
    }
}
