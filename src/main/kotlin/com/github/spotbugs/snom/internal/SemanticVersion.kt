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

import java.util.regex.Pattern

/**
 * See [SemVer 2.0 specification](https://semver.org/spec/v2.0.0.html)
 */
class SemanticVersion(version: String) : Comparable<SemanticVersion?> {
    val major: Int
    val minor: Int
    val patch: Int

    init {
        val matcher = PATTERN.matcher(version)
        require(matcher.matches()) { "$version is not valid as a semantic version" }
        major = matcher.group(1).toInt(10)
        minor = matcher.group(2).toInt(10)
        patch = matcher.group(3).toInt(10)
    }

    override operator fun compareTo(other: SemanticVersion?): Int {
        return Comparator.comparingInt { obj: SemanticVersion -> obj.major }
            .thenComparingInt { obj: SemanticVersion -> obj.minor }
            .thenComparingInt { obj: SemanticVersion -> obj.patch }
            .compare(this, other)
    }

    override fun toString(): String {
        return "SemanticVersion($major.$minor.$patch)"
    }

    companion object {
        /**
         * @see [regular expressions 101](https://regex101.com/r/vkijKf/1/)
         */
        private val PATTERN =
            Pattern.compile(
                "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\." +
                    "(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)" +
                    "(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$",
            )
    }
}
