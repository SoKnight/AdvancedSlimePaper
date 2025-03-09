plugins {
    `java-library`
}

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    api(libs.flowNbt)
    compileOnlyApi(libs.jetbrainsAnnotations)

    compileOnlyApi(libs.paperApi)
}

