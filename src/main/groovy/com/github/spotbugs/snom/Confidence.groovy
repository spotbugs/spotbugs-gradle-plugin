/*
 * Copyright 2019 SpotBugs team
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
package com.github.spotbugs.snom;

import javax.annotation.Nonnull;
import org.gradle.api.tasks.Internal;

/**
 * The {@code Confidence} is used to specify the level to report bugs. Lower level contains more
 * bugs reported. To include all bugs to your report, use {@link LOW}.
 *
 * <p><strong>Usage:</strong>
 *
 * <p>Set via the {@code spotbugs} extension to configure all tasks in your project:<div><code>
 * spotbugs {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;reportLevel = 'low'<br>
 * </code></div>
 *
 * <p>Or via {@code SpotBugsTask} to configure the specific task in your project:<div><code>
 * spotbugsMain { // or name of another task<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;reportLevel = 'high'<br>
 * }</code></div>
 *
 * @see <a href="https://spotbugs.readthedocs.io/en/stable/running.html">SpotBugs Manual</a>
 */
enum Confidence {
    /** The report level to report all detected bugs in the report. */
    LOW {
        @Override
        String toCommandLineOption() {
            return "-low"
        }
    },
    /** The report level to report medium and high priority detected bugs in the report. */
    MEDIUM {
        @Override
        String toCommandLineOption() {
            return "-medium"
        }
    },
    /** The default level that provides the same feature with {@link MEDIUM}. */
    DEFAULT {
        @Override
        String toCommandLineOption() {
            return ""
        }
    },
    /** The report level to report high priority detected bugs in the report. */
    HIGH {
        @Override
        String toCommandLineOption() {
            return "-high"
        }
    }

    @Internal("This is internally used property so no need to refer to judge out-of-date or not.")
    abstract @Nonnull String toCommandLineOption()
}
