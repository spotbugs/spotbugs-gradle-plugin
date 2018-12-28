package com.github.spotbugs;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class KotlinBuildScriptTest {

    @Rule
    public TemporaryFolder folder= new TemporaryFolder();

    private File sourceDir;

    @Before
    public void createKotlinDslProject() throws IOException {
        Files.copy(Paths.get("src/test/resources/KotlinBuildScript.gradle.kts"), folder.getRoot().toPath().resolve("build.gradle.kts"),
                StandardCopyOption.COPY_ATTRIBUTES);

        sourceDir = folder.newFolder("src", "main", "java");
        File to = new File(sourceDir, "Foo.java");
        File from = new File("src/test/java/com/github/spotbugs/Foo.java");
        Files.copy(from.toPath(), to.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
    }

    @Test
    public void TestSpotBugsTasksExist() throws Exception{
        BuildResult result = GradleRunner.create()
                .withProjectDir(folder.getRoot())
                .withArguments(Arrays.asList("tasks", "--all"))
                .withPluginClasspath()
                .build();
        assertTrue(result.getOutput().contains("spotbugsMain"));
        assertTrue(result.getOutput().contains("spotbugsTest"));
    }

    @Test
    @Ignore
    public void testSpotBugsTaskCanRun() throws Exception {
        BuildResult result = GradleRunner.create()
                .withProjectDir(folder.getRoot())
                .withArguments(Arrays.asList("compileJava", "spotbugsMain"))
                .withPluginClasspath()
                .build();
        Optional<BuildTask> spotbugsMain = findTask(result, ":spotbugsMain");
        assertTrue(spotbugsMain.isPresent());
        assertThat(spotbugsMain.get().getOutcome(), is(TaskOutcome.SUCCESS));
        assertTrue(new File(folder.getRoot(), "build/reports/spotbugs/main.xml").exists());
    }

    private Optional<BuildTask> findTask(BuildResult result, String taskName) {
        return result.getTasks().stream()
                .filter(task -> task.getPath().equals(taskName))
                .findAny();
    }

}
