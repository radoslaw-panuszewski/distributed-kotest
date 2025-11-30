@file:Suppress("UnstableApiUsage")

plugins {
    `kotlin-dsl`
    `maven-publish`
}

gradlePlugin {
    plugins {
        create("kotest-distributed-tests") {
            id = "dev.panuszewski.kotest-distributed-tests"
            implementationClass = "dev.panuszewski.kotestdistributed.gradle.KotestDistributedTestsPlugin"
        }
    }
}

dependencies {
    implementation(libs.jackson.moduleKotlin)
    implementation(libs.jackson.dataformatJsr310)
    implementation(libs.commons.numbersCombinatorics)

    testImplementation(libs.jupiter.api)
    testImplementation(libs.kotest.assertions)
}

testing.suites {
    register<JvmTestSuite>("functionalTest")

    withType<JvmTestSuite>().configureEach {
        useJUnitJupiter()
    }
}