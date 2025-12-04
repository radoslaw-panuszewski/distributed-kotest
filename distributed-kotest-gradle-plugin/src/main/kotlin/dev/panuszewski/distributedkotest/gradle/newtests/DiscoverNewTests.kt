package dev.panuszewski.distributedkotest.gradle.newtests

import com.fasterxml.jackson.module.kotlin.readValue
import dev.panuszewski.distributedkotest.gradle.TestResult
import dev.panuszewski.distributedkotest.gradle.util.objectMapper
import io.kotest.runner.junit.platform.discovery.Discovery
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
import org.junit.platform.engine.ConfigurationParameters
import org.junit.platform.engine.DiscoveryFilter
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.DiscoverySelectors
import java.net.URLClassLoader
import java.util.Optional
import kotlin.collections.plus
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
        val newTestsJson = objectMapper.writeValueAsString(newTests)
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
        val discoveryInvokerInstance = discoveryInvokerClass.primaryConstructor?.call()
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

internal class DiscoveryInvoker {
    fun discover(classes: List<String>): List<String> {
        val selectors = classes.map { DiscoverySelectors.selectClass(it) }
        val discoveryRequest = ClassSelectingDiscoveryRequest(selectors)
        val discoveryResult = Discovery.discover(UniqueId.forEngine("kotest"), discoveryRequest)
        return discoveryResult.specs.mapNotNull { it.qualifiedName }
    }
}

internal class ClassSelectingDiscoveryRequest(
    private val selectors: List<ClassSelector>,
) : EngineDiscoveryRequest {

    override fun <T : DiscoverySelector> getSelectorsByType(selectorType: Class<T>): List<T> =
        when (selectorType) {
            ClassSelector::class.java -> selectors as List<T>
            else -> emptyList()
        }

    override fun <T : DiscoveryFilter<*>?> getFiltersByType(filterType: Class<T>): List<T> = emptyList()

    override fun getConfigurationParameters() = EmptyConfigurationParameters
}

internal object EmptyConfigurationParameters : ConfigurationParameters {
    override fun get(key: String?): Optional<String> = Optional.empty()
    override fun getBoolean(key: String?): Optional<Boolean> = Optional.empty()
    override fun size(): Int = 0
    override fun keySet(): Set<String> = emptySet()
}