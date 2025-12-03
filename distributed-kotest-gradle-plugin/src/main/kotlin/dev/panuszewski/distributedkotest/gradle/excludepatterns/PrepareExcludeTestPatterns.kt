package dev.panuszewski.distributedkotest.gradle.excludepatterns

import com.fasterxml.jackson.module.kotlin.readValue
import dev.panuszewski.distributedkotest.gradle.TestBatch
import dev.panuszewski.distributedkotest.gradle.TestResult
import dev.panuszewski.distributedkotest.gradle.util.objectMapper
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.NAME_ONLY
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
public abstract class PrepareExcludeTestPatterns : DefaultTask() {

    @Input
    public val batchNumber: Property<Int> = project.objects.property<Int>()

    @InputDirectory
    @PathSensitive(NAME_ONLY)
    public val batchesDir: DirectoryProperty = project.objects.directoryProperty()

    @OutputFile
    public val excludePatternsFile: RegularFileProperty = project.objects.fileProperty()

    @TaskAction
    public fun execute() {
        val batches = parseTestBatches()
        val testsToExclude = findTestsToExclude(batches)
        writeExcludesFile(testsToExclude)
        logSummaryMessage(batches, testsToExclude)
    }

    private fun parseTestBatches(): List<TestBatch> =
        batchesDir.get()
            .asFile
            .listFiles()
            .map { objectMapper.readValue<TestBatch>(it) }

    private fun findTestsToExclude(batches: List<TestBatch>): List<TestResult> =
        batches
            .filterNot { it.number == batchNumber.get() }
            .flatMap(TestBatch::tests)

    private fun writeExcludesFile(testsToExclude: List<TestResult>) {
        val content = testsToExclude
            .map { "${it.classname.topLevelClassFqn()}*" }
            .distinct()
            .joinToString("\n")
        excludePatternsFile.get().asFile.writeText(content)
    }

    private fun logSummaryMessage(batches: List<TestBatch>, testsToExclude: List<TestResult>) {
        val allTestResults = batches.flatMap(TestBatch::tests)
        val currentBatch = batches.find { it.number == batchNumber.get() }

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

        if (testsToExclude.isNotEmpty() && currentBatch != null) {
            val newTestsCount = currentBatch.tests.count { it.name == "<new test>" }
            val oldTestsCount = currentBatch.tests.size - newTestsCount
            logger.lifecycle(buildString {
                append("Running $oldTestsCount tests from batch ${batchNumber.get()}")
                if (newTestsCount > 0) {
                    append(" + up to $newTestsCount new tests (some of them may be ignored at runtime)")
                }
            })
        } else {
            logger.lifecycle("Running all tests")
        }
    }
}

private fun String.topLevelClassFqn(): String {
    val parts = split(".")
    val newParts = buildList {
        var uppercasePartEncountered = false
        for (part in parts) {
            if (part.first().isUpperCase()) {
                if (uppercasePartEncountered) {
                    break
                }
                uppercasePartEncountered = true
            }
            add(part)
        }
    }
    return newParts.joinToString(".")
}
