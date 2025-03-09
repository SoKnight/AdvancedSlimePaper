plugins {
    application
    `package`
}

application {
    mainClass = "com.infernalsuite.aswm.importer.SWMImporter"
}

dependencies {
    packaged(projects.modules.core)
}

tasks.shadowJar {
    minimize()
}
