package dev.panuszewski.distributedkotest.gradle.batches

import dev.panuszewski.distributedkotest.gradle.TestBatch
import dev.panuszewski.distributedkotest.gradle.TestResult
import dev.panuszewski.distributedkotest.gradle.util.objectMapper
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import kotlin.sequences.flatMap

@CacheableTask
public abstract class GroupTestsIntoBatches : DefaultTask() {

    @Input
    public val numberOfBatches: Property<Int> = project.objects.property<Int>()

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public val testResultsDir: DirectoryProperty = project.objects.directoryProperty()

    @OutputDirectory
    public val batchesOutputDir: DirectoryProperty = project.objects.directoryProperty()

    @TaskAction
    public fun execute() {
        val testResults = collectTestResults()
        val batches = TestGrouper.groupIntoBatches(numberOfBatches.get(), testResults)
        writeBatchesToOutputDir(batches)
        logSummaryMessage(testResults, batches)
    }

    private fun collectTestResults(): List<TestResult> =
        testResultsDir.get().asFile
            .walk()
            .filter { it.name.endsWith(".xml") }
            .flatMap(TestParser::parseTestResultsXmlFile)
            .toList()

    private fun writeBatchesToOutputDir(batches: List<TestBatch>) {
        val outputDir = batchesOutputDir.get().asFile

        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
        outputDir.mkdirs()

        batches.forEach { batch ->
            val batchFile = outputDir.resolve("batch-${batch.number}.json")
            batchFile.createNewFile()
            batchFile.writeText(objectMapper.writeValueAsString(batch))
        }
    }

    private fun logSummaryMessage(allTestResults: List<TestResult>, batches: List<TestBatch>) {
        if (allTestResults.isNotEmpty()) {
            val message = buildString {
                appendLine("Found ${allTestResults.size} tests")
                appendLine("Grouped them into batches:")
                batches.forEach {
                    appendLine("${it.number}. tests = ${it.tests.size}, total duration = ${it.totalDuration}")
                }
            }
            logger.lifecycle(message)
        }
    }
}