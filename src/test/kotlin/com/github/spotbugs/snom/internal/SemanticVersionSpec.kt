package com.github.spotbugs.snom.internal

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe

class SemanticVersionSpec : DescribeSpec({
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
    }
})
