@file:Suppress("UnstableApiUsage")

package conventions

import libs

plugins {
    `java-gradle-plugin`
    `maven-publish`
}

testing.suites {
    withType<JvmTestSuite>().configureEach {
        useJUnitJupiter()

        dependencies {
            implementation(libs.jupiter.api)
            implementation(libs.kotest.assertions)
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
