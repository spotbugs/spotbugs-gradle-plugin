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
package com.github.spotbugs.snom.internal

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayOutputStream

class OutputScannerSpec :
    DescribeSpec({
        describe("OutputScanner") {
            it("is not flagged as failed initially") {
                val out = ByteArrayOutputStream()
                val scanner = OutputScanner(out)
                scanner.isFailedToReport shouldBe false
            }

            it("detects 'Could not generate HTML output' via write(byte[], off, len)") {
                val out = ByteArrayOutputStream()
                val scanner = OutputScanner(out)
                val message = "Could not generate HTML output\n"
                scanner.write(message.toByteArray(), 0, message.length)
                scanner.isFailedToReport shouldBe true
            }

            it("does not flag unrelated messages via write(byte[], off, len)") {
                val out = ByteArrayOutputStream()
                val scanner = OutputScanner(out)
                val message = "Analysis complete\n"
                scanner.write(message.toByteArray(), 0, message.length)
                scanner.isFailedToReport shouldBe false
            }

            it("forwards bytes to the underlying stream via write(byte[], off, len)") {
                val out = ByteArrayOutputStream()
                val scanner = OutputScanner(out)
                val message = "Hello World\n"
                scanner.write(message.toByteArray(), 0, message.length)
                out.toString() shouldBe message
            }

            it("detects 'Could not generate HTML output' via write(int)") {
                val out = ByteArrayOutputStream()
                val scanner = OutputScanner(out)
                for (b in "Could not generate HTML output\n".toByteArray()) {
                    scanner.write(b.toInt())
                }
                scanner.isFailedToReport shouldBe true
            }

            it("does not flag unrelated messages via write(int)") {
                val out = ByteArrayOutputStream()
                val scanner = OutputScanner(out)
                for (b in "Some other output\n".toByteArray()) {
                    scanner.write(b.toInt())
                }
                scanner.isFailedToReport shouldBe false
            }

            it("forwards individual bytes to the underlying stream via write(int)") {
                val out = ByteArrayOutputStream()
                val scanner = OutputScanner(out)
                val message = "Test\n"
                for (b in message.toByteArray()) {
                    scanner.write(b.toInt())
                }
                out.toString() shouldBe message
            }

            it("resets the line buffer after each newline") {
                val out = ByteArrayOutputStream()
                val scanner = OutputScanner(out)
                for (b in "Some line\n".toByteArray()) {
                    scanner.write(b.toInt())
                }
                scanner.isFailedToReport shouldBe false
                // Writing failure message on second line should still be detected
                for (b in "Could not generate HTML output\n".toByteArray()) {
                    scanner.write(b.toInt())
                }
                scanner.isFailedToReport shouldBe true
            }

            it("respects byte offset and length in write(byte[], off, len)") {
                val out = ByteArrayOutputStream()
                val scanner = OutputScanner(out)
                val message = "XXCould not generate HTML output\nXX"
                scanner.write(message.toByteArray(), 2, message.length - 4)
                scanner.isFailedToReport shouldBe true
                out.toString() shouldBe "Could not generate HTML output\n"
            }
        }
    })
