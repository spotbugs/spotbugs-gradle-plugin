package com.github.spotbugs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import org.gradle.internal.impldep.org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Test's behaviour of the spotbugs plugin when operating with multiple code languages.
 * <p>
 * Currently tests for groovy, scala - but changes should apply to kotlin as well.
 *
 * @author Kevin Mc Tiernan, 23.05.2018, kemc@skagenfondene.no / kevin.tiernan@knowit.no
 */
public class MultipleBuildOutputSourceDirsTest {

    @Rule
    public TemporaryFolder folder= new TemporaryFolder();

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
      String buildScript = "plugins {\n"
        + "    id 'java'\n"
        + "    id 'scala'\n"
        + "    id 'com.github.spotbugs' version '1.6.1'\n"
        + "}\n"
        + "version = 1.0\n"
        + "repositories {\n"
        + "    mavenCentral()\n"
        + "    mavenLocal()\n"
        + "}\n"

        + "apply plugin: 'java'\n"
        + "apply plugin: 'scala'\n"

        + "sourceSets.main.scala.srcDirs = ['src/main/java', 'src/main/scala']\n"
        + "sourceSets.main.java.srcDirs = []\n"

        + "ext.scalaFullVersion = '2.10.3'\n"
        + "dependencies {\n"
            + "compile 'org.scala-lang:scala-library:2.10.7'\n"
        + "}\n";
      File buildFile = folder.newFile("build.gradle");
      Files.write(buildFile.toPath(), buildScript.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);

      File javaSourceDir = folder.newFolder("src", "main", "java");
      File scalaSourceDir = folder.newFolder("src", "main", "scala");
      File to = new File(javaSourceDir, "Foo.java");
      File from = new File("src/test/java/com/github/spotbugs/Foo.java");
      Files.copy(from.toPath(), to.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
    }

    private void createProjectGroovy() throws IOException {
        String buildScript = "plugins {\n" +
                "  id 'java'\n" +
                "  id 'groovy'\n" +
                "  id 'com.github.spotbugs' version '1.6.1'\n" +
                "}\n" +
                "version = 1.0\n" +
                "repositories {\n" +
                "  mavenCentral()\n" +
                "  mavenLocal()\n" +
                "}\n" +
                "dependencies {\n" +
                "    compile 'org.codehaus.groovy:groovy-all:2.4.14'\n" +
                "}\n";
        File buildFile = folder.newFile("build.gradle");
        Files.write(buildFile.toPath(), buildScript.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);

        File sourceDir = folder.newFolder("src", "main", "groovy");
        File to = new File(sourceDir, "Bar.groovy");
        File from = new File("src/test/java/com/github/spotbugs/Bar.groovy");
        Files.copy(from.toPath(), to.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
    }

    private void createProjectGroovyTest() throws IOException {
        String buildScript = "plugins {\n" +
                "  id 'java'\n" +
                "  id 'groovy'\n" +
                "  id 'com.github.spotbugs' version '1.6.1'\n" +
                "}\n" +
                "version = 1.0\n" +
                "repositories {\n" +
                "  mavenCentral()\n" +
                "  mavenLocal()\n" +
                "}\n" +
                "dependencies {\n" +
                "    compile 'org.codehaus.groovy:groovy-all:2.4.14'\n" +
                "}\n";
        File buildFile = folder.newFile("build.gradle");
        Files.write(buildFile.toPath(), buildScript.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);

        File sourceDirJava = folder.newFolder("src", "main", "java");
        File sourceDirGroovy = folder.newFolder("src", "main", "groovy");
        File sourceDirGroovyTest = folder.newFolder("src", "test", "groovy");
        File to = new File(sourceDirJava, "Foo.java");
        File from = new File("src/test/java/com/github/spotbugs/Foo.java");

        Files.copy(from.toPath(), to.toPath(), StandardCopyOption.COPY_ATTRIBUTES);

        to = new File(sourceDirGroovyTest, "FooTest.groovy");
        from = new File("src/test/java/com/github/spotbugs/FooTest.groovy");

        Files.copy(from.toPath(), to.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
    }
}
