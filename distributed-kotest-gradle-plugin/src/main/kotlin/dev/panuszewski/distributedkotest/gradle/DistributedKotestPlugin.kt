package dev.panuszewski.distributedkotest.gradle

import dev.panuszewski.distributedkotest.gradle.batches.GroupTestsIntoBatches
import dev.panuszewski.distributedkotest.gradle.collecting.CollectTestResults
import dev.panuszewski.distributedkotest.gradle.excludepatterns.PrepareExcludeTestPatterns
import dev.panuszewski.distributedkotest.gradle.newtests.DiscoverNewTests
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

public class DistributedKotestPlugin : Plugin<Project> {

    override fun apply(rootProject: Project) {
        check(rootProject == rootProject.rootProject) { "DistributedKotestPlugin must be applied to the root project" }

        val collectTestResults = registerCollectTestResultsTask(rootProject)
        val discoverNewTests = registerDiscoverNewTestsTask(rootProject, collectTestResults)
        val groupTestsIntoBatches = registerGroupTestsIntoBatchesTask(rootProject, collectTestResults, discoverNewTests)
        val prepareExcludeTestPatterns = registerPrepareExcludeTestPatternsTask(rootProject, groupTestsIntoBatches)

        configureTestExcludes(rootProject, prepareExcludeTestPatterns)
    }

    private fun registerCollectTestResultsTask(rootProject: Project): TaskProvider<CollectTestResults> {
        rootProject.projectDir.resolve("test-results").mkdirs()

        return rootProject.tasks.register<CollectTestResults>("collectTestResults") {
            testResultsDir = rootProject.layout.projectDirectory.dir("test-results") // TODO some other dir?
            collectedTestResultsFile = rootProject.layout.buildDirectory.file("collectedTestResults.json")
        }
    }

    private fun registerDiscoverNewTestsTask(
        rootProject: Project,
        collectTestResults: TaskProvider<CollectTestResults>
    ) =
        rootProject.tasks.register<DiscoverNewTests>("discoverNewTests") {
            collectedTestResultsFile = collectTestResults.flatMap { it.collectedTestResultsFile }
            discoveredNewTestsFile = rootProject.layout.buildDirectory.file("discoveredNewTests.json")

            rootProject.allprojects {
                val sourceSets = extensions.findByType<SourceSetContainer>()
                val jvmTestSourceSet = sourceSets?.find { it.name == "jvmTest" }

                if (jvmTestSourceSet != null) {
                    testSourceSetOutput.from(jvmTestSourceSet.output) // TODO detect if normal or multiplatform build
                    testRuntimeClasspath.from(jvmTestSourceSet.runtimeClasspath)
                }
            }
        }

    private fun registerGroupTestsIntoBatchesTask(
        rootProject: Project,
        collectTestResults: TaskProvider<CollectTestResults>,
        discoverNewTests: TaskProvider<DiscoverNewTests>
    ) =
        // TODO print the summary even if the tasks are FROM-CACHE
        rootProject.tasks.register<GroupTestsIntoBatches>("groupTestsIntoBatches") {
            collectedTestResultsFile = collectTestResults.flatMap { it.collectedTestResultsFile }
            discoveredNewTestsFile = discoverNewTests.flatMap { it.discoveredNewTestsFile }
            numberOfBatches = System.getenv("NUMBER_OF_BATCHES")?.toInt() ?: 1
            batchesOutputDir = rootProject.layout.buildDirectory.dir("test-batches")
        }

    private fun registerPrepareExcludeTestPatternsTask(
        rootProject: Project,
        groupTestsIntoBatches: TaskProvider<GroupTestsIntoBatches>
    ) =
        rootProject.tasks.register<PrepareExcludeTestPatterns>("prepareExcludeTestPatterns") {
            batchNumber = System.getenv("BATCH_NUMBER")?.toInt() ?: 1
            batchesDir = groupTestsIntoBatches.flatMap { it.batchesOutputDir }
            excludePatternsFile = rootProject.layout.buildDirectory.file("testExcludePatterns.txt")
        }

    private fun configureTestExcludes(
        rootProject: Project,
        prepareExcludeTestPatterns: TaskProvider<PrepareExcludeTestPatterns>
    ) {
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