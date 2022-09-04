plugins {
    `java-gradle-plugin`
    `java-library`
    groovy
}

val functionalTestImplementation by configurations.creating {
    extendsFrom(configurations["testImplementation"])
}

dependencies {
    functionalTestImplementation("org.spockframework:spock-core:2.0-M5-groovy-3.0")
}

val functionalTest by sourceSets.creating {
    groovy {
        srcDir(file("src/functionalTest/groovy"))
    }
    resources {
        srcDir(file("src/functionalTest/resources"))
    }
    compileClasspath += sourceSets["main"].output
    runtimeClasspath += output + compileClasspath
}

gradlePlugin {
    testSourceSets(functionalTest)
}

val functionalTestTask = tasks.register<Test>("functionalTest") {
    description = "Runs the functional tests."
    group = "verification"
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    mustRunAfter(tasks.test)
    systemProperty("snom.test.functional.gradle", System.getProperty("snom.test.functional.gradle", gradle.gradleVersion))
}

tasks.named("check").configure {
    dependsOn(functionalTestTask)
}
