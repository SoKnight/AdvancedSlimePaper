plugins {
    `java-library`
}

dependencies {
    compileOnlyApi(projects.modules.api)

    implementation(libs.commonsIo)
    implementation(libs.lettuceCore)

    compileOnlyApi(libs.paperApi)
}
