package dev.panuszewski.distributedkotest.gradle.collecting

import dev.panuszewski.distributedkotest.gradle.TestResult
import dev.panuszewski.distributedkotest.gradle.util.objectMapper
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
public abstract class CollectTestResults : DefaultTask() {

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public val testResultsDir: DirectoryProperty = project.objects.directoryProperty()

    @OutputFile
    public val collectedTestResultsFile: RegularFileProperty = project.objects.fileProperty()

    @TaskAction
    public fun execute() {
        val testResults: List<TestResult> = testResultsDir.get().asFile
            .walk()
            .filter { it.name.endsWith(".xml") }
            .flatMap(TestXmlParser::parseTestResultsXml)
            .toList()

        val testResultsJson = objectMapper.writeValueAsString(testResults)

        collectedTestResultsFile.get().asFile.writeText(testResultsJson)
    }
}