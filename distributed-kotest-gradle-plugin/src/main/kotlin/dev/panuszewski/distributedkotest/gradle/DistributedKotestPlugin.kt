package dev.panuszewski.distributedkotest.gradle

import dev.panuszewski.distributedkotest.gradle.batches.GroupTestsIntoBatches
import dev.panuszewski.distributedkotest.gradle.excludepatterns.PrepareExcludeTestPatterns
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import org.gradle.kotlin.dsl.withType

public class DistributedKotestPlugin : Plugin<Project> {

    override fun apply(rootProject: Project) {
        check(rootProject == rootProject.rootProject) { "DistributedKotestPlugin must be applied to the root project" }

        rootProject.projectDir.resolve("test-results").mkdirs()

        // TODO print the summary even if the tasks are FROM-CACHE
        val groupTestsIntoBatches by rootProject.tasks.registering(GroupTestsIntoBatches::class) {
            numberOfBatches = System.getenv("NUMBER_OF_BATCHES")?.toInt() ?: 1
            testResultsDir = rootProject.layout.projectDirectory.dir("test-results") // TODO build/test-results
            batchesOutputDir = rootProject.layout.buildDirectory.dir("test-batches")

            rootProject.allprojects {
                val sourceSets = extensions.findByType<SourceSetContainer>()
                val jvmTestSourceSet = sourceSets?.find { it.name == "jvmTest" }

                if (jvmTestSourceSet != null) {
                    testSourceSetOutput.from(jvmTestSourceSet.output) // TODO detect if normal or multiplatform build
                    testRuntimeClasspath.from(jvmTestSourceSet.runtimeClasspath)
                }
            }
        }

        val prepareExcludeTestPatterns by rootProject.tasks.registering(PrepareExcludeTestPatterns::class) {
            batchNumber = System.getenv("BATCH_NUMBER")?.toInt() ?: 1
            batchesDir = groupTestsIntoBatches.flatMap { it.batchesOutputDir }
            excludePatternsFile = rootProject.layout.buildDirectory.file("testExcludePatterns.txt")
        }

        rootProject.allprojects {
            tasks.withType<Test>().configureEach {
                inputs.files(prepareExcludeTestPatterns.flatMap { it.excludePatternsFile })
                // TODO do it only if debugMode is enabled
                outputs.cacheIf { false }
                outputs.upToDateWhen { false }

                doFirst {
                    val excludePatternsFile = inputs.files.filter { it.name.endsWith(".txt") }.singleFile
                    val excludePatterns = excludePatternsFile.readLines()
                    excludePatterns.forEach { pattern -> filter.excludeTestsMatching(pattern) }
                }
            }
        }
    }
}