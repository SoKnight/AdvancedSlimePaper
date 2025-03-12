plugins {
    `package`
}

dependencies {
    packaged(projects.modules.core)
    packaged(projects.modules.loaders)

    packaged(libs.configurateYaml)
    packaged(libs.incendoCloudAnnotations)
    packaged(libs.incendoCloudMinecraftExtras)
    packaged(libs.incendoCloudPaper)
    packaged(libs.lettuceCore)

    compileOnly(libs.paperApi)
}

tasks.processResources {
    inputs.property("project.version", rootProject.version)

    filesMatching(listOf("plugin.yml")) {
        expand(project.properties)
    }
}

tasks.shadowJar {
    dependencies {
        exclude { it.moduleGroup == "io.netty" }
        exclude { it.moduleGroup == "net.kyori" && it.moduleName == "adventure-nbt" }
    }

    relocate("org.bstats", "com.infernalsuite.aswm.internal.bstats")
    relocate("ninja.leaping.configurate", "com.infernalsuite.aswm.internal.configurate")
    relocate("com.flowpowered.nbt", "com.infernalsuite.aswm.internal.nbt")
    relocate("io.lettuce", "com.infernalsuite.aswm.internal.lettuce")
    relocate("org.bson", "com.infernalsuite.aswm.internal.bson")
    relocate("net.kyori.option", "com.infernalsuite.aswm.internal.kyori.option")
    relocate("com.github.luben.zstd", "com.infernalsuite.aswm.internal.zstd")
}
