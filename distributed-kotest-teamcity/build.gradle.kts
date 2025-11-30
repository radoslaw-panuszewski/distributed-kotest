plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    explicitApi()
}

java {
    withSourcesJar()
    withSourcesJar()
}

dependencies {
    compileOnly("org.jetbrains.teamcity:configs-dsl-kotlin-latest:2025.07")
    compileOnly(files("lib/configs-dsl-kotlin-commandLineRunner.jar"))
    compileOnly(files("lib/configs-dsl-kotlin-Gradle.jar"))
}

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
        }
    }
}