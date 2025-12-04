package dev.panuszewski.distributedkotest.gradle

import com.fasterxml.jackson.annotation.JsonIgnore
import kotlin.time.Duration

internal data class TestBatch(
    val number: Int,
    val tests: List<TestResult>,
    val totalDuration: Duration
) {
    @get:JsonIgnore
    val allTestsCount: Int = tests.size

    @get:JsonIgnore
    val newTestsCount: Int = tests.count { it.name == "<new test>" }

    @get:JsonIgnore
    val oldTestsCount: Int = allTestsCount - newTestsCount
}
