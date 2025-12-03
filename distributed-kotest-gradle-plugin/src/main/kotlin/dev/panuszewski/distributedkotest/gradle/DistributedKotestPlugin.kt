package dev.panuszewski.distributedkotest.gradle

import dev.panuszewski.distributedkotest.gradle.batches.GroupTestsIntoBatches
import dev.panuszewski.distributedkotest.gradle.excludepatterns.PrepareExcludeTestPatterns
import io.kotest.runner.junit.platform.discovery.Discovery
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import org.gradle.kotlin.dsl.withType
import org.junit.platform.engine.ConfigurationParameters
import org.junit.platform.engine.DiscoveryFilter
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.DiscoverySelectors
import java.util.Optional

public class DistributedKotestPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        check(project == project.rootProject) { "DistributedKotestPlugin must be applied to the root project" }

        project.projectDir.resolve("test-results").mkdirs()

        // TODO print the summary even if the tasks are FROM-CACHE
        val groupTestsIntoBatches by project.tasks.registering(GroupTestsIntoBatches::class) {
            numberOfBatches = System.getenv("NUMBER_OF_BATCHES")?.toInt() ?: 1
            testResultsDir = project.layout.projectDirectory.dir("test-results") // TODO build/test-results
            batchesOutputDir = project.layout.buildDirectory.dir("test-batches")
            testSourceSetOutput = project.extensions.getByType<SourceSetContainer>()["test"].output
            testRuntimeClasspath = project.configurations["testRuntimeClasspath"]
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