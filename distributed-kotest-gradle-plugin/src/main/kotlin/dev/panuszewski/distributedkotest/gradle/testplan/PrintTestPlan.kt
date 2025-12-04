package dev.panuszewski.distributedkotest.gradle.testplan

import com.fasterxml.jackson.module.kotlin.readValue
import dev.panuszewski.distributedkotest.gradle.TestBatch
import dev.panuszewski.distributedkotest.gradle.TestResult
import dev.panuszewski.distributedkotest.gradle.util.objectMapper
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.NAME_ONLY
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.gradle.work.DisableCachingByDefault
import kotlin.collections.forEach

@DisableCachingByDefault
public abstract class PrintTestPlan : DefaultTask() {

    @InputFile
    @PathSensitive(NAME_ONLY)
    public val collectedTestResultsFile: RegularFileProperty = project.objects.fileProperty()

    @InputDirectory
    @PathSensitive(NAME_ONLY)
    public val batchesDir: DirectoryProperty = project.objects.directoryProperty()

    @Input
    public val batchNumber: Property<Int> = project.objects.property<Int>()

    @TaskAction
    public fun execute() {
        val collectedTestResults = readCollectedTestResults()
        val batches = readTestBatches()
        printGroupingSummary(collectedTestResults, batches)
        printExecutionPlan(batches)
    }

    private fun readCollectedTestResults(): List<TestResult> =
        objectMapper.readValue<List<TestResult>>(collectedTestResultsFile.get().asFile)

    private fun readTestBatches(): List<TestBatch> =
        batchesDir.get()
            .asFile
            .listFiles()
            .map { objectMapper.readValue<TestBatch>(it) }
            .sortedBy(TestBatch::number)

    private fun printGroupingSummary(collectedTestResults: List<TestResult>, batches: List<TestBatch>) {
        if (collectedTestResults.isNotEmpty()) {
            val message = buildString {
                appendLine("Found ${collectedTestResults.size} tests")
                appendLine("Grouped them into batches:")
                batches.forEach {
                    append("${it.number}. tests = ${it.oldTestsCount}")
                    append(", total duration = ${it.totalDuration}")
                    if (it.newTestsCount > 0) {
                        append(" (+ ${it.newTestsCount} new or ignored test classes)")
                    }
                    appendLine()
                }
            }
            logger.lifecycle(message)
        }
    }

    private fun printExecutionPlan(batches: List<TestBatch>) {
        val currentBatch = batches.find { it.number == batchNumber.get() }

        if (currentBatch != null && currentBatch.tests.isNotEmpty()) {
            val message = buildString {
                append("Batch ${batchNumber.get()}: running ${currentBatch.oldTestsCount} tests")
                if (currentBatch.newTestsCount > 0) {
                    append(" + ${currentBatch.newTestsCount} potentially new test classes (some of them may be ignored at runtime)")
                }
            }
            logger.lifecycle(message)
        } else {
            logger.lifecycle("Running all tests")
        }
    }
}