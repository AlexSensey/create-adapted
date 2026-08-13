plugins {
    alias(libs.plugins.setup.git.hash)
}

val baseVersion = findProperty("version") as String
val buildNumber: String = providers.environmentVariable("BUILD_NUMBER").orElse("99999").get()
version = baseVersion.replace("<build>", buildNumber) + "+mc${libs.versions.minecraft.get()}"
