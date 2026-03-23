import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * Convention plugin pour les sous-projets publiant un plugin Gradle.
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

group = "com.cheroliv"

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
    jvmToolchain(17)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
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