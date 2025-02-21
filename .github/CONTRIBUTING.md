# Contributing Guideline

## How to hack this project

Make sure you have Java 17+, Node.js 22+ and NPM on your `PATH`.

Before you start hacking, run `npm install` once to install development toolchain
to follow [the Conventional Commits](https://conventionalcommits.org/).

To build the implementation and test, run `./gradlew` then it runs all necessary formatter, compile and test.

## How to use latest plugin in your project

To test your changes, you need to build and install the plugin by your own.

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
    classpath "com.github.spotbugs.snom:spotbugs-gradle-plugin:(YOUR_VERSION)"
  }
}

apply plugin: "com.github.spotbugs.snom"
```

## Before reporting a problem

When you find problems and want to share with the community, consider to add some JUnit test cases to reproduce.
Just two steps to follow:

1. Create a [minimum and complete](http://stackoverflow.com/help/mcve) project, and reproduce it by [functional testing](https://guides.gradle.org/testing-gradle-plugins/). The test code is located in `src/functionalTest` directory.
2. Confirm that `./gradlew clean build` fails by the newly added test case.
