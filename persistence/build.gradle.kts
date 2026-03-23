/**
 * Subproject: persistence
 * Artifact  : dev.jhipster.persistence
 *
 * Lightweight plugin — no LangChain4j, Docker, pgvector or MCP dependencies.
 * The convention plugin `jhipster.gradle-plugin-conventions` configures
 * Kotlin, Java, testing and publishing.
 */
plugins {
    `java-library`
    signing
    `maven-publish`
    `java-gradle-plugin`
//    alias(libs.plugins.kotlin.jvm)
//    alias(libs.plugins.publish)
    id("jhipster.gradle-plugin-conventions")
}

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
                Orchestrates the JHipster regeneration cycle (clean/generate/sync)
                without losing the Kotlin business code persisted in __codebase__/.
                Convention-based resolution — no DSL extension required.
            """.trimIndent()
            tags = listOf("jhipster", "kotlin", "codegen", "persistence")
        }
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            if (name == "pluginMaven") {
                pom {
                    name.set(gradlePlugin.plugins.getByName("jhipsterPersistence").displayName)
                    description.set(gradlePlugin.plugins.getByName("jhipsterPersistence").description)
                    url.set(gradlePlugin.website.get())
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("cheroliv")
                            name.set("cheroliv")
                            email.set("cheroliv.developer@gmail.com")
                        }
                    }
                    scm {
                        connection.set(gradlePlugin.vcsUrl.get())
                        developerConnection.set(gradlePlugin.vcsUrl.get())
                        url.set(gradlePlugin.vcsUrl.get())
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "sonatype"
            url = (if (version.toString().endsWith("-SNAPSHOT"))
                uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            else
                uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"))
            credentials {
                username = project.findProperty("ossrhUsername") as? String
                password = project.findProperty("ossrhPassword") as? String
            }
        }
        mavenCentral()
    }
}

signing {
    // Only sign release versions, not snapshots
    val isReleaseVersion = !version.toString().endsWith("-SNAPSHOT")
    if (isReleaseVersion) sign(publishing.publications)
    useGpgCmd()
}

java {
    withJavadocJar()
    withSourcesJar()
}