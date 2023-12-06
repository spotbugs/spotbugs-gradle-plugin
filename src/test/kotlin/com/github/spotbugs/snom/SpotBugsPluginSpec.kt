package com.github.spotbugs.snom

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldNotBe
import org.gradle.util.GradleVersion

class SpotBugsPluginSpec : DescribeSpec({
    describe("SpotBugsPlugin") {
        it("loads the properties file from the packaged artifact") {
            SpotBugsBasePlugin().loadProperties().apply {
                getProperty("spotbugs-version") shouldNotBe null
                getProperty("slf4j-version") shouldNotBe null
            }
        }

        it("does not support Gradle 7.0") {
            shouldThrow<IllegalStateException> {
                SpotBugsBasePlugin().verifyGradleVersion(GradleVersion.version("7.0"))
            }
        }

        it("supports Gradle 7.1") {
            SpotBugsBasePlugin().verifyGradleVersion(GradleVersion.version("7.1"))
        }

        it("does not depend on Gradle internal API") {
            val implementation = ClassFileImporter()
                .importPackages("com.github.spotbugs.snom", "com.github.spotbugs.snom.internal")
            val rule: ArchRule = ArchRuleDefinition.noClasses().should()
                .dependOnClassesThat().resideInAPackage("org.gradle..internal..")
            rule.check(implementation)
        }
    }
})
