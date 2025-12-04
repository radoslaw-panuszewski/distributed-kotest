package dev.panuszewski.distributedkotest.gradle.newtests

import io.kotest.runner.junit.platform.discovery.Discovery
import org.junit.platform.engine.ConfigurationParameters
import org.junit.platform.engine.DiscoveryFilter
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.discovery.ClassSelector
import org.junit.platform.engine.discovery.DiscoverySelectors
import java.util.Optional

internal class DiscoveryInvoker {
    fun discover(classes: List<String>): List<String> {
        val selectors = classes.map { DiscoverySelectors.selectClass(it) }
        val discoveryRequest = ClassSelectingDiscoveryRequest(selectors)
        val discoveryResult = Discovery.discover(UniqueId.forEngine("kotest"), discoveryRequest)
        return discoveryResult.specs.mapNotNull { it.qualifiedName }
    }
}

private class ClassSelectingDiscoveryRequest(
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

private object EmptyConfigurationParameters : ConfigurationParameters {
    override fun get(key: String?): Optional<String> = Optional.empty()
    override fun getBoolean(key: String?): Optional<Boolean> = Optional.empty()
    override fun size(): Int = 0
    override fun keySet(): Set<String> = emptySet()
}