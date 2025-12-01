@file:Suppress("UnstableApiUsage")

plugins {
    `kotlin-dsl`
}

val initScripts by tasks.registering(Copy::class) {
    include("*.init.gradle.kts")
    from(sourceSets["main"].kotlin)
    into(layout.buildDirectory.dir("init-scripts"))
}

configurations {
    consumable("initScripts") {
        outgoing.artifact(initScripts)
    }
}