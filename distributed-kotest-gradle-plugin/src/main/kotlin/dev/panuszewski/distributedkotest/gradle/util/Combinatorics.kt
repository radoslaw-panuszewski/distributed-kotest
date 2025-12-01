package dev.panuszewski.distributedkotest.gradle.util

import org.apache.commons.numbers.combinatorics.Combinations

internal fun <T> List<T>.combinations(k: Int): List<Pair<T, T>> =
    Combinations.of(size, k)
        .map { Pair(get(it[0]), get(it[1])) }