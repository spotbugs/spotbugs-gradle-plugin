# Contributing Guideline

## How to use latest plugin in your project

This Gradle plugin has already been published to [Gradle Plugin Portal](https://plugins.gradle.org/plugin/jp.skypencil.spotbugs.snom) but the approval process is still pending. So you need to build and install the plugin by your own.

To install the plugin into your Maven Local Repository, add `apply plugin: 'maven-publish'` to `build.gradle` and run `./gradlew publishToMavenLocal`. Then you can use the installed plugin like below:

```groovy
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
    mavenLocal()
  }
  dependencies {
    classpath "com.github.spotbugs.snom:spotbugs-gradle-plugin-v2:0.1.1"
  }
}

apply plugin: "jp.skypencil.spotbugs.snom"
```

## Before reporting a problem

When you find problems and want to share with the community, consider to add some JUnit test cases to reproduce.
Just two steps to follow:

1. Create a [minimum and complete](http://stackoverflow.com/help/mcve) project, and reproduce it by [functional testing](https://guides.gradle.org/testing-gradle-plugins/). The test code is located in `src/functionalTest` directory.
2. Confirm that `./gradlew clean build` fails by the newly added test case.

