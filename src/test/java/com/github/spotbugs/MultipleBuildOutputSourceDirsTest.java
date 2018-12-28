package com.github.spotbugs;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import org.gradle.internal.impldep.org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test's behaviour of the spotbugs plugin when operating with multiple code languages.
 * <p>
 * Currently tests for groovy, scala - but changes should apply to kotlin as well.
 *
 * @author Kevin Mc Tiernan, 23.05.2018, kemc@skagenfondene.no / kevin.tiernan@knowit.no
 */
public class MultipleBuildOutputSourceDirsTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void cleanUpProjectFolder() {
        final File[] directoryContent = folder.getRoot().listFiles();

        if (directoryContent != null && directoryContent.length == 0) {
            return;
        }

        for (File currentFile : directoryContent) {
            FileUtils.deleteQuietly(currentFile);
        }
    }

    @Test
    public void testCombinedScalaJavaSources() throws IOException {
        // :: Setup
        createProjectScala();
        // :: Act
        BuildResult result = GradleRunner.create()
                .withProjectDir(folder.getRoot())
                .withArguments(Arrays.asList("spotbugsMain"))
                .withPluginClasspath()
                .forwardOutput()
                .build();
        // :: Verify
        assertThat(result.task(":spotbugsMain").getOutcome(), is(TaskOutcome.SUCCESS));
    }

    @Test
    public void testSingleGroovySource() throws IOException {
        // :: Setup
        createProjectGroovy();
        // :: Act
        BuildResult result = GradleRunner.create()
                .withProjectDir(folder.getRoot())
                .withArguments(Arrays.asList("spotbugsMain"))
                .withPluginClasspath()
                .forwardOutput()
                .build();
        // :: Verify
        assertThat(result.task(":spotbugsMain").getOutcome(), is(TaskOutcome.SUCCESS));
    }

    @Test
    public void testSingleGroovyTestSource() throws IOException {
        // :: Setup
        createProjectGroovyTest();
        // :: Act
        BuildResult result = GradleRunner.create()
                .withProjectDir(folder.getRoot())
                .withArguments(Arrays.asList("spotbugsTest"))
                .withPluginClasspath()
                .forwardOutput()
                .build();
        // :: Verify
        assertThat(result.task(":spotbugsTest").getOutcome(), is(TaskOutcome.SUCCESS));
    }

    // =========================== Helper methods =====================================================================

    private void createProjectScala() throws IOException {
      Files.copy(Paths.get("src/test/resources/MultipleBuildOutputSourceDirsScala.gradle"), folder.getRoot().toPath().resolve("build.gradle"),
              StandardCopyOption.COPY_ATTRIBUTES);

      File javaSourceDir = folder.newFolder("src", "main", "java");
      File to = new File(javaSourceDir, "Foo.java");
      File from = new File("src/test/java/com/github/spotbugs/Foo.java");
      Files.copy(from.toPath(), to.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
    }

    private void createProjectGroovy() throws IOException {
        Files.copy(Paths.get("src/test/resources/MultipleBuildOutputSourceDirsGroovy.gradle"), folder.getRoot().toPath().resolve("build.gradle"),
                StandardCopyOption.COPY_ATTRIBUTES);

        File sourceDir = folder.newFolder("src", "main", "groovy");
        File to = new File(sourceDir, "Bar.groovy");
        File from = new File("src/test/java/com/github/spotbugs/Bar.groovy");
        Files.copy(from.toPath(), to.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
    }

    private void createProjectGroovyTest() throws IOException {
        Files.copy(Paths.get("src/test/resources/MultipleBuildOutputSourceDirsGroovy.gradle"), folder.getRoot().toPath().resolve("build.gradle"),
                StandardCopyOption.COPY_ATTRIBUTES);

        File sourceDirJava = folder.newFolder("src", "main", "java");
        File sourceDirGroovyTest = folder.newFolder("src", "test", "groovy");
        File to = new File(sourceDirJava, "Foo.java");
        File from = new File("src/test/java/com/github/spotbugs/Foo.java");

        Files.copy(from.toPath(), to.toPath(), StandardCopyOption.COPY_ATTRIBUTES);

        to = new File(sourceDirGroovyTest, "FooTest.groovy");
        from = new File("src/test/java/com/github/spotbugs/FooTest.groovy");

        Files.copy(from.toPath(), to.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
    }
}