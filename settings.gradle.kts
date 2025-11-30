dependencyResolutionManagement {
    repositories {
        maven("https://download.jetbrains.com/teamcity-repository")
        maven("https://interview.teamcity.com/app/dsl-plugins-repository")
        mavenCentral()
    }
}

rootProject.name = "distributed-kotest"

include("distributed-kotest-gradle-plugin")
include("distributed-kotest-teamcity")
