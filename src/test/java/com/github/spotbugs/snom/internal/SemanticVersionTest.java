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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SemanticVersionTest {
  @Test
  void versionWithoutClassifier() {
    SemanticVersion version = new SemanticVersion("1.23.4");
    assertEquals(1, version.getMajor());
    assertEquals(23, version.getMinor());
    assertEquals(4, version.getPatch());
  }

  @Test
  void snapshotVersion() {
    SemanticVersion version = new SemanticVersion("5.0.67-SNAPSHOT");
    assertEquals(5, version.getMajor());
    assertEquals(0, version.getMinor());
    assertEquals(67, version.getPatch());
  }

  @Test
  void compare() {
    SemanticVersion version = new SemanticVersion("4.5.0");
    assertTrue(new SemanticVersion("4.4.10").compareTo(version) < 0);
    assertTrue(new SemanticVersion("4.5.0").compareTo(version) == 0);
    assertTrue(new SemanticVersion("4.7.0").compareTo(version) > 0);
  }
}
