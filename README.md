# Newly Organized SpotBugs Gradle Plugin

This is an unofficial Gradle Plugin to run SpotBugs on Java and Android project.

![](https://github.com/KengoTODA/spotbugs-gradle-plugin-v2/workflows/Java%20CI/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=KengoTODA_spotbugs-gradle-plugin-v2&metric=alert_status)](https://sonarcloud.io/dashboard?id=KengoTODA_spotbugs-gradle-plugin-v2)

## Goal

This Gradle plugin is designed to solve the following problems in the official one:

- [x] Remove any dependency on the Gradle's internal API
- [x] Solve mutability problem for the build contains multiple projects and/or sourceSet
- [ ] Native Support for [the Parallel Build](https://guides.gradle.org/using-the-worker-api/)
- [ ] Native Support for [the Android project](https://developer.android.com/studio/build/gradle-tips)
- [ ] Missing user document about how to use extension and task

## Usage

### Apply to your project

Apply the plugin to your project.
Currently this plugin isn't published to the repository, so you need to build and install your own.

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
    reports {
        
    }
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

### For Java project

Apply this plugin with [the `java` plugin](https://docs.gradle.org/current/userguide/java_plugin.html) to your project,
then `SpotBugsTask` will be generated for each existing sourceSet.

### For Android project

TBU

## Copyright

Copyright &copy; 2019-present SpotBugs Team
