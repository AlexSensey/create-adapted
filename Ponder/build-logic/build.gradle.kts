plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    kotlin("jvm") version "1.9.23"
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("ponderPlugin") {
            id = "net.createmod.ponder.gradle"
            implementationClass = "net.createmod.pondergradle.PonderGradlePlugin"
        }
    }
}
