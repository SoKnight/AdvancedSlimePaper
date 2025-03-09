plugins {
    `package`
}

dependencies {
    packaged(projects.modules.loaders)

    packaged(libs.configurateYaml)
    packaged(libs.incendoCloudAnnotations)
    packaged(libs.incendoCloudMinecraftExtras)
    packaged(libs.incendoCloudPaper)

    compileOnly(libs.paperApi)
}

tasks.shadowJar {
    relocate("org.bstats", "com.grinderwolf.swm.internal.bstats")
    relocate("ninja.leaping.configurate", "com.grinderwolf.swm.internal.configurate")
    relocate("com.flowpowered.nbt", "com.grinderwolf.swm.internal.nbt")
    relocate("io.lettuce", "com.grinderwolf.swm.internal.lettuce")
    relocate("org.bson", "com.grinderwolf.swm.internal.bson")
}
