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

/**
 * The {@code Effort} is configuration to adjust SpotBugs detectors. Use lower effort to reduce
 * computation cost.
 *
 * <p><strong>Usage:</strong>
 *
 * <p>Set via the {@code spotbugs} extension to configure all tasks in your project: <div><code>
 * spotbugs {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;effort = 'less'<br>
 * }</code></div>
 *
 * <p>Or via {@code SpotBugsTask} to configure the specific task in your project:<div><code>
 * spotbugsMain { // or name of another task<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;effort = 'max'<br>
 * }</code></div>
 *
 * <p>See also <a href="https://spotbugs.readthedocs.io/en/stable/effort.html">SpotBugs Manual</a>.</p>
 */
enum Effort {
    /**
     * The effort level to minimize the computation cost. SpotBugs will try to conserve space at the
     * expense of precision.
     */
    MIN,
    /** The effort level to reduce the computation cost. */
    LESS,
    /** The default level that provides the same feature with {@link #MORE}. */
    DEFAULT,
    /**
     * The effort level that uses more computation cost. SpotBugs will try to detect more problems by
     * Interprocedural Analysis and Null Pointer Analysis.
     */
    MORE,
    /**
     * The effort level that maximize the computation cost. SpotBugs will run Interprocedural Analysis
     * of Referenced Classes.
     */
    MAX
}
