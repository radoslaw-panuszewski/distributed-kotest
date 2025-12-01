dependencyResolutionManagement {
    repositories {
        maven("https://download.jetbrains.com/teamcity-repository") {
            content {
                includeGroup("org.jetbrains.teamcity")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "distributed-kotest"

includeBuild("build-logic")

include("distributed-kotest-gradle-init-script")
include("distributed-kotest-gradle-plugin")
include("distributed-kotest-teamcity")
