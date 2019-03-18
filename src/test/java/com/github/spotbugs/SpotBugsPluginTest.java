package com.github.spotbugs;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Optional;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.util.GradleVersion;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SpotBugsPluginTest extends Assert{
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Before
  public void createProject() throws IOException {
    Files.copy(Paths.get("src/test/resources/SpotBugsPlugin.gradle"), folder.getRoot().toPath().resolve("build.gradle"),
            StandardCopyOption.COPY_ATTRIBUTES);

    File sourceDir = folder.newFolder("src", "main", "java");
    File to = new File(sourceDir, "Foo.java");
    File from = new File("src/test/java/com/github/spotbugs/Foo.java");
    Files.copy(from.toPath(), to.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
  }

  @Test
  public void TestSpotBugsTasksExist() throws Exception{
    BuildResult result = GradleRunner.create().withProjectDir(folder.getRoot()).withArguments(Arrays.asList("tasks", "--all")).withPluginClasspath().build();
    assertTrue(result.getOutput().contains("spotbugsMain"));
    assertTrue(result.getOutput().contains("spotbugsTest"));
  }

  @Test
  public void testSpotBugsTaskCanRun() throws Exception {
    BuildResult result = GradleRunner.create()
            .withProjectDir(folder.getRoot())
            .withArguments(Arrays.asList("compileJava", "spotbugsMain"))
            .withPluginClasspath().build();
    Optional<BuildTask> spotbugsMain = findTask(result, ":spotbugsMain");
    assertTrue(spotbugsMain.isPresent());
    assertThat(spotbugsMain.get().getOutcome(), is(TaskOutcome.SUCCESS));
  }

  /**
   * Only run this test under JDK 8.
   *
   * Skip this test on Java 11 - it simulates a lower Gradle version,
   * whose {@link org.gradle.api.JavaVersion} enum may not recognize Java 11 at runtime
   * and will fail.
   */
  @Test
  public void testSpotBugsTaskCanRunWithMinimumSupportedVersion() throws Exception {
    Assume.assumeThat(System.getProperty("java.version"), CoreMatchers.startsWith("1.8"));
    BuildResult result = GradleRunner.create()
            .withProjectDir(folder.getRoot())
            .withArguments(Arrays.asList("compileJava", "spotbugsMain"))
            .withPluginClasspath()
            .withGradleVersion(SpotBugsPlugin.SUPPORTED_VERSION.getVersion())
            .build();
    Optional<BuildTask> spotbugsMain = findTask(result, ":spotbugsMain");
    assertTrue(spotbugsMain.isPresent());
    assertThat(spotbugsMain.get().getOutcome(), is(TaskOutcome.SUCCESS));
  }

  @Test
  public void testSpotBugsTestTaskCanRun() throws Exception {
    BuildResult result = GradleRunner.create()
            .withProjectDir(folder.getRoot())
            .withArguments(Arrays.asList("compileTestJava", "spotbugsTest"))
            .withPluginClasspath().build();
    Optional<BuildTask> spotbugsTest = findTask(result, ":spotbugsTest");
    assertTrue(spotbugsTest.isPresent());
    assertThat(spotbugsTest.get().getOutcome(), is(TaskOutcome.NO_SOURCE));
  }

  @Test
  public void testCheckTaskDependsOnSpotBugsTasks() throws Exception {
    BuildResult result = GradleRunner.create()
            .withProjectDir(folder.getRoot())
            .withArguments(Arrays.asList("compileJava", "compileTestJava", "check"))
            .withPluginClasspath().build();
    assertTrue(findTask(result, ":spotbugsMain").isPresent());
    assertTrue(findTask(result, ":spotbugsTest").isPresent());
  }

  private Optional<BuildTask> findTask(BuildResult result, String taskName) {
    return result.getTasks().stream()
          .filter(task -> task.getPath().equals(taskName))
          .findAny();
  }

  @Test
  public void testLoadToolVersion() {
    SpotBugsPlugin plugin = new SpotBugsPlugin();
    assertThat(plugin.loadToolVersion(), is(notNullValue()));
  }

    @Test(expected = IllegalArgumentException.class)
    public void testVersionVerifyForGradleVersion2() {
        SpotBugsPlugin plugin = new SpotBugsPlugin();
        plugin.verifyGradleVersion(GradleVersion.version("2.0"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVersionVerifyForGradleVersion3() {
        SpotBugsPlugin plugin = new SpotBugsPlugin();
        plugin.verifyGradleVersion(GradleVersion.version("3.0"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVersionVerifyForGradleVersion4() {
        SpotBugsPlugin plugin = new SpotBugsPlugin();
        plugin.verifyGradleVersion(GradleVersion.version("4.0"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVersionVerifyForGradleVersion41() {
        SpotBugsPlugin plugin = new SpotBugsPlugin();
        plugin.verifyGradleVersion(GradleVersion.version("4.1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVersionVerifyForGradleVersion42() {
        SpotBugsPlugin plugin = new SpotBugsPlugin();
        plugin.verifyGradleVersion(GradleVersion.version("4.2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVersionVerifyForGradleVersion5() {
        SpotBugsPlugin plugin = new SpotBugsPlugin();
        plugin.verifyGradleVersion(GradleVersion.version("5.0"));
    }

    @Test
    public void testVersionVerifyForGradleVersion51() {
        SpotBugsPlugin plugin = new SpotBugsPlugin();
        plugin.verifyGradleVersion(GradleVersion.version("5.1"));
    }

    @Test
    public void testVersionVerifyForGradleVersion52() {
        SpotBugsPlugin plugin = new SpotBugsPlugin();
        plugin.verifyGradleVersion(GradleVersion.version("5.2"));
    }
}
