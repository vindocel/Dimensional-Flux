pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases/")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
}

rootProject.name = "Dimensional-Flux"

stonecutter {
    create(rootProject) {
        versions("26.1.2")
        vcsVersion = "26.1.2"
    }
}
