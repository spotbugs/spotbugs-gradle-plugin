# Release procedure

When you release fixed version of SpotBugs Gradle Plugin, please follow these procedures.

## Update version info

Create a commit to hadnle following changes:

* Remove `-SNAPSHOT` from `version` in `build.gradle`
* Replace `Unreleased` with actual version number in `CHANGELOG.md`

This commit will be tagged with version number. Refer [db979662](https://github.com/spotbugs/spotbugs-gradle-plugin/commit/db9796621629fd6326f8618ddc4660e881a8b396) as example.

## Prepare for next development

Create a commit to handle following changes:

* change `version` in `build.gradle` to SNAPSHOT version
* add `Unreleased - 2018-??-??` into `CHANGELOG.md`

Refer [ef63f198](https://github.com/spotbugs/spotbugs-gradle-plugin/commit/ef63f1980d75a1999af00b3505667f3932c8c0fa) as example.

## Create pull request, and merge it with review

Now you have two commits in your topic branch. Create a pull request from it, and ask other SpotBugs teammates to review. They'll merge and tag your commit.

## Release to Gradle Plugin Portal

When we push tag, the build result on Travis CI will be deployed to [Gradle Plugin Portal](https://plugins.gradle.org/). Check [official document](https://plugins.gradle.org/docs/submit) for detail.
