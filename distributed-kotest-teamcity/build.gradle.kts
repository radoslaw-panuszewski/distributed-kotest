import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0

plugins {
    `java-library`
    `maven-publish`
    `kotlin-dsl`
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

sourceSets {
    register("initScripts")
}

dependencies {
    compileOnly("org.jetbrains.teamcity:configs-dsl-kotlin-latest:2025.07")
    compileOnly(files("lib/configs-dsl-kotlin-commandLineRunner.jar"))
    compileOnly(files("lib/configs-dsl-kotlin-Gradle.jar"))

    "initScriptsCompileOnly"(gradleKotlinDsl())
}

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
        }
    }
}

tasks {
    processResources {
        from(sourceSets["initScripts"].allSource)
    }
}