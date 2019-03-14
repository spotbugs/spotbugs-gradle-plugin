package com.github.spotbugs;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class HtmlReportTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void createProject() throws IOException {
      Files.copy(Paths.get("src/test/resources/HtmlReportTest.gradle"), folder.getRoot().toPath().resolve("build.gradle"),
          StandardCopyOption.COPY_ATTRIBUTES);

      File sourceDir = folder.newFolder("src", "main", "java");
      File to = new File(sourceDir, "Foo.java");
      File from = new File("src/test/java/com/github/spotbugs/Foo.java");
      Files.copy(from.toPath(), to.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
      Files.write(folder.newFile("settings.gradle").toPath(), "rootProject.name = 'my project name'".getBytes());
    }

    @Test
    public void testReportContainsVersion() throws Exception {
        GradleRunner.create().withProjectDir(folder.getRoot())
                .withArguments(Arrays.asList("spotbugsMain")).withPluginClasspath().build();
        Path report = folder.getRoot().toPath().resolve("build").resolve("reports").resolve("spotbugs").resolve("main.html");
        String html = Files.readAllLines(report).stream().collect(Collectors.joining("\n"));
        assertThat(html, containsString("\"1.2.3\""));
    }

    @Test
    public void testReportContainsProjectName() throws Exception {
        GradleRunner.create().withProjectDir(folder.getRoot())
                .withArguments(Arrays.asList("spotbugsMain")).withPluginClasspath().build();
        Path report = folder.getRoot().toPath().resolve("build").resolve("reports").resolve("spotbugs").resolve("main.html");
        String html = Files.readAllLines(report).stream().collect(Collectors.joining("\n"));
        assertThat(html, containsString("'my project name' (main)"));
    }

}
