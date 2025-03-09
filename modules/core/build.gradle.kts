plugins {
    `java-library`
}

dependencies {
    compileOnlyApi(projects.modules.api)

    implementation(libs.zstdJni)
}
