package dev.panuszewski.distributedkotest.gradle

import java.io.Serializable
import kotlin.time.Duration

internal data class TestResult(
    val name: String,
    val classname: String,
    val result: String,
    val duration: Duration,
) : Serializable {

    override fun toString() = name
}
