package dev.panuszewski.distributedkotest.gradle.newtests

import com.fasterxml.jackson.module.kotlin.readValue
import dev.panuszewski.distributedkotest.gradle.TestResult
import dev.panuszewski.distributedkotest.gradle.util.objectMapper
import dev.panuszewski.distributedkotest.gradle.util.objectWriter
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.NAME_ONLY
import org.gradle.api.tasks.TaskAction
import java.net.URLClassLoader
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.primaryConstructor
import kotlin.time.Duration

@CacheableTask
public abstract class DiscoverNewTests : DefaultTask() {

    @InputFile
    @PathSensitive(NAME_ONLY)
    public val collectedTestResultsFile: RegularFileProperty = project.objects.fileProperty()

    @InputFiles
    @Classpath
    public val testSourceSetOutput: ConfigurableFileCollection = project.objects.fileCollection()

    @InputFiles
    @Classpath
    public val testRuntimeClasspath: ConfigurableFileCollection = project.objects.fileCollection()

    @OutputFile
    public val discoveredNewTestsFile: RegularFileProperty = project.objects.fileProperty()

    @TaskAction
    public fun execute() {
        val collectedTestResults = readCollectedTestResults()
        val classes = fullyQualifiedNamesFromClassFiles()
        val discoveredTestClasses = discoverTestClasses(classes)
        val newTests = filterNewTests(discoveredTestClasses, collectedTestResults)
        val newTestsJson = objectWriter.writeValueAsString(newTests)
        discoveredNewTestsFile.get().asFile.writeText(newTestsJson)
    }

    private fun readCollectedTestResults(): List<TestResult> =
        objectMapper.readValue<List<TestResult>>(collectedTestResultsFile.get().asFile)

    private fun fullyQualifiedNamesFromClassFiles(): List<String> =
        testSourceSetOutput.files.flatMap { parentPath ->
            parentPath.walk()
                .filter { it.isFile && !it.name.contains("$") && it.name.endsWith(".class") }
                .map { it.relativeTo(parentPath) }
                .map {
                    it.path
                        .removeSuffix(".class")
                        .replace('/', '.')
                        .replace('\\', '.')
                }
        }

    private fun discoverTestClasses(classes: List<String>): List<String> {
        val urls = (testSourceSetOutput + testRuntimeClasspath).map { it.toURI().toURL() }.toTypedArray() + javaClass.protectionDomain.codeSource.location
        val customClassLoader = URLClassLoader(urls)
        val discoveryInvokerClass = customClassLoader.loadClass(DiscoveryInvoker::class.qualifiedName).kotlin
        val discoveryInvokerConstructor = discoveryInvokerClass.constructors.first()
        val discoveryInvokerInstance = discoveryInvokerConstructor.call()
        val discoverMethod = discoveryInvokerClass.declaredFunctions.find { it.name == "discover" }
        val discoveredTestClasses = discoverMethod?.call(discoveryInvokerInstance, classes) as List<String>
        return discoveredTestClasses
    }

    private fun filterNewTests(discoveredTestClasses: List<String>, testResults: List<TestResult>): List<TestResult> =
        discoveredTestClasses
            .filter { discoveredTestClass -> testResults.none { it.classname == discoveredTestClass } }
            .map {
                TestResult(
                    name = "<new test>",
                    classname = it,
                    result = "successful",
                    duration = Duration.ZERO
                )
            }
}
