/**
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
package com.github.spotbugs.gradle;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpotBugsPluginTest {

  @Test
  void testLoadToolVersion() {
    assertNotNull(new SpotBugsPlugin().loadToolVersion());
  }

  @Test
  void testVerifyGradleVersion() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new SpotBugsPlugin().verifyGradleVersion(GradleVersion.version("3.9"));
        });
    new SpotBugsPlugin().verifyGradleVersion(GradleVersion.version("4.0"));
  }

  @Test
  void defaultBehaviour(@TempDir Path tempDir) throws IOException {
    Path javaSource =
        Files.createDirectories(
            tempDir
                .resolve("src")
                .resolve("main")
                .resolve("java")
                .resolve("com")
                .resolve("github")
                .resolve("spotbugs")
                .resolve("gradle"));
    Files.copy(
        Paths.get("src/test/resources/no-config.gradle"),
        tempDir.resolve("build.gradle"),
        StandardCopyOption.COPY_ATTRIBUTES);
    Files.copy(
        Paths.get("src/test/java/com/github/spotbugs/gradle/Foo.java"),
        javaSource.resolve("Foo.java"),
        StandardCopyOption.COPY_ATTRIBUTES);

    BuildResult result =
        GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withArguments(Arrays.asList("check"))
            .withPluginClasspath()
            .build();
    List<BuildTask> tasks = result.getTasks();
    System.out.println(tasks);
    assertNotNull(result.task(":spotbugsMain"));
  }

  @Test
  void toolVersion(@TempDir Path tempDir) throws IOException {
    Path javaSource =
        Files.createDirectories(
            tempDir
                .resolve("src")
                .resolve("main")
                .resolve("java")
                .resolve("com")
                .resolve("github")
                .resolve("spotbugs")
                .resolve("gradle"));
    Files.copy(
        Paths.get("src/test/resources/tool-version.gradle"),
        tempDir.resolve("build.gradle"),
        StandardCopyOption.COPY_ATTRIBUTES);
    Files.copy(
        Paths.get("src/test/java/com/github/spotbugs/gradle/Foo.java"),
        javaSource.resolve("Foo.java"),
        StandardCopyOption.COPY_ATTRIBUTES);

    BuildResult result =
        GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withArguments(Arrays.asList("check"))
            .withPluginClasspath()
            .withDebug(true)
            .withArguments("--scan")
            .build();
    List<BuildTask> tasks = result.getTasks();
    // TODO confirm that the spotbugs 4.0.0-beta4 is used
  }

  @Test
  void generateTask(@TempDir Path tempDir) throws IOException {
    Path javaSource =
        Files.createDirectories(
            tempDir
                .resolve("src")
                .resolve("main")
                .resolve("java")
                .resolve("com")
                .resolve("github")
                .resolve("spotbugs")
                .resolve("gradle"));
    Files.copy(
        Paths.get("src/test/resources/skip-generate-task.gradle"),
        tempDir.resolve("build.gradle"),
        StandardCopyOption.COPY_ATTRIBUTES);
    Files.copy(
        Paths.get("src/test/java/com/github/spotbugs/gradle/Foo.java"),
        javaSource.resolve("Foo.java"),
        StandardCopyOption.COPY_ATTRIBUTES);

    BuildResult result =
        GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withArguments(Arrays.asList("check"))
            .withPluginClasspath()
            .build();
    assertNull(result.task(":spotbugsMain"));
  }
}
