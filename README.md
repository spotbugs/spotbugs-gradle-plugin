# SpotBugs Gradle Plugin

This is the official Gradle Plugin to run SpotBugs on Java and Android project.

![](https://github.com/spotbugs/spotbugs-gradle-plugin/workflows/Java%20CI/badge.svg)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=com.github.spotbugs.gradle&metric=coverage)](https://sonarcloud.io/component_measures?id=com.github.spotbugs.gradle&metric=coverage)
[![Debt](https://sonarcloud.io/api/project_badges/measure?project=com.github.spotbugs.gradle&metric=sqale_index)](https://sonarcloud.io/component_measures/domain/Maintainability?id=com.github.spotbugs.gradle)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v?label=Plugin+Portal&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Fcom%2Fgithub%2Fspotbugs%2Fcom.github.spotbugs.gradle.plugin%2Fmaven-metadata.xml)](https://plugins.gradle.org/plugin/com.github.spotbugs)
[![](https://img.shields.io/badge/groovydoc-latest-blightgreen?logo=groovy)](https://spotbugs.github.io/spotbugs-gradle-plugin/spotbugs-gradle-plugin/com.github.spotbugs.snom/index.html)

## Goal

This Gradle plugin is designed to solve the following problems in the legacy plugin:

- [x] Remove any dependency on the Gradle's internal API
- [x] Solve mutability problem for the build contains multiple projects and/or sourceSet
- [x] Native Support for [the Parallel Build](https://guides.gradle.org/using-the-worker-api/)
- [ ] Native Support for [the Android project](https://developer.android.com/studio/build/gradle-tips)
- [x] Missing user document about how to use extension and task

## Usage

### Apply to your project

Apply the plugin to your project.
Refer [the Gradle Plugin portal](https://plugins.gradle.org/plugin/com.github.spotbugs) about the detail of installation procedure.

### Configure SpotBugs Plugin

Configure `spotbugs` extension to configure the behaviour of tasks:

```kotlin
// require Gradle 8.2+
import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
spotbugs {
    ignoreFailures = false
    showStackTraces = true
    showProgress = true
    effort = Effort.DEFAULT
    reportLevel = Confidence.DEFAULT
    visitors = listOf("FindSqlInjection", "SwitchFallthrough")
    omitVisitors = listOf("FindNonShortCircuit")
    chooseVisitors = listOf("-FindNonShortCircuit", "+TestASM")
    reportsDir = file("$buildDir/spotbugs")
    includeFilter = file("include.xml")
    excludeFilter = file("exclude.xml")
    baselineFile = file("baseline.xml")
    onlyAnalyze = listOf("com.foobar.MyClass", "com.foobar.mypkg.*")
    maxHeapSize = "1g"
    extraArgs = listOf("-nested:false")
    jvmArgs = listOf("-Duser.language=ja")
}
```

<details>
<summary>with Groovy DSL</summary>

```groovy
import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
spotbugs {
    ignoreFailures = false
    showStackTraces = true
    showProgress = true

    // https://discuss.kotlinlang.org/t/bug-cannot-use-kotlin-enum-from-groovy/1521
    // https://touk.pl/blog/2018/05/28/testing-kotlin-with-spock-part-2-enum-with-instance-method/
    effort = Effort.valueOf('DEFAULT')
    reportLevel = Confidence.valueOf('DEFAULT')

    visitors = [ 'FindSqlInjection', 'SwitchFallthrough' ]
    omitVisitors = [ 'FindNonShortCircuit' ]
    chooseVisitors = [ '-FindNonShortCircuit', '+TestASM' ]
    reportsDir = file("$buildDir/spotbugs")
    includeFilter = file("include.xml")
    excludeFilter = file("exclude.xml")
    baselineFile = file("baseline.xml")
    onlyAnalyze = [ 'com.foobar.MyClass', 'com.foobar.mypkg.*' ]
    maxHeapSize = '1g'
    extraArgs = [ '-nested:false' ]
    jvmArgs = [ '-Duser.language=ja' ]
}
```
</details>

Configure `spotbugsPlugin` to apply any SpotBugs plugin:

```kotlin
dependencies {
    spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.14.0")
}
```

<details>
<summary>with Groovy DSL</summary>

```groovy
dependencies {
    spotbugsPlugins 'com.h3xstream.findsecbugs:findsecbugs-plugin:1.14.0'
}
```
</details>

Configure `spotbugs` to choose your favorite SpotBugs version:

```kotlin
dependencies {
    spotbugs("com.github.spotbugs:spotbugs:4.9.3")
}
```


<details>
<summary>with Groovy DSL</summary>

```groovy
dependencies {
    spotbugs 'com.github.spotbugs:spotbugs:4.9.3'
}
```
</details>

### Apply to Java project

Apply this plugin with [the `java` plugin](https://docs.gradle.org/current/userguide/java_plugin.html) to your project,
then [`SpotBugsTask`](https://spotbugs.github.io/spotbugs-gradle-plugin/spotbugs-gradle-plugin/com.github.spotbugs.snom/-spot-bugs-task/index.html) will be generated for each existing sourceSet.

If you want to create and configure `SpotBugsTask` by own, apply the base plugin (`com.github.spotbugs-base`) instead, then it won't create tasks automatically.

### Apply to Android project

TBU

### Configure the SpotBugsTask

Configure [`SpotBugsTask`](https://spotbugs.github.io/spotbugs-gradle-plugin/spotbugs-gradle-plugin/com.github.spotbugs.snom/-spot-bugs-task/index.html) directly,
to set task-specific properties.

```kotlin
// require Gradle 8.2+
tasks.spotbugsMain {
    reports.create("html") {
        required = true
        outputLocation = file("$buildDir/reports/spotbugs.html")
        setStylesheet("fancy-hist.xsl")
    }
}
```

<details>
<summary>with Groovy DSL</summary>
    
```groovy
// Example to configure HTML report
spotbugsMain {
    reports {
        html {
            required = true
            outputLocation = file("$buildDir/reports/spotbugs/main/spotbugs.html")
            stylesheet = 'fancy-hist.xsl'
        }
    }
}
```
</details>

### Migration guides

- [v4 to v5: Bump up Gradle to v7 or later](https://github.com/spotbugs/spotbugs-gradle-plugin/releases/tag/5.0.0)
- [v5 to v6: Bump up Gradle to v7.1 or later, and update the `effort` and `reportLevel` properties of `SpotBugsTask` and `SpotBugsExtension` to enum value](https://github.com/spotbugs/spotbugs-gradle-plugin/releases/tag/6.0.0)

## SpotBugs version mapping

By default, this Gradle Plugin uses the SpotBugs version listed in the following table.

You can change SpotBugs version by [the `toolVersion` property of the spotbugs extension](https://spotbugs.github.io/spotbugs-gradle-plugin/spotbugs-gradle-plugin/com.github.spotbugs.snom/-spot-bugs-extension/index.html#674051637%2FProperties%2F769193423) or the `spotbugs` configuration.

| Gradle Plugin | SpotBugs |
|--------------:|---------:|
|        6.2.0  |    4.9.3 |
|        6.1.13 |    4.8.6 |
|        6.0.18 |    4.8.6 |
|        6.0.14 |    4.8.5 |
|        6.0.10 |    4.8.4 |
|         6.0.3 |    4.8.3 |
|         6.0.0 |    4.8.2 |
|         5.2.5 |    4.8.2 |
|         5.2.3 |    4.8.1 |
|         5.1.5 |    4.8.0 |
|        5.0.13 |    4.7.3 |
|        5.0.12 |    4.7.2 |
|         5.0.9 |    4.7.1 |
|         5.0.7 |    4.7.0 |
|         5.0.4 |    4.5.3 |
|         5.0.3 |    4.5.2 |
|         5.0.2 |    4.5.1 |

### Refer the version in the build script

From v4, the `spotbugs.toolVersion` is changed from `String` to [`Provider<String>`](https://docs.gradle.org/current/javadoc/org/gradle/api/provider/Property.html), so use `get()` or other methods to refer to the actual version.

```groovy
dependencies {
    compileOnly "com.github.spotbugs:spotbugs-annotations:${spotbugs.toolVersion.get()}"
}
```

<details>
<summary>with Kotlin DSL</summary>

```kotlin
dependencies {
    compileOnly("com.github.spotbugs:spotbugs-annotations:${spotbugs.toolVersion.get()}")
}
```
</details>

## Development
### Setup
* development requires java 17 or higher to be installed
* The CI server uses `ubuntu-latest` docker image, but you should be able to develop on any linux/unix based OS.
* before creating commits
  * read https://www.conventionalcommits.org/en
  * Optionally create the following script in your .git/hooks directory and name it commit.msg. This will ensure that your commits follow the conventional commits pattern.
```python
#!/usr/bin/env python
import re, sys, os

#turn off the traceback as it doesn't help readability
sys.tracebacklimit = 0

def main():
    # example:
    # feat(apikey): added the ability to add api key to configuration
    pattern = r'(build|ci|docs|feat|fix|perf|refactor|style|test|chore|revert)(\([\w\-]+\))?:\s.*'
    filename = sys.argv[1]
    ss = open(filename, 'r').read()
    m = re.match(pattern, ss)
    if m == None: raise Exception("Conventional commit validation failed. Did you forget to add one of the allowed prefixes? (build|ci|docs|feat|fix|perf|refactor|style|test|chore|revert)")

if __name__ == "__main__":
    main()
  ```
* when running gradle, do so using the `gradlew` script in this directory

### Signing Artifacts
Since version 4.3, when we publish artifacts we now sign them. This is designed so that the build will still pass if you don't have the signing keys available, this way pull requests and forked repos will still work as before.

Before github workflow can sign the artifacts generated during build, we first need to generate pgp keys (you will have to do this again when the key expires. once a year is a good timeframe) and upload them to the servers. See https://www.gnupg.org/faq/gnupg-faq.html#starting_out for more details.

That means github needs the following secrets:
```
SIGNING_KEY = "-----BEGIN PGP PRIVATE KEY BLOCK-----..."
SIGNING_PASSWORD = password
```
where `secrets.SIGNING_KEY` is the in-memory ascii-armored keys (you get this by running `gpg --armor --export-secret-keys <EMAIL>`)
and `secrets.SIGNING_PASSWORD` is the password you used when generating the key.

Gradle is configured to use these to generate the private key in memory so as to minimize our risk of the keys being found and used by someone else.

## Copyright

Copyright &copy; 2019-present SpotBugs Team
