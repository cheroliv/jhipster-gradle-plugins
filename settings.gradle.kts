pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "jhipster-gradle-plugins"

include("persistence")
include("assistant")