plugins {
    alias(libs.plugins.axion.release)
}

allprojects {
    group = "dev.panuszewski"
    version = rootProject.scmVersion.version
}