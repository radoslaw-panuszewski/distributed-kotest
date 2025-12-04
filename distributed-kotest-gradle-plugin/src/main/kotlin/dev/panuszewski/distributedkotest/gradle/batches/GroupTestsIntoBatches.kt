package dev.panuszewski.distributedkotest.gradle.batches

import com.fasterxml.jackson.module.kotlin.readValue
import dev.panuszewski.distributedkotest.gradle.TestBatch
import dev.panuszewski.distributedkotest.gradle.TestResult
import dev.panuszewski.distributedkotest.gradle.util.objectMapper
import dev.panuszewski.distributedkotest.gradle.util.objectWriter
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.NAME_ONLY
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property

@CacheableTask
public abstract class GroupTestsIntoBatches : DefaultTask() {

    @InputFile
    @PathSensitive(NAME_ONLY)
    public val collectedTestResultsFile: RegularFileProperty = project.objects.fileProperty()

    @InputFile
    @PathSensitive(NAME_ONLY)
    public val discoveredNewTestsFile: RegularFileProperty = project.objects.fileProperty()

    @Input
    public val numberOfBatches: Property<Int> = project.objects.property<Int>()

    @OutputDirectory
    public val batchesOutputDir: DirectoryProperty = project.objects.directoryProperty()

    @TaskAction
    public fun execute() {
        val testResults = readCollectedTestResults()
        val newTests = readDiscoveredNewTests()
        val batches = TestGrouper.groupIntoBatches(numberOfBatches.get(), testResults, newTests)
        writeBatchesToOutputDir(batches)
    }

    private fun readCollectedTestResults(): List<TestResult> =
        objectMapper.readValue<List<TestResult>>(collectedTestResultsFile.get().asFile)

    private fun readDiscoveredNewTests(): List<TestResult> =
        objectMapper.readValue<List<TestResult>>(discoveredNewTestsFile.get().asFile)

    private fun writeBatchesToOutputDir(batches: List<TestBatch>) {
        val outputDir = batchesOutputDir.get().asFile

        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
        outputDir.mkdirs()

        batches.forEach { batch ->
            val batchFile = outputDir.resolve("batch-${batch.number}.json")
            batchFile.createNewFile()
            batchFile.writeText(objectWriter.writeValueAsString(batch))
        }
    }
}
