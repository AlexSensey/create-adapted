val compileOnly: Configuration by configurations.getting
val clientCompileOnly: Configuration by configurations.getting

val commonMainJava: Configuration by configurations.dependencyScope("commonMainJava")
val commonMainResources: Configuration by configurations.dependencyScope("commonMainResources")

val commonClientJava: Configuration by configurations.dependencyScope("commonClientJava")
val commonClientResources: Configuration by configurations.dependencyScope("commonClientResources")

val commonPath = project.parent!!.path + ":common"

dependencies {
    compileOnly(project(commonPath))
    clientCompileOnly(project(commonPath, configuration = "commonClientOutput"))

    commonMainJava(project(path = commonPath, configuration = "commonMainJava"))
    commonMainResources(project(path = commonPath, configuration = "commonMainResources"))

    commonClientJava(project(path = commonPath, configuration = "commonClientJava"))
    commonClientResources(project(path = commonPath, configuration = "commonClientResources"))
}

val resolvableCommonMainJava: Configuration by configurations.resolvable("resolvableCommonMainJava") {
    extendsFrom(commonMainJava)
}

val resolvableCommonMainResources: Configuration by configurations.resolvable("resolvableCommonMainResources") {
    extendsFrom(commonMainResources)
}

val resolvableCommonClientJava: Configuration by configurations.resolvable("resolvableCommonClientJava") {
    extendsFrom(commonClientJava)
}

val resolvableCommonClientResources: Configuration by configurations.resolvable("resolvableCommonClientResources") {
    extendsFrom(commonClientResources)
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(resolvableCommonMainJava)
    source(resolvableCommonMainJava)
}

tasks.named<JavaCompile>("compileClientJava") {
    dependsOn(resolvableCommonClientJava)
    source(resolvableCommonClientJava)
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(resolvableCommonMainResources)
    from(resolvableCommonMainResources)
}

tasks.named<ProcessResources>("processClientResources") {
    dependsOn(resolvableCommonClientResources)
    from(resolvableCommonClientResources)
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(resolvableCommonMainJava, resolvableCommonMainResources)
    from(resolvableCommonMainJava, resolvableCommonMainResources)
}
