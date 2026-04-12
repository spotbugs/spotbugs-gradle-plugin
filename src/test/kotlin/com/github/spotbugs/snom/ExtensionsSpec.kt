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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.gradle.testfixtures.ProjectBuilder

class ExtensionsSpec :
    DescribeSpec({
        describe("Extensions") {
            val project = ProjectBuilder.builder().build()

            describe("Property<Confidence>.assign") {
                it("sets confidence from a valid string name") {
                    val property = project.objects.property(Confidence::class.java)
                    property.assign("LOW")
                    property.get() shouldBe Confidence.LOW
                }

                it("sets each confidence value from its string name") {
                    val property = project.objects.property(Confidence::class.java)
                    for (confidence in Confidence.entries) {
                        property.assign(confidence.name)
                        property.get() shouldBe confidence
                    }
                }

                it("throws when given an invalid confidence name") {
                    val property = project.objects.property(Confidence::class.java)
                    shouldThrow<IllegalArgumentException> {
                        property.assign("INVALID")
                    }
                }
            }

            describe("Property<Effort>.assign") {
                it("sets effort from a valid string name") {
                    val property = project.objects.property(Effort::class.java)
                    property.assign("MAX")
                    property.get() shouldBe Effort.MAX
                }

                it("sets each effort value from its string name") {
                    val property = project.objects.property(Effort::class.java)
                    for (effort in Effort.entries) {
                        property.assign(effort.name)
                        property.get() shouldBe effort
                    }
                }

                it("throws when given an invalid effort name") {
                    val property = project.objects.property(Effort::class.java)
                    shouldThrow<IllegalArgumentException> {
                        property.assign("INVALID")
                    }
                }
            }
        }
    })
