plugins {
    `java-library`
}

dependencies {
    api(projects.modules.api)

    compileOnlyApi(libs.fastutil)
    implementation(libs.zstdJni)
}
