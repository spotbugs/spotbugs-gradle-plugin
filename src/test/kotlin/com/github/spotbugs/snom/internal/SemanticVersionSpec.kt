package com.github.spotbugs.snom.internal

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class SemanticVersionSpec :
    DescribeSpec({
        describe("SemanticVersion") {
            it("supports version without classifier") {
                val version = SemanticVersion("1.23.4")
                version.major shouldBe 1
                version.minor shouldBe 23
                version.patch shouldBe 4
            }

            it("supports version with SNAPSHOT classifier") {
                val version = SemanticVersion("5.0.67-SNAPSHOT")
                version.major shouldBe 5
                version.minor shouldBe 0
                version.patch shouldBe 67
            }

            it("compares different versions") {
                val version = SemanticVersion("4.5.0")
                SemanticVersion("4.4.10") shouldBeLessThan version
                SemanticVersion("4.5.0") shouldBeEqualComparingTo version
                SemanticVersion("4.7.0") shouldBeGreaterThan version
            }

            it("throws IllegalArgumentException for a version string that is not semver") {
                shouldThrow<IllegalArgumentException> {
                    SemanticVersion("not-a-version")
                }
            }

            it("error message contains the invalid version string") {
                val ex = shouldThrow<IllegalArgumentException> {
                    SemanticVersion("bad.version")
                }
                ex.message!! shouldContain "bad.version"
            }

            it("supports version with build metadata") {
                val version = SemanticVersion("1.2.3+build.456")
                version.major shouldBe 1
                version.minor shouldBe 2
                version.patch shouldBe 3
            }

            it("supports version with pre-release and build metadata") {
                val version = SemanticVersion("2.0.0-alpha.1+build.123")
                version.major shouldBe 2
                version.minor shouldBe 0
                version.patch shouldBe 0
            }

            it("has a meaningful toString") {
                val version = SemanticVersion("3.4.5")
                version.toString() shouldBe "SemanticVersion(3.4.5)"
            }
        }
    })
