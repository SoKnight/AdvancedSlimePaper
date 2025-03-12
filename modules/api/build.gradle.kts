plugins {
    `java-library`
}

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    compileOnlyApi(libs.adventureNbt)
    compileOnlyApi(libs.jetbrainsAnnotations)

    compileOnlyApi(libs.paperApi)
}

