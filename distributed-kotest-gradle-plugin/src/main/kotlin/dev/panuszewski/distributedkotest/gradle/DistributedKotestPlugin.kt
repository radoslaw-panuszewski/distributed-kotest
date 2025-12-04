package dev.panuszewski.distributedkotest.gradle

import dev.panuszewski.distributedkotest.gradle.batches.GroupTestsIntoBatches
import dev.panuszewski.distributedkotest.gradle.collecting.CollectTestResults
import dev.panuszewski.distributedkotest.gradle.excludepatterns.PrepareExcludeTestPatterns
import dev.panuszewski.distributedkotest.gradle.newtests.DiscoverNewTests
import dev.panuszewski.distributedkotest.gradle.testplan.PrintTestPlan
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
        val printTestPlan = registerPrintTestPlanTask(rootProject, collectTestResults, groupTestsIntoBatches)
        val prepareExcludeTestPatterns = registerPrepareExcludeTestPatternsTask(rootProject, groupTestsIntoBatches, printTestPlan)

        configureTestExcludes(rootProject, prepareExcludeTestPatterns)
    }

    private fun registerCollectTestResultsTask(rootProject: Project) =
        rootProject.tasks.register<CollectTestResults>("collectTestResults") {
            testResultsDir = rootProject.layout.buildDirectory.dir("$PATH_PREFIX/test-results").apply { get().asFile.mkdirs() }
            collectedTestResultsFile = rootProject.layout.buildDirectory.file("$PATH_PREFIX/collectedTestResults.json")
        }

    private fun registerDiscoverNewTestsTask(
        rootProject: Project,
        collectTestResults: TaskProvider<CollectTestResults>
    ) =
        rootProject.tasks.register<DiscoverNewTests>("discoverNewTests") {
            collectedTestResultsFile = collectTestResults.flatMap { it.collectedTestResultsFile }
            discoveredNewTestsFile = rootProject.layout.buildDirectory.file("$PATH_PREFIX/discoveredNewTests.json")

            rootProject.allprojects {
                val sourceSets = extensions.findByType<SourceSetContainer>()
                val testSourceSet = sourceSets?.find { it.name in listOf("test", "jvmTest") }

                if (testSourceSet != null) {
                    testSourceSetOutput.from(testSourceSet.output)
                    testRuntimeClasspath.from(testSourceSet.runtimeClasspath)
                }
            }
        }

    private fun registerGroupTestsIntoBatchesTask(
        rootProject: Project,
        collectTestResults: TaskProvider<CollectTestResults>,
        discoverNewTests: TaskProvider<DiscoverNewTests>
    ) =
        rootProject.tasks.register<GroupTestsIntoBatches>("groupTestsIntoBatches") {
            collectedTestResultsFile = collectTestResults.flatMap { it.collectedTestResultsFile }
            discoveredNewTestsFile = discoverNewTests.flatMap { it.discoveredNewTestsFile }
            numberOfBatches = System.getenv("NUMBER_OF_BATCHES")?.toInt() ?: 1
            batchesOutputDir = rootProject.layout.buildDirectory.dir("$PATH_PREFIX/test-batches")
        }

    private fun registerPrintTestPlanTask(
        rootProject: Project,
        collectTestResults: TaskProvider<CollectTestResults>,
        groupTestsIntoBatches: TaskProvider<GroupTestsIntoBatches>
    ) =
        rootProject.tasks.register<PrintTestPlan>("printTestPlan") {
            collectedTestResultsFile = collectTestResults.flatMap { it.collectedTestResultsFile }
            batchesDir = groupTestsIntoBatches.flatMap { it.batchesOutputDir }
            batchNumber = System.getenv("BATCH_NUMBER")?.toInt() ?: 1
        }

    private fun registerPrepareExcludeTestPatternsTask(
        rootProject: Project,
        groupTestsIntoBatches: TaskProvider<GroupTestsIntoBatches>,
        printTestPlan: TaskProvider<PrintTestPlan>
    ) =
        rootProject.tasks.register<PrepareExcludeTestPatterns>("prepareExcludeTestPatterns") {
            batchesDir = groupTestsIntoBatches.flatMap { it.batchesOutputDir }
            batchNumber = System.getenv("BATCH_NUMBER")?.toInt() ?: 1
            excludePatternsFile = rootProject.layout.buildDirectory.file("$PATH_PREFIX/testExcludePatterns.txt")
            finalizedBy(printTestPlan)
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

    private companion object {
        const val PATH_PREFIX = "distributed-kotest"
    }
}