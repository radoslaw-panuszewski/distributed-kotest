package dev.panuszewski.distributedkotest.gradle.framework

import java.io.File

class AppendableFile(
    private val file: File,
    private val tailContent: String? = null,
) {
    init {
        appendTailContent()
    }

    fun acceptConfigurator(configurator: AppendableFile.() -> Any): AppendableFile {
        val maybeNewContent = configurator()

        if (maybeNewContent is String) {
            setContent { maybeNewContent }
        }
        return this
    }

    private fun setContent(content: () -> String) {
        file.writeText(content().trimIndent().trimStart())
        appendTailContent()
    }

    private fun appendTailContent() {
        tailContent?.let { append { it } }
    }

    fun prepend(content: () -> String) {
        val previousContent = file.readText()
        val separator = if (previousContent.isNotBlank()) "\n\n" else ""
        file.writeText(content().trimIndent().trimStart() + separator + previousContent)
    }

    fun append(content: () -> String) {
        val previousContent = file.readText()
        val separator = if (previousContent.isNotBlank()) "\n\n" else ""
        file.writeText(previousContent + separator + content().trimIndent().trimStart())
    }

    fun appendLine(content: () -> String) {
        append(content)
        append { "\n" }
    }

    fun prependLine(content: () -> String) {
        prepend(content)
        prepend { "\n" }
    }
}