package com.github.spotbugs.snom.internal

import com.github.spotbugs.snom.internal.SpotBugsTaskFactory.Companion.toLowerCamelCase
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class SpotBugsTaskFactorySpec : DescribeSpec({
    describe("SpotBugsTaskFactory") {
        it("converts task name to lowerCamelCase") {
            toLowerCamelCase("spotbugs", null) shouldBe "spotbugs"
            toLowerCamelCase("spotbugs", "") shouldBe "spotbugs"
            toLowerCamelCase("spotbugs", "main") shouldBe "spotbugsMain"
            toLowerCamelCase("spotbugs", "mainCode") shouldBe "spotbugsMainCode"
        }
    }
})
