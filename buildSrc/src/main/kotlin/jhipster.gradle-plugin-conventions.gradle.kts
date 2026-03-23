import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Convention plugin pour les sous-projets publiant un plugin Gradle.
 *
 * Cible Java 24+ — les consommateurs du plugin doivent utiliser Java 24 minimum.
 *
 * Note : `kotlin-dsl` est appliqué par buildSrc/build.gradle.kts sur buildSrc
 * lui-même, ce qui rend le compilateur Kotlin disponible ici.
 * Ce convention plugin applique uniquement les plugins nécessaires aux
 * sous-projets consommateurs (java-gradle-plugin, maven-publish, plugin-publish).
 */
plugins {
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish")
    kotlin("jvm")
}

group = "dev.jhipster"

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_24
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
    jvmToolchain(24)
}

java {
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("FAILED", "SKIPPED")
    }
}

publishing {
    repositories {
        maven {
            name = "localRepo"
            url  = uri(rootProject.layout.buildDirectory.dir("local-repo"))
        }
    }
}