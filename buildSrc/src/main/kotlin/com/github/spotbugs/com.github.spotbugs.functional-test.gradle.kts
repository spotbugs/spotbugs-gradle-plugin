plugins {
    groovy
    java
    `java-gradle-plugin`
}

val functionalTestImplementation by configurations.creating {
    extendsFrom(configurations.named("testImplementation").get())
}

dependencies {
    functionalTestImplementation("org.spockframework:spock-core:2.0-M5-groovy-3.0")
}

val functionalTest by sourceSets.creating {
    groovy.srcDir(file("src/functionalTest/groovy"))
    resources.srcDir(file("src/functionalTest/resources"))
    compileClasspath += files(sourceSets.named("main").map { it.output })
    runtimeClasspath += output + compileClasspath
}

gradlePlugin {
    testSourceSets(functionalTest)
}

tasks {
    val runFunctionalTest by registering(Test::class) {
        description = "Runs the functional tests."
        group = "verification"
        testClassesDirs = functionalTest.output.classesDirs
        classpath = functionalTest.runtimeClasspath
        mustRunAfter(tasks.test)
        systemProperty("snom.test.functional.gradle", providers.systemProperty("snom.test.functional.gradle").orElse(gradle.gradleVersion))
    }

    check.configure {
        dependsOn(runFunctionalTest)
    }
}
