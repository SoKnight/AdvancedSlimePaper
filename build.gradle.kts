plugins {
    base
}

group = "com.infernalsuite.aswm"
version = "3.0.0+patch.2"

subprojects {
    if (project.path.startsWith(":modules:")) {
        apply(plugin = "aswm")
    }
}
