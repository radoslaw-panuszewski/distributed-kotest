package dev.panuszewski.distributedkotest.teamcity.util

import jetbrains.buildServer.configs.kotlin.BuildSteps
import jetbrains.buildServer.configs.kotlin.ui.add

internal fun BuildSteps.ifParamDoesNotExist(param: String, steps: BuildSteps.() -> Unit) {
    steps()

    items.forEach {
        it.conditions.add { doesNotExist(param) }
    }
}
