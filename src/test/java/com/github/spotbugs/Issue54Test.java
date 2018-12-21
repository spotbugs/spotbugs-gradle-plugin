/*
 * Contributions to SpotBugs
 * Copyright (C) 2017, kengo
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.github.spotbugs;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs-gradle-plugin/issues/54">GitHub issue</a>
 */
public class Issue54Test {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void createProject() throws IOException {
      String buildScript = "plugins {\n" +
        "  id 'java'\n" +
        "  id 'com.github.spotbugs'\n" +
        "}\n" +
        "version = 1.0\n" +
        "repositories {\n" +
        "  mavenCentral()\n" +
        "}\n" +
        "spotbugsMain {\n" +
        "  classes = classes.filter {\n" +
        "    !it.path.contains('com/')" +
        "  }\n" +
        "}";
      File buildFile = folder.newFile("build.gradle");
      Files.write(buildFile.toPath(), buildScript.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);

      File sourceDir = folder.newFolder("src", "main", "java");
      File to = new File(sourceDir, "Foo.java");
      File from = new File("src/test/java/com/github/spotbugs/Foo.java");
      Files.copy(from.toPath(), to.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
    }

    @Test
    public void test() throws Exception {
        BuildResult result = GradleRunner.create().withProjectDir(folder.getRoot())
                .withArguments(Arrays.asList("spotbugsMain")).withPluginClasspath().buildAndFail();
        assertThat(result.task(":spotbugsMain").getOutcome(), is(TaskOutcome.FAILED));
        assertThat(result.getOutput(), containsString("No classes configured for SpotBugs analysis."));
    }
}
