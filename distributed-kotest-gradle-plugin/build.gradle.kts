plugins {
    `kotlin-dsl`
    id("conventions.gradle-plugin")
    id("conventions.jmh")
}

kotlin {
    explicitApi()
}

gradlePlugin {
    plugins {
        create("distributed-kotest", Action {
            id = "dev.panuszewski.distributed-kotest"
            implementationClass = "dev.panuszewski.distributedkotest.gradle.DistributedKotestPlugin"
        })
    }
}

dependencies {
    implementation(libs.jackson.moduleKotlin)
    implementation(libs.jackson.dataformatJsr310)
    implementation(libs.commons.numbersCombinatorics)
    implementation(libs.kotest.runnerJunit5)
}
