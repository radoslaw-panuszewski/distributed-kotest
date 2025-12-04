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
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.NAME_ONLY
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.gradle.work.DisableCachingByDefault

@CacheableTask
public abstract class PrepareExcludeTestPatterns : DefaultTask() {

    @InputDirectory
    @PathSensitive(NAME_ONLY)
    public val batchesDir: DirectoryProperty = project.objects.directoryProperty()

    @Input
    public val batchNumber: Property<Int> = project.objects.property<Int>()

    @OutputFile
    public val excludePatternsFile: RegularFileProperty = project.objects.fileProperty()

    @TaskAction
    public fun execute() {
        val batches = readTestBatches()
        val testsToExclude = findTestsToExclude(batches)
        writeExcludesFile(testsToExclude)
    }

    private fun readTestBatches(): List<TestBatch> =
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
