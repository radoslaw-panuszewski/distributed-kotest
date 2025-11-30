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
    compileOnly("org.jetbrains.teamcity:configs-dsl-kotlin-plugins-latest:1.0-SNAPSHOT")
}

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
        }
    }
}