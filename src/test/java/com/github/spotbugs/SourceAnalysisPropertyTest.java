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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SourceAnalysisPropertyTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void createProject() throws IOException {
        Files.copy(Paths.get("src/test/resources/SourceAnalysisProperty.gradle"), folder.getRoot().toPath().resolve("build.gradle"),
                StandardCopyOption.COPY_ATTRIBUTES);

        File sourceDir = folder.newFolder("src", "main", "java", "com", "github", "spotbugs");
        File destinationFile = new File(sourceDir, "SourceAnalysisProperty.java");
        File sourceFile = new File("src/test/java/com/github/spotbugs/SourceAnalysisProperty.java");
        Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
    }

    @Test
    public void testNotReportingSwitchFallthroughWhenSourceAnalysisPropertySet() {
        BuildResult result = GradleRunner.create()
                .withProjectDir(folder.getRoot())
                .withDebug(true)
                .withArguments(Arrays.asList("compileJava", "spotbugsMain"))
                .withPluginClasspath().build();
        Optional<BuildTask> spotbugsTest = findTask(result, ":spotbugsMain");
        assertTrue(spotbugsTest.isPresent());
        assertThat(spotbugsTest.get().getOutcome(), is(TaskOutcome.SUCCESS));
    }

    private Optional<BuildTask> findTask(BuildResult result, String taskName) {
        return result.getTasks().stream()
                .filter(task -> task.getPath().equals(taskName))
                .findAny();
    }
}
