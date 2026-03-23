/**
 * Sous-projet : persistence
 * Artefact    : dev.jhipster.persistence
 *
 * Plugin léger — aucune dépendance LangChain4j, Docker, pgvector ou MCP.
 * Le convention plugin `jhipster.gradle-plugin-conventions` configure
 * Kotlin, Java, test et publication.
 */
plugins { id("jhipster.gradle-plugin-conventions") }

group = "com.cheroliv"

version = libs.versions.persistence.get()

dependencies {
    implementation(libs.kotlin.stdlib)

    testImplementation(libs.junit.jupiter)
    testImplementation(gradleTestKit())
    testRuntimeOnly(libs.junit.platform.launcher)
}

gradlePlugin {
    website = "https://github.com/cheroliv/jhipster-gradle-plugins"
    vcsUrl  = "https://github.com/cheroliv/jhipster-gradle-plugins"

    plugins {
        create("jhipsterPersistence") {
            id                  = "com.cheroliv.jhipster.persistence"
            implementationClass = "dev.jhipster.persistence.JHipsterPersistencePlugin"
            displayName         = "JHipster Persistence Plugin"
            description         = """
                Orchestre le cycle de régénération JHipster (clean/generate/sync)
                sans perdre le code métier Kotlin persisté dans __codebase__/.
                Résolution par convention — aucune extension DSL requise.
            """.trimIndent()
            tags = listOf("jhipster", "kotlin", "codegen", "gradle", "persistence")
        }
    }
}
