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
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.process.ExecOperations;
import org.gradle.process.JavaExecSpec;
import org.gradle.process.internal.ExecException;
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

  public SpotBugsRunnerForHybrid(@NonNull WorkerExecutor workerExecutor) {
    this.workerExecutor = Objects.requireNonNull(workerExecutor);
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
      params.getReportsDir().set(task.getReportsDir());
    };
  }

  public interface SpotBugsWorkParameters extends WorkParameters {
    ConfigurableFileCollection getClasspath();

    Property<String> getMaxHeapSize();

    ListProperty<String> getArgs();

    ListProperty<String> getJvmArgs();

    Property<Boolean> getIgnoreFailures();

    Property<Boolean> getShowStackTraces();

    DirectoryProperty getReportsDir();
  }

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
      try {
        execOperations.javaexec(configureJavaExec(params)).rethrowFailure().assertNormalExitValue();
      } catch (ExecException e) {
        if (params.getIgnoreFailures().getOrElse(Boolean.FALSE)) {
          log.warn(
              "SpotBugs reported failures",
              params.getShowStackTraces().getOrElse(Boolean.FALSE) ? e : null);
        } else {
          String errorMessage = "Verification failed: SpotBugs execution thrown exception.";
          List<String> reportPaths =
              params.getReportsDir().getAsFileTree().getFiles().stream()
                  .map(File::toPath)
                  .map(Path::toUri)
                  .map(URI::toString)
                  .collect(Collectors.toList());
          if (!reportPaths.isEmpty()) {
            errorMessage += "See the report at: " + String.join(",", reportPaths);
          }
          throw new GradleException(errorMessage, e);
        }
      }
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
      };
    }
  }
}
