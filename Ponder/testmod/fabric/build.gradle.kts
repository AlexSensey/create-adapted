plugins {
    alias(libs.plugins.loom)
    alias(libs.plugins.configure.platform)
}

loom {
    mods.register("testmod") {
        sourceSet(sourceSets.main.get())
        sourceSet(sourceSets.client.get())
    }

    runs {
        named("server") {
            runDir = "run/server"
        }

        configureEach {
            ideConfigGenerated(true)
            vmArg("-Dmixin.debug.export=true")
            vmArg("-XX:+IgnoreUnrecognizedVMOptions")
            vmArg("-XX:+AllowEnhancedClassRedefinition")
        }
    }
}

dependencies {
    minecraft(libs.minecraft)
    implementation(libs.bundles.fabric)
    api(project(":fabric"))
    clientCompileOnly(project(":fabric", configuration = "clientJar"))
    clientCompileOnly(project(":catnip:fabric", configuration = "clientJar"))
}
