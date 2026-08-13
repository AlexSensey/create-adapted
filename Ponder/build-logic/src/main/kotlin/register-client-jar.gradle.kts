// FIXME: we need to wrap the client output in a jar for now
// https://github.com/FabricMC/fabric-loom/issues/1525

val sourceSets: SourceSetContainer = extensions.getByType<SourceSetContainer>()

val clientJar = tasks.register<Jar>("clientJar") {
    from(sourceSets["client"].output)
    archiveClassifier = "client"
}

configurations.consumable("clientJar")
artifacts.add("clientJar", clientJar)
