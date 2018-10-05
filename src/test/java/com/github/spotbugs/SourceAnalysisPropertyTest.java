package com.github.spotbugs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class SourceAnalysisPropertyTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void createProject() throws IOException {
        String buildScript =
                "plugins {\n" +
                        "  id 'java'\n" +
                        "  id 'com.github.spotbugs'\n" +
                        "}\n" +
                        "version = 1.0\n" +
                        "repositories {\n" +
                        "  mavenCentral()\n" +
                        "  mavenLocal()\n" +
                        "}\n" +
                        "sourceSets {\n" +
                        "  main {\n" +
                        "    java.srcDirs = ['src/main/java']\n" +
                        "  }\n" +
                        "}\n" +
                        "tasks.withType(com.github.spotbugs.SpotBugsTask) {\n" +
                        "  jvmArgs = ['-Dfindbugs.sf.comment=true']\n" +
                        "}\n";

        File buildFile = folder.newFile("build.gradle");
        Files.write(buildFile.toPath(), buildScript.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);

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
