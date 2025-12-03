package dev.panuszewski.distributedkotest.gradle.batches

import dev.panuszewski.distributedkotest.gradle.TestBatch
import dev.panuszewski.distributedkotest.gradle.TestResult
import dev.panuszewski.distributedkotest.gradle.util.combinations
import kotlin.time.Duration

internal object TestGrouper {

    fun groupIntoBatches(numberOfBatches: Int, testResults: List<TestResult>, newTests: List<TestResult>): List<TestBatch> {
        val batches: List<MutableTestBatch> = (1..numberOfBatches).map(::MutableTestBatch)

        val atomicGroupsFromSlowest = findAtomicGroups(testResults)
            .sortedByDescending(AtomicTestGroup::totalDuration)

        for (atomicGroup in atomicGroupsFromSlowest) {
            val smallestBatch = batches.minBy(MutableTestBatch::totalDuration)
            smallestBatch.addTests(atomicGroup.tests)
        }

        batches[0].addTests(newTests)

        return batches.map(MutableTestBatch::toTestBatch)
    }

    private fun findAtomicGroups(testResults: List<TestResult>): List<AtomicTestGroup> {
        val testClasses = groupTestsIntoClasses(testResults)

        val prefixes = findCommonPrefixes(testClasses)
        val normalizedPrefixes = normalizePrefixes(prefixes)

        val testClassesWithCommonPrefix = mergeClassesWithCommonPrefix(testClasses, normalizedPrefixes)
        val testClassesWithoutCommonPrefix = findClassesWithNoCommonPrefix(testClasses, normalizedPrefixes)

        return testClassesWithCommonPrefix + testClassesWithoutCommonPrefix
    }

    private fun groupTestsIntoClasses(testResults: List<TestResult>): List<AtomicTestGroup> =
        testResults
            .groupBy(TestResult::classname)
            .toSortedMap()
            .mapValues { (classname, tests) -> AtomicTestGroup(classname, tests) }
            .values
            .toList()

    private fun findCommonPrefixes(testClasses: List<AtomicTestGroup>): List<String> =
        testClasses
            .map { it.commonPrefix }
            .groupBy { it.packageName() }
            .toSortedMap()
            .filterValues { it.size >= 2 }
            .flatMap { (packageName, classes) ->
                classes
                    .map { it.className() }
                    .combinations(k = 2)
                    .map { (className, anotherClassName) -> className.commonPrefixWith(anotherClassName) }
                    .filter(String::isNotBlank)
                    .map { classNamePrefix -> "$packageName.$classNamePrefix" }
            }
            .distinct()

    private fun normalizePrefixes(prefixes: List<String>): List<String> =
        prefixes
            .filter { prefix ->
                prefixes
                    .filter { it != prefix }
                    .none { anotherPrefix -> prefix.startsWith(anotherPrefix) }
            }

    private fun mergeClassesWithCommonPrefix(testClasses: List<AtomicTestGroup>, prefixes: List<String>): List<AtomicTestGroup> =
        prefixes
            .map { prefix ->
                testClasses
                    .filter { testClass -> testClass.commonPrefix.startsWith(prefix) }
                    .reduce { testClass, anotherTestClass -> testClass.merge(prefix, anotherTestClass) }
            }

    private fun findClassesWithNoCommonPrefix(testClasses: List<AtomicTestGroup>, prefixes: List<String>): List<AtomicTestGroup> =
        testClasses
            .filter { testClass ->
                prefixes.none { prefix -> testClass.commonPrefix.startsWith(prefix) }
            }
}

private data class AtomicTestGroup(
    val commonPrefix: String,
    val tests: List<TestResult>
) {
    val totalDuration = tests.totalDuration()

    fun merge(commonPrefix: String, other: AtomicTestGroup) =
        AtomicTestGroup(
            commonPrefix = commonPrefix,
            tests = this.tests + other.tests
        )
}

private class MutableTestBatch(val number: Int) {
    private val tests = mutableListOf<TestResult>()
    val totalDuration get() = tests.totalDuration()

    fun addTests(test: List<TestResult>) {
        tests.addAll(test)
    }

    fun toTestBatch() = TestBatch(number, tests, totalDuration)
}

private fun List<TestResult>.totalDuration() =
    map(TestResult::duration).fold(Duration.ZERO, Duration::plus)

private fun String.packageName() = substringBeforeLast(".")
private fun String.className() = substringAfterLast(".")