package dev.panuszewski.distributedkotest.teamcity.steps

import jetbrains.buildServer.configs.kotlin.BuildSteps
import jetbrains.buildServer.configs.kotlin.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.script

internal fun BuildSteps.copyInitScript(initScriptName: String, customizer: ScriptBuildStep.() -> Unit = {}) {
    script {
        name = "Copy init script"

        val initScriptContent = javaClass.getResource("/$initScriptName").readText()
        scriptContent = """
            |cat > $initScriptName <<EOF
            |$initScriptContent
            |EOF
            """.trimMargin()

        customizer()
    }
}