dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "kotest-distributed-tests"

include("kotest-distributed-tests-gradle-plugin")
include("kotest-distributed-tests-teamcity")
