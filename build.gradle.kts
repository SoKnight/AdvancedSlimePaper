plugins {
    base
}

group = "com.infernalsuite.aswm"
version = "3.0.0+patch.1"

subprojects {
    if (project.path.startsWith(":modules:")) {
        apply(plugin = "aswm")
    }
}

//subprojects {
//    apply(plugin = "java")
//    apply(plugin = "maven-publish")
//
//    java {
//        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
//    }
//
//    repositories {
//        mavenCentral()
//        maven("https://repo.papermc.io/repository/maven-public/")
//        maven("https://repo.codemc.io/repository/nms/")
//        maven("https://repo.rapture.pw/repository/maven-releases/")
//        maven("https://repo.glaremasters.me/repository/concuncan/")
//        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
//        maven("https://oss.sonatype.org/content/repositories/snapshots/")
//    }
//
//    tasks.withType<JavaCompile> {
//        options.encoding = Charsets.UTF_8.name()
//    }
//
//    tasks.withType<Javadoc> {
//        options.encoding = Charsets.UTF_8.name()
//    }
//
//    tasks.withType<ProcessResources> {
//        filteringCharset = Charsets.UTF_8.name()
//    }
//}
