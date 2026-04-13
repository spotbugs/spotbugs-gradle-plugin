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

class EffortSpec :
    DescribeSpec({
        describe("Effort") {
            it("has five values") {
                Effort.entries.size shouldBe 5
            }

            it("can be retrieved by name") {
                Effort.valueOf("MIN") shouldBe Effort.MIN
                Effort.valueOf("LESS") shouldBe Effort.LESS
                Effort.valueOf("DEFAULT") shouldBe Effort.DEFAULT
                Effort.valueOf("MORE") shouldBe Effort.MORE
                Effort.valueOf("MAX") shouldBe Effort.MAX
            }

            it("has values in expected order") {
                Effort.entries.map { it.name } shouldBe listOf("MIN", "LESS", "DEFAULT", "MORE", "MAX")
            }
        }
    })
