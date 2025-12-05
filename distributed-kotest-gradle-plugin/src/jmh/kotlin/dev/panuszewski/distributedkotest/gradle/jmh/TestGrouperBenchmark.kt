package dev.panuszewski.distributedkotest.gradle.jmh

import dev.panuszewski.distributedkotest.gradle.TestResult
import dev.panuszewski.distributedkotest.gradle.batches.TestGrouper
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode.AverageTime
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope.Benchmark
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.time.Duration.Companion.milliseconds

@State(Benchmark)
@BenchmarkMode(AverageTime)
@OutputTimeUnit(MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = SECONDS)
open class TestGrouperBenchmark {

    @Param("1000000")
    var testCount: Int = 0

    @Param("10")
    var batchCount: Int = 0

    private lateinit var testResults: List<TestResult>

    @Setup
    fun setup() {
        testResults = generateRealisticTestResults(testCount)
    }

    @Benchmark
    fun benchmarkLargeScaleGrouping(): Any {
        return TestGrouper.groupIntoBatches(batchCount, testResults)
    }

    private fun generateRealisticTestResults(count: Int): List<TestResult> {
        val basePackages = listOf("com.example", "org.something", "io.company")
        val modules = listOf("core", "api", "service", "web", "data")
        val testTypes = listOf("unit", "integration", "e2e")
        val classNames = listOf(
            "SomeSpec", "SomeOtherSpec", "SomeOtherOtherSpec", "SomeSpecIntegration",
            "ApiTestSpec",
            "DataSpec",
            "FinalSpec",
            "IntegrationTestSpec",
            "MediumSpec",
            "MySpecBar", "MySpecBaz", "MySpecFoo",
            "NewSpec",
            "PerformanceTestSpec",
            "QuickSpec",
            "RestSpecOne", "RestSpecTwo",
            "ServiceSpecAlpha", "ServiceSpecBeta", "ServiceSpecGamma",
            "SlowSpec",
            "TestSpecA", "TestSpecB", "TestSpec",
        )

        return (1..count).map { i ->
            val basePackage = basePackages[i % basePackages.size]
            val module = modules[i % modules.size]
            val testType = testTypes[i % testTypes.size]
            val className = classNames[i % classNames.size]
            val fullClassName = "$basePackage.$module.$testType.$className"

            val baseDuration = when (testType) {
                "unit" -> 50
                "integration" -> 200
                "e2e" -> 1000
                else -> 100
            }

            TestResult(
                name = "test $i",
                classname = fullClassName,
                result = "passed",
                duration = (baseDuration + (i * 17) % 500).milliseconds
            )
        }
    }
}
