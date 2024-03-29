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

import org.gradle.api.tasks.Internal

/**
 * The [Confidence] is used to specify the level to report bugs. Lower level contains more
 * bugs reported. To include all bugs to your report, use [LOW].
 *
 * ### Usage
 *
 * Set via the [SpotBugsExtension] to configure all tasks in your project:
 * ```kotlin
 * // require Gradle 8.2+
 * spotbugs {
 *     reportLevel = com.github.spotbugs.snom.Confidence.LOW
 * }
 * ```
 *
 * Or via [SpotBugsTask] to configure the specific task in your project:
 * ```kotlin
 * // require Gradle 8.2+
 * spotbugsMain { // or name of another task
 *     reportLevel = com.github.spotbugs.snom.Confidence.LOW
 * }
 * ```
 *
 * See also [SpotBugs Manual](https://spotbugs.readthedocs.io/en/stable/running.html).
 */
enum class Confidence {
    /** The report level to report all detected bugs in the report. */
    LOW {
        override val commandLineOption: String = "-low"
    },

    /** The report level to report medium and high priority detected bugs in the report. */
    MEDIUM {
        override val commandLineOption: String = "-medium"
    },

    /** The default level that provides the same feature with [MEDIUM]. */
    DEFAULT {
        override val commandLineOption: String? = null
    },

    /** The report level to report high priority detected bugs in the report. */
    HIGH {
        override val commandLineOption: String = "-high"
    },
    ;

    @get:Internal("This is internally used property so no need to refer to judge out-of-date or not.")
    internal abstract val commandLineOption: String?
}
