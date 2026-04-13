/*
 * Copyright 2021 SpotBugs team
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.spotbugs.snom

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ConfidenceSpec :
    DescribeSpec({
        describe("Confidence") {
            it("LOW has command line option '-low'") {
                Confidence.LOW.commandLineOption shouldBe "-low"
            }

            it("MEDIUM has command line option '-medium'") {
                Confidence.MEDIUM.commandLineOption shouldBe "-medium"
            }

            it("DEFAULT has a null command line option") {
                Confidence.DEFAULT.commandLineOption shouldBe null
            }

            it("HIGH has command line option '-high'") {
                Confidence.HIGH.commandLineOption shouldBe "-high"
            }

            it("has four values") {
                Confidence.entries.size shouldBe 4
            }

            it("can be retrieved by name") {
                Confidence.valueOf("LOW") shouldBe Confidence.LOW
                Confidence.valueOf("MEDIUM") shouldBe Confidence.MEDIUM
                Confidence.valueOf("DEFAULT") shouldBe Confidence.DEFAULT
                Confidence.valueOf("HIGH") shouldBe Confidence.HIGH
            }

            it("LOW command line option is not null") {
                Confidence.LOW.commandLineOption shouldNotBe null
            }

            it("MEDIUM command line option is not null") {
                Confidence.MEDIUM.commandLineOption shouldNotBe null
            }

            it("HIGH command line option is not null") {
                Confidence.HIGH.commandLineOption shouldNotBe null
            }
        }
    })
