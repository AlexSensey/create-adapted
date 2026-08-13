pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "ponder"

for (platform in listOf("common", "neoforge")) {
    include(platform)

    include(":catnip:$platform")
}

includeBuild("build-logic")
