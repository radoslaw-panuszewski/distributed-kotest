@file:Suppress("UnstableApiUsage")

package conventions

import libs
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    `maven-publish`
}

afterEvaluate {
    tasks {
        withType<KotlinCompile>().configureEach {
            compilerOptions {
                languageVersion = KOTLIN_2_0
                apiVersion = KOTLIN_2_0
            }
        }
    }
}

testing.suites {
    withType<JvmTestSuite>().configureEach {
        useJUnitJupiter()

        dependencies {
            implementation(libs.jupiter.api)
            implementation(libs.kotest.assertions)
        }

        targets.all {
            tasks.check {
                dependsOn(testTask)
            }
        }
    }

    register<JvmTestSuite>("functionalTest") {
        dependencies {
            implementation(gradleTestKit())
        }
    }
}

configurations {
    named("functionalTestRuntimeClasspath") {
        extendsFrom(configurations.testRuntimeClasspath.get())
    }
}
