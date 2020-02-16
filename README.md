# Newly Organized SpotBugs Gradle Plugin

This is an unofficial Gradle Plugin to run SpotBugs on Java and Android project.

![](https://github.com/KengoTODA/spotbugs-gradle-plugin-v2/workflows/Java%20CI/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=KengoTODA_spotbugs-gradle-plugin-v2&metric=alert_status)](https://sonarcloud.io/dashboard?id=KengoTODA_spotbugs-gradle-plugin-v2)
[![](https://img.shields.io/badge/groovydoc-latest-blightgreen?logo=groovy)](https://spotbugs-gradle-plugin.netlify.com/com/github/spotbugs/snom/package-summary.html)

## Goal

This Gradle plugin is designed to solve the following problems in the official one:

- [x] Remove any dependency on the Gradle's internal API
- [x] Solve mutability problem for the build contains multiple projects and/or sourceSet
- [x] Native Support for [the Parallel Build](https://guides.gradle.org/using-the-worker-api/)
- [ ] Native Support for [the Android project](https://developer.android.com/studio/build/gradle-tips)
- [x] Missing user document about how to use extension and task

## Usage

### Apply to your project

Apply the plugin to your project.
Refer [the Gradle Plugin portal](https://plugins.gradle.org/plugin/jp.skypencil.spotbugs.snom) about the detail of installation procedure.

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

## Copyright

Copyright &copy; 2019-present SpotBugs Team
