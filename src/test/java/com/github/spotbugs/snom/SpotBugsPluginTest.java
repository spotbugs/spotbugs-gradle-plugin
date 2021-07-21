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
package com.github.spotbugs.snom;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.Test;

class SpotBugsPluginTest {

  @Test
  void testLoadToolVersion() {
    assertNotNull(new SpotBugsBasePlugin().loadProperties().getProperty("spotbugs-version"));
    assertNotNull(new SpotBugsBasePlugin().loadProperties().getProperty("slf4j-version"));
  }

  @Test
  void testVerifyGradleVersion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new SpotBugsBasePlugin().verifyGradleVersion(GradleVersion.version("6.7"));
        });
    new SpotBugsBasePlugin().verifyGradleVersion(GradleVersion.version("7.0"));
  }

  @Test
  void testDependencyOnGradleInternalAPI() {
    JavaClasses implementation =
        new ClassFileImporter()
            .importPackages("com.github.spotbugs.snom", "com.github.spotbugs.snom.internal");
    ArchRule rule =
        noClasses().should().dependOnClassesThat().resideInAPackage("org.gradle..internal..");
    rule.check(implementation);
  }
}
