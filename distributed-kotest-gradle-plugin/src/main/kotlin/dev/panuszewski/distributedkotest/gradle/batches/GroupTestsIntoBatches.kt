package dev.panuszewski.distributedkotest.gradle.batches

import dev.panuszewski.distributedkotest.gradle.TestBatch
import dev.panuszewski.distributedkotest.gradle.TestResult
import dev.panuszewski.distributedkotest.gradle.util.objectMapper
import io.kotest.runner.junit.platform.discovery.Discovery
import io.kotest.runner.junit.platform.discovery.DiscoveryResult
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.junit.platform.engine.ConfigurationParameters
import org.junit.platform.engine.DiscoveryFilter
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.DiscoverySelectors
import java.net.URLClassLoader
import java.util.Optional
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.primaryConstructor
import kotlin.sequences.flatMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@CacheableTask
public abstract class GroupTestsIntoBatches : DefaultTask() {

    @Input
    public val numberOfBatches: Property<Int> = project.objects.property<Int>()

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public val testResultsDir: DirectoryProperty = project.objects.directoryProperty()

    @OutputDirectory
    public val batchesOutputDir: DirectoryProperty = project.objects.directoryProperty()

    @InputFiles
    @Classpath
    public val testSourceSetOutput: ConfigurableFileCollection = project.objects.fileCollection()

    @InputFiles
    @Classpath
    public val testRuntimeClasspath: ConfigurableFileCollection = project.objects.fileCollection()

    @TaskAction
    public fun execute() {
        val classes = testSourceSetOutput.files
            .flatMap { parentPath ->
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

        val urls = (testSourceSetOutput + testRuntimeClasspath).map { it.toURI().toURL() }.toTypedArray() + javaClass.protectionDomain.codeSource.location
        val customClassLoader = URLClassLoader(urls)
        val discoveryInvokerClass = customClassLoader.loadClass("dev.panuszewski.distributedkotest.gradle.batches.DiscoveryInvoker").kotlin
        val discoveryInvokerInstance = discoveryInvokerClass.primaryConstructor?.call()
        val discoverMethod = discoveryInvokerClass.declaredFunctions.find { it.name == "discover" }
        val discoveredTestClasses = discoverMethod?.call(discoveryInvokerInstance, classes) as List<String>

        val testResults = collectTestResults()
        val newTests = discoveredTestClasses
            .filter { discoveredTestClass -> testResults.none { it.classname == discoveredTestClass } }
            .map {
                TestResult(
                    name = "<new test>",
                    classname = it,
                    result = "successful",
                    duration = Duration.ZERO
                )
            }

        val batches = TestGrouper.groupIntoBatches(numberOfBatches.get(), testResults, newTests)
        writeBatchesToOutputDir(batches)
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
}


public class DiscoveryInvoker {

    public fun discover(classes: List<String>): List<String> {
        val selectors = classes.map { DiscoverySelectors.selectClass(it) }

        val parameters = object : ConfigurationParameters {
            override fun get(key: String?): Optional<String> = Optional.empty()
            override fun getBoolean(key: String?): Optional<Boolean> = Optional.empty()
            override fun size(): Int = 0
            override fun keySet(): Set<String> = emptySet()
        }
        val request = object : EngineDiscoveryRequest {
            override fun <T : DiscoverySelector> getSelectorsByType(selectorType: Class<T>): List<T> =
                when (selectorType) {
                    ClassSelector::class.java -> selectors as List<T>
                    else -> emptyList()
                }

            override fun <T : DiscoveryFilter<*>?> getFiltersByType(filterType: Class<T>): List<T> = emptyList()
            override fun getConfigurationParameters(): ConfigurationParameters = parameters
        }

        val discoveryResult = Discovery.discover(UniqueId.forEngine("kotest"), request)
        return discoveryResult.specs.mapNotNull { it.qualifiedName }
    }
}