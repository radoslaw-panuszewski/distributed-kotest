package dev.panuszewski.distributedkotest.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import org.gradle.kotlin.dsl.withType

public class DistributedKotestPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        check(project == project.rootProject) { "DistributedKotestPlugin must be applied to the root project" }

        val groupTestsIntoBatches by project.tasks.registering(GroupTestsIntoBatches::class) {
            numberOfBatches = System.getenv("NUMBER_OF_BATCHES")?.toInt() ?: 1
            testResultsDir = project.projectDir.resolve("test-results") // TODO build/test-results
            batchesOutputDir = project.layout.buildDirectory.dir("test-batches")
        }

        val prepareExcludeTestPatterns by project.tasks.registering(PrepareExcludeTestPatterns::class) {
            batchNumber = System.getenv("BATCH_NUMBER")?.toInt() ?: 1
            batchesDir = groupTestsIntoBatches.flatMap { it.batchesOutputDir }
            excludePatternsFile = project.layout.buildDirectory.file("testExcludePatterns.txt")
        }

        project.allprojects {
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