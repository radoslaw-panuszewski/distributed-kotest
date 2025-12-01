# distributed-kotest

A set of tools for distributing Kotest tests between multiple CI runners.

## Usage

### TeamCity

Add the following dependency to your `.teamcity/pom.xml`:
```xml
<dependency>
    <groupId>dev.panuszewski</groupId>
    <artifactId>distributed-kotest-teamcity</artifactId>
    <version>...</version>
</dependency>
```

If you want to use snapshot version, remember to add the Sonatype snapshots repository:
```xml
<repository>
    <id>sonatype-snapshots</id>
    <url>https://central.sonatype.com/repository/maven-snapshots</url>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```

Now you can use the `DistributedTests` build type in your `settings.kts`:
```kotlin
project {
    val distributedTests = DistributedTests(
        testTask = "test",
        numberOfBatches = 5,
        debugMode = true
    ) {
        // any customizations, like VCS references
        vcs { root(DslContext.settingsRoot) }
        triggers { vcs { } }
    }

    buildType(distributedTests)
}
```