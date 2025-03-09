plugins {
    `java-library`
}

dependencies {
    compileOnlyApi(projects.modules.api)

    compileOnlyApi(libs.fastutil)
    implementation(libs.zstdJni)
}
