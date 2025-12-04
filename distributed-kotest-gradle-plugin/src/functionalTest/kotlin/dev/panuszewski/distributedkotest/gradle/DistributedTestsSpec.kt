package dev.panuszewski.distributedkotest.gradle

import dev.panuszewski.distributedkotest.gradle.framework.BuildOutcome.BUILD_SUCCESSFUL
import dev.panuszewski.distributedkotest.gradle.framework.GradleSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Test

class DistributedTestsSpec : GradleSpec() {

    @Test
    fun `should distribute tests evenly`() {
        // given
        val numberOfTests = 50
        val numberOfBatches = 5

        buildGradleKts { KOTEST_SETUP }

        (1..numberOfTests).forEach { i ->
            customProjectFile("src/test/kotlin/com/example${i}/Spec${i}.kt") {
                """
                package com.example$i
                    
                import io.kotest.core.spec.style.FunSpec
                import io.kotest.matchers.shouldBe
                
                class Spec$i : FunSpec() {
                    init {
                        test("test $i") {
                            1 shouldBe 1
                        }
                    }
                }
                """
            }

            customProjectFile("build/distributed-kotest/test-results/test/TEST-com.example${i}.Spec${i}.xml") {
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite name="com.example${i}.Spec$i">
                  <testcase name="test $i" classname="com.example${i}.Spec$i" time="1"/>
                </testsuite>
                """
            }
        }

        // when
        val results = (1..numberOfBatches).map { i ->
            buildEnvironment["BATCH_NUMBER"] = "$i"
            buildEnvironment["NUMBER_OF_BATCHES"] = "$numberOfBatches"
            runGradle("test")
        }

        // then
        results.shouldForAll { result ->
            result.buildOutcome shouldBe BUILD_SUCCESSFUL
            result.passedTests() shouldHaveSize numberOfTests / numberOfBatches
        }
        results.flatMap(BuildResult::passedTests) shouldHaveSize numberOfTests
    }

    @Test
    fun `should run new tests only in first batch`() {
        // given
        val numberOfTests = 12
        val numberOfBatches = 2

        buildGradleKts { KOTEST_SETUP }

        (1..numberOfTests).forEach { i ->
            customProjectFile("src/test/kotlin/com/example${i}/Spec${i}.kt") {
                """
                package com.example$i
                    
                import io.kotest.core.spec.style.FunSpec
                import io.kotest.matchers.shouldBe
                
                class Spec$i : FunSpec() {
                    init {
                        test("test $i") {
                            1 shouldBe 1
                        }
                    }
                }
                """
            }

            if (i <= 10) {
                customProjectFile("build/distributed-kotest/test-results/test/TEST-com.example${i}.Spec${i}.xml") {
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <testsuite name="com.example${i}.Spec$i">
                      <testcase name="test $i" classname="com.example${i}.Spec$i" time="1"/>
                    </testsuite>
                    """
                }
            }
        }

        // when
        val results = (1..numberOfBatches).map { i ->
            buildEnvironment["BATCH_NUMBER"] = "$i"
            buildEnvironment["NUMBER_OF_BATCHES"] = "$numberOfBatches"
            runGradle("test")
        }

        // then
        results.shouldForAll { it.buildOutcome shouldBe BUILD_SUCCESSFUL }
        results[0].passedTests() shouldHaveSize 7
        results[1].passedTests() shouldHaveSize 5
    }

    companion object {
        private const val KOTEST_SETUP = """
            import org.gradle.api.tasks.testing.logging.TestLogEvent.*
                
            plugins {
                id("dev.panuszewski.distributed-kotest")
                kotlin("jvm") version "2.2.21"
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
               testImplementation("io.kotest:kotest-runner-junit5:6.0.6")
            }
            
            testing.suites.withType<JvmTestSuite>() {
                useJUnitJupiter()
                targets.all {
                   testTask {
                       testLogging {
                           events(PASSED, FAILED, SKIPPED)
                       }      
                   }
               }
            }
            """
    }
}

private fun BuildResult.passedTests(): List<String> =
    output.lines().filter { it.endsWith("PASSED") }
