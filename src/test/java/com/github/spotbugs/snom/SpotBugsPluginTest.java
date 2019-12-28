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
package com.github.spotbugs.snom;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpotBugsPluginTest {

  @Test
  void testLoadToolVersion() {
    assertNotNull(new SpotBugsPlugin().loadProperties().getProperty("spotbugs-version"));
    assertNotNull(new SpotBugsPlugin().loadProperties().getProperty("slf4j-version"));
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
                .resolve("snom"));
    Files.copy(
        Paths.get("src/test/resources/no-config.gradle"),
        tempDir.resolve("build.gradle"),
        StandardCopyOption.COPY_ATTRIBUTES);
    Files.copy(
        Paths.get("src/test/java/com/github/spotbugs/snom/Foo.java"),
        javaSource.resolve("Foo.java"),
        StandardCopyOption.COPY_ATTRIBUTES);

    BuildResult result =
        GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withArguments(Arrays.asList(":spotbugsMain"))
            .withPluginClasspath()
            .forwardOutput()
            .build();
    assertEquals(TaskOutcome.SUCCESS, result.task(":classes").getOutcome());
    assertEquals(TaskOutcome.SUCCESS, result.task(":spotbugsMain").getOutcome());
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
                .resolve("snom"));
    Files.copy(
        Paths.get("src/test/resources/tool-version.gradle"),
        tempDir.resolve("build.gradle"),
        StandardCopyOption.COPY_ATTRIBUTES);
    Files.copy(
        Paths.get("src/test/java/com/github/spotbugs/snom/Foo.java"),
        javaSource.resolve("Foo.java"),
        StandardCopyOption.COPY_ATTRIBUTES);

    StringWriter writer = new StringWriter();
    BuildResult result =
        GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withArguments(Arrays.asList("build", "--info", "-x", ":spotbugsTest"))
            .withPluginClasspath()
            .forwardStdOutput(writer)
            .forwardStdError(new OutputStreamWriter(System.err, StandardCharsets.UTF_8))
            .build();
    assertEquals(TaskOutcome.SUCCESS, result.task(":spotbugsMain").getOutcome());
    assertTrue(writer.getBuffer().toString().contains("spotbugs-4.0.0-beta4.jar"));
  }

  @Test
  @Disabled("need to install Android SDK, and configure plugin classpath via a file")
  void android(@TempDir Path tempDir) throws IOException {
    Path javaSource =
        Files.createDirectories(
            tempDir
                .resolve("src")
                .resolve("main")
                .resolve("java")
                .resolve("com")
                .resolve("github")
                .resolve("spotbugs")
                .resolve("snom"));
    Files.copy(
        Paths.get("src/test/resources/android.gradle"),
        tempDir.resolve("build.gradle"),
        StandardCopyOption.COPY_ATTRIBUTES);
    Files.copy(
        Paths.get("src/test/java/com/github/spotbugs/snom/Foo.java"),
        javaSource.resolve("Foo.java"),
        StandardCopyOption.COPY_ATTRIBUTES);

    GradleRunner runner =
        GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withArguments(
                Arrays.asList(
                    "tasks")) // TODO find proper task to run, still need some configuration in
            // build.gradle
            .withPluginClasspath()
            .forwardOutput();
    // we use `buildscript {}` in build.gradle,
    // so need to provide the PluginClasspath via a file
    Files.writeString(
        tempDir.resolve("plugin-classpath.txt"),
        runner.getPluginClasspath().stream()
            .map(File::getAbsolutePath)
            .collect(Collectors.joining("\n")),
        StandardCharsets.UTF_8);
    BuildResult result = runner.build();
    assertEquals(TaskOutcome.SUCCESS, result.task(":spotbugsMain").getOutcome());
  }
}
