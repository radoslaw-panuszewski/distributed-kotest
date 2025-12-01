package conventions

import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
import libs

plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
    explicitApi()
    compilerOptions {
        languageVersion = KOTLIN_2_0
        apiVersion = KOTLIN_2_0
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
        }
    }
}

val classpathItems by configurations.registering { isCanBeResolved = true }

tasks.jar {
    from(classpathItems)
}
