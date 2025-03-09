plugins {
    `java-library`
}

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    api(libs.adventureNbt)
    compileOnlyApi(libs.jetbrainsAnnotations)

    compileOnlyApi(libs.paperApi)
}

