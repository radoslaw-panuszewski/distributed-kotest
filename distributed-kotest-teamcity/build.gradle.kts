plugins {
    id("conventions.library")
}

dependencies {
    compileOnly("org.jetbrains.teamcity:configs-dsl-kotlin-latest:2025.07")
    compileOnly(files("lib/configs-dsl-kotlin-commandLineRunner.jar"))
    compileOnly(files("lib/configs-dsl-kotlin-Gradle.jar"))

    classpathItems(project(":distributed-kotest-gradle-init-script", "initScripts"))
}
