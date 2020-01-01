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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.gradle.api.file.FileCollection;
import org.gradle.process.JavaExecSpec;
import org.gradle.workers.ProcessWorkerSpec;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Value.Style(jdkOnly = true) // do not use Guava even though it's in CLASSPATH
@Value.Immutable
abstract class SpotBugsSpec implements Serializable {
  private static final long serialVersionUID = 100L;
  private final Logger log = LoggerFactory.getLogger(SpotBugsSpec.class);

  /**
   * @return The {@code maxHeapSize} for the JVM process. Configured by {@link SpotBugsExtension}.
   */
  abstract Optional<String> maxHeapSize();

  /** @return The SpotBugs .jar file and its dependencies. Configured by Gradle Configuration. */
  abstract List<File> spotbugsJar();

  /** @return The plugin files. Configured by Gradle Configuration. */
  abstract List<File> plugins();

  /**
   * @return The flag to ignore exit code of SpotBugs execution. Configured by {@link
   *     SpotBugsExtension}.
   */
  abstract boolean isIgnoreFailures();

  /** @return The flag to show progress during analysis. Configured by {@link SpotBugsExtension}. */
  abstract boolean isShowProgress();

  abstract FileCollection sourceDirs();

  abstract List<File> classDirs();

  abstract List<File> auxClassPaths();

  abstract List<String> extraArguments();

  abstract List<String> jvmArgs();

  void applyTo(JavaExecSpec javaExec) {
    javaExec.classpath(spotbugsJar());
    javaExec.setJvmArgs(jvmArgs());
    javaExec.setMain("edu.umd.cs.findbugs.FindBugs2");
    javaExec.setArgs(generateArguments());
    maxHeapSize().ifPresent(javaExec::setMaxHeapSize);
  }

  void applyTo(ProcessWorkerSpec spec) {
    spec.getClasspath().setFrom(spotbugsJar());
    spec.forkOptions(
        option -> {
          option.jvmArgs(jvmArgs());
          maxHeapSize().ifPresent(option::setMaxHeapSize);
        });
  }

  void applyTo(SpotBugsWorkParameters params) {
    params.getArguments().addAll(generateArguments());
  }

  private String join(Collection<File> files) {
    return files.stream()
        .map(File::getAbsolutePath)
        .collect(Collectors.joining(File.pathSeparator));
  }

  private List<String> generateArguments() {
    List<String> args = new ArrayList<>();
    if (!plugins().isEmpty()) {
      args.add("-pluginList");
      args.add(join(plugins()));
    }

    args.add("-sortByClass");
    args.add("-timestampNow");
    if (!auxClassPaths().isEmpty()) {
      args.add("-auxclasspath");
      args.add(join(auxClassPaths()));
    }
    if (!sourceDirs().isEmpty()) {
      args.add("-sourcepath");
      args.add(sourceDirs().getAsPath());
    }
    if (isShowProgress()) {
      args.add("-progress");
    }
    args.addAll(extraArguments());
    classDirs().forEach(dir -> args.add(dir.getAbsolutePath()));

    log.debug("Arguments for SpotBugs are generated: {}", args);
    return args;
  }
}
