package conventions

import libs

plugins {
    alias(libs.plugins.jmh)
}

tasks {
    jmhJar {
        notCompatibleWithConfigurationCache("")
    }
}
