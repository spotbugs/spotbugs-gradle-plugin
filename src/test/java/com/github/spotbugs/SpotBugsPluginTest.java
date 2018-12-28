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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SpotBugsPluginTest extends Assert{
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private File sourceDir;

  @Before
  public void createProject() throws IOException {
    Files.copy(Paths.get("src/test/resources/SpotBugsPlugin.gradle"), folder.getRoot().toPath().resolve("build.gradle"),
            StandardCopyOption.COPY_ATTRIBUTES);

    sourceDir = folder.newFolder("src", "main", "java");
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

  @Test
  public void testSpotBugsTaskCanFailTheBuild() throws IOException {
    addClassWithBug();

    BuildResult result = GradleRunner.create()
        .withProjectDir(folder.getRoot())
        .withArguments(Arrays.asList("compileJava", "spotbugsMain"))
        .withPluginClasspath()
        .buildAndFail(); //Bar.java's bug _should_ fail the build

    Optional<BuildTask> spotbugsMain = findTask(result, ":spotbugsMain");
    assertTrue(spotbugsMain.isPresent());
    assertThat(spotbugsMain.get().getOutcome(), is(TaskOutcome.FAILED));
    assertTrue(result.getOutput().contains("SpotBugs rule violations were found."));
  }

  @Test
  public void testSpotBugsTaskWarnsWhenIgnoringFailures() throws IOException {
    addClassWithBug();

    BuildResult result = GradleRunner.create()
        .withProjectDir(folder.getRoot())
        .withArguments(Arrays.asList("compileJava", "spotbugsMain", "-PignoreFailures=true"))
        .withPluginClasspath()
        .build(); //Bar.java's bug _should_ fail the build, but logs a warning in this case since we set ignoreFailures = true

    Optional<BuildTask> spotbugsMain = findTask(result, ":spotbugsMain");
    assertTrue(spotbugsMain.isPresent());
    assertThat(spotbugsMain.get().getOutcome(), is(TaskOutcome.SUCCESS));
    assertTrue(result.getOutput().contains("SpotBugs rule violations were found."));
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

  private void addClassWithBug() {
    try {
      File to = new File(sourceDir, "Bar.java");
      File from = new File(getClass().getClassLoader().getResource("com/github/spotbugs/Bar.java").getFile());
      Files.copy(from.toPath(), to.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
    } catch (IOException e) {
      throw new RuntimeException("Error adding Bar.java", e);
    }
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

    @Test
    public void testVersionVerifyForGradleVersion4() {
        SpotBugsPlugin plugin = new SpotBugsPlugin();
        plugin.verifyGradleVersion(GradleVersion.version("4.0"));
    }

    @Test
    public void testVersionVerifyForGradleVersion41() {
        SpotBugsPlugin plugin = new SpotBugsPlugin();
        plugin.verifyGradleVersion(GradleVersion.version("4.1"));
    }

    @Test
    public void testVersionVerifyForGradleVersion42() {
        SpotBugsPlugin plugin = new SpotBugsPlugin();
        plugin.verifyGradleVersion(GradleVersion.version("4.2"));
    }
}
