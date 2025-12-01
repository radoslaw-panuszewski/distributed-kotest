package dev.panuszewski.distributedkotest.gradle.framework

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.nio.file.Paths

abstract class GradleSpec {
    val rootProjectName = "test-project"

    /**
     * Gradle with the given version will be used in [GradleRunner]
     */
    var gradleVersion: GradleVersion = GradleVersion.current()
    val buildEnvironment = mutableMapOf<String, String>()

    lateinit var gradleBuild: GradleBuild

    var configurationCacheEnabled = true
    var buildCacheEnabled = true

    /**
     * Override, append or prepend content of `build.gradle.kts`
     */
    fun buildGradleKts(configurator: AppendableFile.() -> Any) {
        gradleBuild.buildGradleKts(configurator)
    }

    /**
     * Override, append or prepend content of `<subprojectName>/build.gradle.kts` and include the subproject in the build
     */
    fun subprojectBuildGradleKts(subprojectName: String, configurator: AppendableFile.() -> Any) {
        gradleBuild.subprojectBuildGradleKts(subprojectName, configurator)
    }

    /**
     * Override, append or prepend content of `settings.gradle.kts`
     */
    fun settingsGradleKts(configurator: AppendableFile.() -> Any) {
        gradleBuild.settingsGradleKts(configurator)
    }

    /**
     * Override, append or prepend content of `gradle/libs.versions.toml`
     */
    fun libsVersionsToml(configurator: AppendableFile.() -> Any) {
        gradleBuild.libsVersionsToml(configurator)
    }

    /**
     * Override, append or prepend content of a custom file under [path]
     */
    fun customProjectFile(path: String, configurator: AppendableFile.() -> Any) {
        gradleBuild.customProjectFile(path, configurator)
    }

    /**
     * Execute Gradle build in the temporary directory
     */
    protected fun runGradle(
        vararg arguments: String,
        customizer: GradleRunner.() -> Unit = {},
    ): SuccessOrFailureBuildResult =
        try {
            val args = buildList {
                addAll(arguments)
                add("--stacktrace")
                if (configurationCacheEnabled) add("--configuration-cache")
                if (buildCacheEnabled) add("--build-cache")
            }

            dumpBuildEnvironment()

            GradleRunner.create()
                .withProjectDir(gradleBuild.rootDir)
                .withPluginClasspath()
                .forwardOutput()
                .withGradleVersion(gradleVersion.version)
                .withEnvironment(System.getenv() + buildEnvironment)
                .withArguments(args)
                .apply(customizer)
                .build()
                .let { SuccessOrFailureBuildResult(it, BuildOutcome.BUILD_SUCCESSFUL) }
        } catch (e: UnexpectedBuildFailure) {
            SuccessOrFailureBuildResult(e.buildResult, BuildOutcome.BUILD_FAILED)
        }

    private fun dumpBuildEnvironment() {
        val envDump = buildEnvironment.entries
            .joinToString(separator = "\n") { (name, value) -> "systemProp.$name=${value.replace("\n", " \\n\\\n")}" }

        gradleBuild.rootDir.resolveOrCreate("gradle.properties").writeText(envDump)
    }

    @BeforeEach
    fun beforeEach(testInfo: TestInfo) {
        val testProjectDir = Paths.get(".test-projects")
            .resolve(gradleVersion.version)
            .resolve(testInfo.testMethod.get().name.replace(" ", "-"))
            .toAbsolutePath().toFile()

        gradleBuild = GradleBuild(
            rootProjectName = rootProjectName,
            rootDir = testProjectDir,
            gradleVersion = gradleVersion
        )
    }
}

class SuccessOrFailureBuildResult(
    private val delegate: BuildResult,
    val buildOutcome: BuildOutcome,
) : BuildResult by delegate

enum class BuildOutcome {
    BUILD_SUCCESSFUL,
    BUILD_FAILED,
}

