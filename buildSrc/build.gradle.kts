/**
 * Configuration de buildSrc.
 *
 * `kotlin-dsl` ici s'applique à buildSrc lui-même — il permet d'écrire
 * les precompiled script plugins en Kotlin et rend les plugins Kotlin
 * disponibles sur le classpath pour les convention plugins.
 */
plugins { `kotlin-dsl` }

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Plugin Kotlin JVM — requis par kotlin("jvm") dans le convention plugin
    implementation(libs.kotlin.gradle.plugin)
    // Plugin Publish — requis par id("com.gradle.plugin-publish") dans le convention plugin
    implementation(libs.plugin.publish.gradle.plugin)
}