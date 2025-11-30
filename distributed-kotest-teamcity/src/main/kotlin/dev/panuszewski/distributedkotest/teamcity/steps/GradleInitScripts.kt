package dev.panuszewski.distributedkotest.teamcity.steps

import jetbrains.buildServer.configs.kotlin.BuildSteps
import jetbrains.buildServer.configs.kotlin.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.script

internal fun BuildSteps.copyInitScript(initScript: String, customizer: ScriptBuildStep.() -> Unit = {}) {
    script {
        val initScriptContent = javaClass.getResource("/$initScript").readText()
        scriptContent = """
            cat << 'EOF' > distributed-kotest.init.gradle.kts
            $initScriptContent
            EOF
            """

        customizer()
    }
}