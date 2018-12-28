plugins {
  java
  id("com.github.spotbugs")
}
version = 1.0
repositories {
  mavenCentral()
}
if (project.hasProperty("ignoreFailures")) {
  spotbugs.setIgnoreFailures(true)
}