# Newly Organized SpotBugs Gradle Plugin

This is the official Gradle Plugin to run SpotBugs on Java and Android project.

![](https://github.com/spotbugs/spotbugs-gradle-plugin/workflows/Java%20CI/badge.svg)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=com.github.spotbugs.gradle&metric=coverage)](https://sonarcloud.io/component_measures?id=com.github.spotbugs.gradle&metric=coverage)
[![Debt](https://sonarcloud.io/api/project_badges/measure?project=com.github.spotbugs.gradle&metric=sqale_index)](https://sonarcloud.io/component_measures/domain/Maintainability?id=com.github.spotbugs.gradle)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v?label=Plugin+Portal&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Fcom%2Fgithub%2Fspotbugs%2Fcom.github.spotbugs.gradle.plugin%2Fmaven-metadata.xml)](https://plugins.gradle.org/plugin/com.github.spotbugs)
[![](https://img.shields.io/badge/groovydoc-latest-blightgreen?logo=groovy)](https://spotbugs-gradle-plugin.netlify.com/com/github/spotbugs/snom/package-summary.html)
[![Issue Hunt](./.github/issuehunt-shield-v1.svg)](https://issuehunt.io/r/spotbugs/spotbugs-gradle-plugin)

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

```groovy
spotbugs {
    ignoreFailures = false
    showStackTraces = true
    showProgress = true
    effort = 'default'
    reportLevel = 'default'
    visitors = [ 'FindSqlInjection', 'SwitchFallthrough' ]
    omitVisitors = [ 'FindNonShortCircuit' ]
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

<details>
<summary>with Kotlin DSL</summary>

```kotlin
spotbugs {
    ignoreFailures.set(false)
    showStackTraces.set(true)
    showProgress.set(true)
    effort.set(com.github.spotbugs.snom.Effort.DEFAULT)
    reportLevel.set(com.github.spotbugs.snom.Confidence.DEFAULT)
    visitors.set(listOf("FindSqlInjection", "SwitchFallthrough"))
    omitVisitors.set(listOf("FindNonShortCircuit"))
    reportsDir.set(file("$buildDir/spotbugs"))
    includeFilter.set(file("include.xml"))
    excludeFilter.set(file("exclude.xml"))
    baselineFile.set(file("baseline.xml"))
    onlyAnalyze.set(listOf("com.foobar.MyClass", "com.foobar.mypkg.*"))
    maxHeapSize.set("1g")
    extraArgs.set(listOf("-nested:false"))
    jvmArgs.set(listOf("-Duser.language=ja"))
}
```
</details>

Configure `spotbugsPlugin` to apply any SpotBugs plugin:

```groovy
dependencies {
    spotbugsPlugins 'com.h3xstream.findsecbugs:findsecbugs-plugin:1.11.0'
}
```

<details>
<summary>with Kotlin DSL</summary>

```kotlin
dependencies {
    spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.11.0")
}
```
</details>

Configure `spotbugs` to choose your favorite SpotBugs version:

```groovy
dependencies {
    spotbugs 'com.github.spotbugs:spotbugs:4.4.2'
}
```

<details>
<summary>with Kotlin DSL</summary>

```kotlin
dependencies {
    spotbugs("com.github.spotbugs:spotbugs:4.4.2")
}
```
</details>

### Apply to Java project

Apply this plugin with [the `java` plugin](https://docs.gradle.org/current/userguide/java_plugin.html) to your project,
then [`SpotBugsTask`](https://spotbugs-gradle-plugin.netlify.com/com/github/spotbugs/snom/spotbugstask) will be generated for each existing sourceSet.

If you want to create and configure `SpotBugsTask` by own, apply the base plugin (`com.github.spotbugs-base`) instead, then it won't create tasks automatically.

### Apply to Android project

TBU

### Configure the SpotBugsTask

Configure [`SpotBugsTask`](https://spotbugs-gradle-plugin.netlify.com/com/github/spotbugs/snom/spotbugstask) directly,
to set task-specific properties.

```groovy
// Example to configure HTML report
spotbugsMain {
    reports {
        html {
            enabled = true
            destination = file("$buildDir/reports/spotbugs/main/spotbugs.html")
            stylesheet = 'fancy-hist.xsl'
        }
    }
}
```

<details>
<summary>with Kotlin DSL</summary>

```kotlin
tasks.spotbugsMain {
    reports.create("html") {
        isEnabled = true
        setDestination(file("$buildDir/reports/spotbugs/main/spotbugs.html"))
        setStylesheet("fancy-hist.xsl")
    }
}
```
</details>

## SpotBugs version mapping

By default, this Gradle Plugin uses the SpotBugs version listed in this table.

You can change SpotBugs version by [the `toolVersion` property of the spotbugs extension](https://spotbugs-gradle-plugin.netlify.com/com/github/spotbugs/snom/spotbugsextension#toolVersion) or the `spotbugs` configuration.

|Gradle Plugin|SpotBugs|
|-----:|-----:|
| 4.7.10| 4.5.0|
| 4.7.8| 4.4.2|
| 4.7.5| 4.4.1|
| 4.7.3| 4.4.0|
| 4.7.2| 4.3.0|
| 4.6.1| 4.2.1|
| 4.5.0| 4.1.1|
| 4.4.4| 4.0.6|
| 4.4.2| 4.0.5|
| 4.0.7| 4.0.2|
| 4.0.0| 4.0.0|

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
* development requires java 11 or higher to be installed
* The CI server uses `ubuntu-latest` docker image, but you should be able to develop on any linux/unix based OS.
* before creating commits
  * read https://www.conventionalcommits.org/en
  * Optionally create the following script in your .git/hooks directory and name it commit.msg. This will ensure that your commits follow the covential commits pattern.
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
