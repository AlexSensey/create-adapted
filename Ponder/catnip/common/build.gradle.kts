plugins {
    alias(libs.plugins.loom)
    alias(libs.plugins.configure.platform)
}

dependencies {
    minecraft(libs.minecraft)
    compileOnly(libs.bundles.mixin)
}

loom {
    accessWidenerPath = file("catnip_common_source.accesswidener")
}
