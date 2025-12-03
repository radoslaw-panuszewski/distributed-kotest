package conventions

import libs

plugins {
    alias(libs.plugins.axion.release)
}

check(project == rootProject) { "versioning convention must be applied to the root project" }

allprojects {
    group = "dev.panuszewski"
    version = "local" // rootProject.scmVersion.version
}
