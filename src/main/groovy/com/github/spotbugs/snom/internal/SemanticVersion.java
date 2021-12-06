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
package com.github.spotbugs.snom.internal;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** @see <a href="https://semver.org/spec/v2.0.0.html">spec</a> */
public final class SemanticVersion implements Comparable<SemanticVersion> {
  /** @see <a href="https://regex101.com/r/vkijKf/1/">regular expressions 101</a> */
  private static final Pattern PATTERN =
      Pattern.compile(
          "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");

  private final int major;
  private final int minor;
  private final int patch;

  public SemanticVersion(String version) {
    Matcher matcher = PATTERN.matcher(version);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(version + " is not valid as a semantic version");
    }

    major = Integer.parseInt(matcher.group(1), 10);
    minor = Integer.parseInt(matcher.group(2), 10);
    patch = Integer.parseInt(matcher.group(3), 10);
  }

  public int getMajor() {
    return major;
  }

  public int getMinor() {
    return minor;
  }

  public int getPatch() {
    return patch;
  }

  @Override
  public int compareTo(SemanticVersion that) {
    return Comparator.comparingInt(SemanticVersion::getMajor)
        .thenComparingInt(SemanticVersion::getMinor)
        .thenComparingInt(SemanticVersion::getPatch)
        .compare(this, that);
  }

  @Override
  public String toString() {
    return "SemanticVersion(" + major + '.' + minor + '.' + patch + ')';
  }
}
