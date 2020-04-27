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
    showProgress = true
    effort = 'default'
    reportLevel = 'default'
    visitors = [ 'FindSqlInjection', 'SwitchFallthrough' ]
    omitVisitors = [ 'FindNonShortCircuit' ]
    reportsDir = file("$buildDir/spotbugs")
    includeFilter = file("include.xml")
    excludeFilter = file("exclude.xml")
    onlyAnalyze = [ 'com.foobar.MyClass', 'com.foobar.mypkg.*' ]
    maxHeapSize = '1g'
    extraArgs = [ '-nested:false' ]
    jvmArgs = [ '-Duser.language=ja' ]
}
```

Configure `spotbugsPlugin` to apply any SpotBugs plugin:

```groovy
dependencies {
    spotbugsPlugins 'com.h3xstream.findsecbugs:findsecbugs-plugin:1.7.1'
}
```

Configure `spotbugs` to choose your favorite SpotBugs version:

```groovy
dependencies {
    spotbugs 'com.github.spotbugs:spotbugs:4.0.0'
}
```

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

## SpotBugs version mapping

By default, this Gradle Plugin uses the SpotBugs version listed in this table.

You can change SpotBugs version by [the `toolVersion` property of the spotbugs extension](https://spotbugs-gradle-plugin.netlify.com/com/github/spotbugs/snom/spotbugsextension#toolVersion) or the `spotbugs` configuration.

|Gradle Plugin|SpotBugs|
|-----:|-----:|
| 4.0.7| 4.0.2|
| 4.0.0| 4.0.0|

### Refer the version in the build script

From v4, the `spotbugs.toolVersion` is changed from `String` to [`Provider<String>`](https://docs.gradle.org/current/javadoc/org/gradle/api/provider/Property.html), so use `get()` or other methods to refer the actual version.

```groovy
dependencies {
    compileOnly "com.github.spotbugs:spotbugs-annotations:${spotbugs.toolVersion.get()}"
}
```

## Copyright

Copyright &copy; 2019-present SpotBugs Team
