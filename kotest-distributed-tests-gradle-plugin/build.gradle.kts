@file:Suppress("UnstableApiUsage")

plugins {
    `kotlin-dsl`
    `maven-publish`
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