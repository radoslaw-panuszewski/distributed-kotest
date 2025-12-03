plugins {
    `kotlin-dsl`
    id("conventions.gradle-plugin")
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
    implementation("io.kotest:kotest-runner-junit5:6.0.7")
}
