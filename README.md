# Newly Organized SpotBugs Gradle Plugin

This is an unofficial Gradle Plugin to run SpotBugs on Java and Android project.

![](https://github.com/KengoTODA/spotbugs-gradle-plugin-v2/workflows/Java%20CI/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=KengoTODA_spotbugs-gradle-plugin-v2&metric=alert_status)](https://sonarcloud.io/dashboard?id=KengoTODA_spotbugs-gradle-plugin-v2)
[![](https://img.shields.io/badge/javadoc-latest-blightgreen?logo=java)](https://spotbugs-gradle-plugin.netlify.com/com/github/spotbugs/snom/package-summary.html)

## Goal

This Gradle plugin is designed to solve the following problems in the official one:

- [x] Remove any dependency on the Gradle's internal API
- [x] Solve mutability problem for the build contains multiple projects and/or sourceSet
- [x] Native Support for [the Parallel Build](https://guides.gradle.org/using-the-worker-api/)
- [ ] Native Support for [the Android project](https://developer.android.com/studio/build/gradle-tips)
- [ ] Missing user document about how to use extension and task

## Usage

### Apply to your project

Apply the plugin to your project.
Currently this plugin isn't published to the repository, so [you need to build and install your own](.github/CONTRIBUTING.md).

```groovy
plugins {
    id 'jp.skypencil.spotbugs.snom' // will be changed at the timing of official release
}
```

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
    onlyAnalyze = ['com.foobar.MyClass', 'com.foobar.mypkg.*']
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
    spotbugsPlugins 'com.github.spotbugs:spotbugs:3.1.11'
}
```

### Apply to Java project

Apply this plugin with [the `java` plugin](https://docs.gradle.org/current/userguide/java_plugin.html) to your project,
then [`SpotBugsTask`](https://spotbugs-gradle-plugin.netlify.com/com/github/spotbugs/snom/spotbugstask) will be generated for each existing sourceSet.

### Apply to Android project

TBU

### Configure the SpotBugsTask

Configure [`SpotBugsTask`](https://spotbugs-gradle-plugin.netlify.com/com/github/spotbugs/snom/spotbugstask) directly,
to set task-specific properties.

```groovy
// Example to configure HTML report
configurations { spotbugsStylesheets { transitive false } }
dependencies { spotbugsStylesheets 'com.github.spotbugs:spotbugs:3.1.10' }

spotbugsMain {
    reports {
        html {
            enabled = true
            destination = file("$buildDir/reports/spotbugs/main/spotbugs.html")
            stylesheet = resources.text.fromArchiveEntry(configurations.spotbugsStylesheets, 'fancy-hist.xsl')
        }
    }
}
```

### Run Analysis in Gradle Worker process

For multi sub-projects project, use [Gradle Worker](https://guides.gradle.org/using-the-worker-api/) to accelerate your build.
This feature is still experimental so you need to enable it explicitly by `com.github.spotbugs.snom.worker` property.

For instance, you can add the following line to your `gradle.properties` file:

```properties
com.github.spotbugs.snom.worker=true
```

## Copyright

Copyright &copy; 2019-present SpotBugs Team
