val providerKey = "git_hash_provider"

// compute once, read in subprojects
if (project == rootProject) {
    ext[providerKey] = providers.zip(exec("git", "rev-parse", "HEAD"), exec("git", "status", "--porcelain")) { hash, status ->
        val hasUnstaged = !status.replace("/M gradlew(\\.bat)?/", "").isEmpty()
        return@zip hash + (if (hasUnstaged) "-modified" else "")
    }
}

tasks.withType<Jar> {
    manifest.attributes(mapOf("Git-Hash" to "\"${findHash()}\""))
}

fun exec(vararg args: String): Provider<String> {
    return providers.exec {
        commandLine = args.asList()
    }.standardOutput.asText.map { it.trim() }
}

fun findHash(): String {
    return (rootProject.ext[providerKey] as Provider<*>).get() as String
}
