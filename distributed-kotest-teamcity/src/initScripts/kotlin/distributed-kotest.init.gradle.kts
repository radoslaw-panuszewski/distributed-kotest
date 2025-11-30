rootProject {
    buildscript {
        repositories {
            maven("https://central.sonatype.com/repository/maven-snapshots")
            mavenCentral()
        }

        dependencies {
            classpath("dev.panuszewski:distributed-kotest-gradle-plugin:0.1.0-SNAPSHOT")
        }
    }

    afterEvaluate {
        apply(plugin = "dev.panuszewski.distributed-kotest")
    }
}
