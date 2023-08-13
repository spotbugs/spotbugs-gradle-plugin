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
import java.util.Optional

/**
 * The [Confidence] is used to specify the level to report bugs. Lower level contains more
 * bugs reported. To include all bugs to your report, use [LOW].
 *
 * ### Usage
 *
 * Set via the {@code spotbugs} extension to configure all tasks in your project:
 * ```kotlin
 * // require Gradle 8.2+
 * import com.github.spotbugs.snom.Confidence
 * spotbugs {
 *     reportLevel = Confidence.LOW
 * }
 * ```
 *
 * Or via [SpotBugsTask] to configure the specific task in your project:
 * ```kotlin
 * // require Gradle 8.2+
 * import com.github.spotbugs.snom.Confidence
 * spotbugsMain { // or name of another task
 *     reportLevel = Confidence.LOW
 * }
 * ```
 *
 * See also [SpotBugs Manual](https://spotbugs.readthedocs.io/en/stable/running.html).
 */
enum class Confidence {
    /** The report level to report all detected bugs in the report. */
    LOW {
        override fun toCommandLineOption(): Optional<String> =
            Optional.of("-low")
    },

    /** The report level to report medium and high priority detected bugs in the report. */
    MEDIUM {
        override fun toCommandLineOption(): Optional<String> =
            Optional.of("-medium")
    },

    /** The default level that provides the same feature with {@link #MEDIUM}. */
    DEFAULT {
        override fun toCommandLineOption(): Optional<String> =
            Optional.empty()
    },

    /** The report level to report high priority detected bugs in the report. */
    HIGH {
        override fun toCommandLineOption(): Optional<String> =
            Optional.of("-high")
    }, ;

    @Internal("This is internally used property so no need to refer to judge out-of-date or not.")
    abstract fun toCommandLineOption(): Optional<String>
}
