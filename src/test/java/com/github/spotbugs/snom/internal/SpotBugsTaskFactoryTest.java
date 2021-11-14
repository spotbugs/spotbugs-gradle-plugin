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

import org.junit.jupiter.api.Test;

class SpotBugsTaskFactoryTest {
  @Test
  void toLowerCamelCase() {
    assertEquals("spotbugs", SpotBugsTaskFactory.toLowerCamelCase("spotbugs", null));
    assertEquals("spotbugs", SpotBugsTaskFactory.toLowerCamelCase("spotbugs", ""));
    assertEquals("spotbugsMain", SpotBugsTaskFactory.toLowerCamelCase("spotbugs", "main"));
    assertEquals("spotbugsMainCode", SpotBugsTaskFactory.toLowerCamelCase("spotbugs", "mainCode"));
  }
}
