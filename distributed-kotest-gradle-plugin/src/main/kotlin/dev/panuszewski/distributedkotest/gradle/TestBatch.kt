package dev.panuszewski.distributedkotest.gradle

import kotlin.time.Duration

internal data class TestBatch(
    val number: Int,
    val tests: List<TestResult>,
    val totalDuration: Duration
)
