package net.createmod.pondergradle

import net.createmod.pondergradle.nullability.PackageInfosExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class PonderGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("defaultPackageInfos", PackageInfosExtension::class.java, project)
    }
}
