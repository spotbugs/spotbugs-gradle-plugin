/*
 * Copyright 2023 SpotBugs team
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

import java.io.ByteArrayOutputStream
import java.io.FilterOutputStream
import java.io.OutputStream

/**
 * Monitors the stdout of forked process, and report when it contains some problems reported by
 * SpotBugs core.
 */
internal class OutputScanner(out: OutputStream) : FilterOutputStream(out) {
    private val builder = ByteArrayOutputStream()
    private var isFailedToReport = false
        get() = field

    override fun write(b: ByteArray, off: Int, len: Int) {
        super.write(b, off, len)
        builder.write(b, off, len)
    }

    override fun write(b: Int) {
        super.write(b)
        builder.write(b)
        if (b == '\n'.code) {
            val line: String = builder.toString()
            if (line.contains("Could not generate HTML output")) {
                isFailedToReport = true
            }
            builder.reset()
        }
    }
}
