package dev.panuszewski.distributedkotest.teamcity

import dev.panuszewski.distributedkotest.teamcity.steps.copyInitScript
import dev.panuszewski.distributedkotest.teamcity.steps.packGradleCaches
import dev.panuszewski.distributedkotest.teamcity.steps.unpackGradleCaches
import dev.panuszewski.distributedkotest.teamcity.steps.unpackTestResults
import dev.panuszewski.distributedkotest.teamcity.util.ifParamDoesNotExist
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.matrix

public class DistributedTests(
    testTask: String,
    numberOfBatches: Int,
    debugMode: Boolean = false,
    cacheGradleHome: Boolean = true,
    customizer: BuildType.() -> Unit = {}
) : BuildType() {
    init {
        name = "JVM tests"
        artifactRules = buildString {
            appendLine("+:**/build/test-results/**/TEST-*.xml => test-results-%batchNumber%.zip")

            if (cacheGradleHome) {
                appendLine("+:gradle-caches.z*")
            }
        }

        dependencies {
            artifacts(this@DistributedTests) {
                buildRule = lastFinished()

                artifactRules = buildString {
                    appendLine("?:test-results*.zip => test-results")

                    if (cacheGradleHome) {
                        appendLine("?:gradle-caches.z* => %env.HOME%/.gradle/caches")
                    }
                }
            }
        }

        features {
            matrix {
                param("batchNumber", (1..numberOfBatches).map { value(it.toString()) })
            }
        }

        params {
            param("env.BATCH_NUMBER", "%batchNumber%")
            param("env.NUMBER_OF_BATCHES", "$numberOfBatches")
        }

        steps {
            unpackTestResults()

            ifParamDoesNotExist("env.SKIP_BUILD") {
                unpackGradleCaches()

                copyInitScript("distributed-kotest.init.gradle.kts")

                gradle {
                    tasks = testTask
                    gradleParams = "--init-script distributed-kotest.init.gradle.kts"
                }

                script {
                    name = "Clear previous test results"
                    scriptContent = "rm -rf test-results"
                }

                packGradleCaches()
            }
        }

        customizer()
    }
}