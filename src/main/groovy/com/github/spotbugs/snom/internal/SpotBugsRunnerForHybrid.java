/*
 * Copyright 2021 SpotBugs team
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
package com.github.spotbugs.snom.internal;

import com.github.spotbugs.snom.SpotBugsReport;
import com.github.spotbugs.snom.SpotBugsTask;
import edu.umd.cs.findbugs.annotations.NonNull;
import groovy.lang.Closure;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.process.ExecOperations;
import org.gradle.process.JavaExecSpec;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkerExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link SpotBugsRunner} implementation that runs SpotBugs process from the worker process. This
 * approach enables applying benefit of both {@link org.gradle.api.Project#javaexec(Closure)} and
 * Worker API: provide larger Java heap to SpotBugs process and shorten their lifecycle.
 *
 * @see <a href="https://github.com/spotbugs/spotbugs-gradle-plugin/issues/416">The related GitHub
 *     issue</a>
 */
class SpotBugsRunnerForHybrid extends SpotBugsRunner {
  private final WorkerExecutor workerExecutor;
  private final Property<JavaLauncher> javaLauncher;

  public SpotBugsRunnerForHybrid(
      @NonNull WorkerExecutor workerExecutor, Property<JavaLauncher> javaLauncher) {
    this.workerExecutor = Objects.requireNonNull(workerExecutor);
    this.javaLauncher = javaLauncher;
  }

  @Override
  public void run(@NonNull SpotBugsTask task) {
    workerExecutor.noIsolation().submit(SpotBugsExecutor.class, configureWorkerSpec(task));
  }

  private Action<SpotBugsWorkParameters> configureWorkerSpec(SpotBugsTask task) {
    return params -> {
      List<String> args = new ArrayList<>();
      args.add("-exitcode");
      args.addAll(buildArguments(task));
      params.getClasspath().setFrom(task.getSpotbugsClasspath());
      params.getJvmArgs().set(buildJvmArguments(task));
      params.getArgs().set(args);
      String maxHeapSize = task.getMaxHeapSize().getOrNull();
      if (maxHeapSize != null) {
        params.getMaxHeapSize().set(maxHeapSize);
      }
      params.getIgnoreFailures().set(task.getIgnoreFailures());
      params.getShowStackTraces().set(task.getShowStackTraces());
      task.getEnabledReports().stream()
          .map(SpotBugsReport::getOutputLocation)
          .forEach(params.getReports()::add);
      if (javaLauncher.isPresent()) {
        params
            .getJavaToolchainExecutablePath()
            .set(javaLauncher.get().getExecutablePath().getAsFile().getAbsolutePath());
      }
    };
  }

  public interface SpotBugsWorkParameters extends WorkParameters {
    ConfigurableFileCollection getClasspath();

    Property<String> getMaxHeapSize();

    ListProperty<String> getArgs();

    ListProperty<String> getJvmArgs();

    Property<Boolean> getIgnoreFailures();

    Property<Boolean> getShowStackTraces();

    Property<String> getJavaToolchainExecutablePath();

    ListProperty<RegularFile> getReports();
  }

  /**
   * Exit code which is set when classes needed for analysis were missing.
   *
   * @see <a
   *     href="https://javadoc.io/static/com.github.spotbugs/spotbugs/4.4.2/constant-values.html#edu.umd.cs.findbugs.ExitCodes.MISSING_CLASS_FLAG">Constant
   *     Field Values from javadoc of the SpotBugs</a>
   */
  private static final int MISSING_CLASS_FLAG = 2;

  public abstract static class SpotBugsExecutor implements WorkAction<SpotBugsWorkParameters> {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ExecOperations execOperations;

    @Inject
    public SpotBugsExecutor(ExecOperations execOperations) {
      this.execOperations = Objects.requireNonNull(execOperations);
    }

    @Override
    public void execute() {
      // TODO print version of SpotBugs and Plugins
      SpotBugsWorkParameters params = getParameters();

      final int exitValue =
          execOperations.javaexec(configureJavaExec(params)).rethrowFailure().getExitValue();
      if (ignoreMissingClassFlag(exitValue) == 0) {
        return;
      }

      if (params.getIgnoreFailures().getOrElse(Boolean.FALSE)) {
        log.warn("SpotBugs ended with exit code " + exitValue);
        return;
      }

      String errorMessage = "Verification failed: SpotBugs ended with exit code " + exitValue + ".";
      List<String> reportPaths =
          params.getReports().get().stream()
              .map(RegularFile::getAsFile)
              .map(File::toPath)
              .map(Path::toUri)
              .map(URI::toString)
              .collect(Collectors.toList());
      if (!reportPaths.isEmpty()) {
        errorMessage += " See the report at: " + String.join(",", reportPaths);
      }
      throw new GradleException(errorMessage);
    }

    private int ignoreMissingClassFlag(int exitValue) {
      if ((exitValue & MISSING_CLASS_FLAG) == 0) {
        return exitValue;
      }
      log.debug(
          "MISSING_CLASS_FLAG (2) was set to the exit code, but ignore it to keep the task result stable.");
      return (exitValue ^ MISSING_CLASS_FLAG);
    }

    private Action<? super JavaExecSpec> configureJavaExec(SpotBugsWorkParameters params) {
      return spec -> {
        spec.setJvmArgs(params.getJvmArgs().get());
        spec.classpath(params.getClasspath());
        spec.setArgs(params.getArgs().get());
        spec.getMainClass().set("edu.umd.cs.findbugs.FindBugs2");
        String maxHeapSize = params.getMaxHeapSize().getOrNull();
        if (maxHeapSize != null) {
          spec.setMaxHeapSize(maxHeapSize);
        }
        if (params.getJavaToolchainExecutablePath().isPresent()) {
          log.info(
              "Spotbugs will be executed using Java Toolchain configuration: {}",
              params.getJavaToolchainExecutablePath().get());
          spec.setExecutable(params.getJavaToolchainExecutablePath().get());
        }
        spec.setIgnoreExitValue(true);
      };
    }
  }
}
