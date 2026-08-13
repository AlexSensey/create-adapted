val providerKey = "git_hash_provider"

// Compute once, read in subprojects. Create: Adapted is distributed as a
// single repository containing Ponder as an included build. Asking Git from
// inside that composite build is unreliable on Windows, so allow the outer
// build/release process to provide a stable identifier without requiring a
// nested Git checkout.
if (project == rootProject) {
    ext[providerKey] = providers.gradleProperty("createAdaptedGitHash")
        .orElse("create-adapted-0.8")
}

tasks.withType<Jar> {
    manifest.attributes(mapOf("Git-Hash" to "\"${findHash()}\""))
}

fun findHash(): String {
    return (rootProject.ext[providerKey] as Provider<*>).get() as String
}
