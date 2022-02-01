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
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.TextUICommandLine;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.workers.ProcessWorkerSpec;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkQueue;
import org.gradle.workers.WorkerExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpotBugsRunnerForWorker extends SpotBugsRunner {
  private final Logger log = LoggerFactory.getLogger(SpotBugsRunnerForWorker.class);
  private final WorkerExecutor workerExecutor;
  private final Property<JavaLauncher> javaLauncher;

  public SpotBugsRunnerForWorker(
      @NonNull WorkerExecutor workerExecutor, Property<JavaLauncher> javaLauncher) {
    this.workerExecutor = Objects.requireNonNull(workerExecutor);
    this.javaLauncher = javaLauncher;
  }

  @Override
  public void run(@NonNull SpotBugsTask task) {
    Objects.requireNonNull(task);

    WorkQueue workerQueue = workerExecutor.processIsolation(configureWorkerSpec(task));
    workerQueue.submit(SpotBugsExecutor.class, configureWorkParameters(task));
  }

  private Action<ProcessWorkerSpec> configureWorkerSpec(SpotBugsTask task) {
    return spec -> {
      spec.getClasspath().setFrom(task.getSpotbugsClasspath());
      spec.forkOptions(
          option -> {
            option.jvmArgs(buildJvmArguments(task));
            String maxHeapSize = task.getMaxHeapSize().getOrNull();
            if (maxHeapSize != null) {
              option.setMaxHeapSize(maxHeapSize);
            }
            if (javaLauncher.isPresent()) {
              log.info(
                  "Spotbugs will be executed using Java Toolchain configuration: Vendor: {} | Version: {}",
                  javaLauncher.get().getMetadata().getVendor(),
                  javaLauncher.get().getMetadata().getLanguageVersion().asInt());
              option.setExecutable(
                  javaLauncher.get().getExecutablePath().getAsFile().getAbsolutePath());
            }
          });
    };
  }

  private Action<SpotBugsWorkParameters> configureWorkParameters(SpotBugsTask task) {
    return params -> {
      params.getArguments().addAll(buildArguments(task));
      params.getIgnoreFailures().set(task.getIgnoreFailures());
      params.getShowStackTraces().set(task.getShowStackTraces());
      task.getEnabledReports().stream()
          .map(SpotBugsReport::getOutputLocation)
          .forEach(params.getReports()::add);
    };
  }

  interface SpotBugsWorkParameters extends WorkParameters {
    ListProperty<String> getArguments();

    Property<Boolean> getIgnoreFailures();

    Property<Boolean> getShowStackTraces();

    ListProperty<RegularFile> getReports();
  }

  public abstract static class SpotBugsExecutor implements WorkAction<SpotBugsWorkParameters> {
    private final Logger log = LoggerFactory.getLogger(SpotBugsExecutor.class);

    @Override
    public void execute() {
      SpotBugsWorkParameters params = getParameters();
      String[] args = params.getArguments().get().toArray(new String[0]);
      DetectorFactoryCollection.resetInstance(new DetectorFactoryCollection());

      try {
        edu.umd.cs.findbugs.Version.printVersion(false);
        try (FindBugs2 findBugs2 = new FindBugs2()) {
          TextUICommandLine commandLine = new TextUICommandLine();
          FindBugs.processCommandLine(commandLine, args, findBugs2);
          findBugs2.execute();

          StringBuilder message = new StringBuilder();
          if (findBugs2.getErrorCount() > 0) {
            message.append(findBugs2.getErrorCount()).append(" SpotBugs errors were found.");
          }
          if (findBugs2.getBugCount() > 0) {
            if (message.length() > 0) {
              message.append(' ');
            }
            message.append(findBugs2.getBugCount()).append(" SpotBugs violations were found.");
          }
          if (message.length() > 0) {
            List<String> reportPaths =
                params.getReports().get().stream()
                    .map(RegularFile::getAsFile)
                    .map(File::toPath)
                    .map(Path::toUri)
                    .map(URI::toString)
                    .collect(Collectors.toList());
            if (!reportPaths.isEmpty()) {
              message.append("See the report at: ").append(String.join(", ", reportPaths));
            }

            GradleException e = new GradleException(message.toString());

            if (params.getIgnoreFailures().getOrElse(Boolean.FALSE).booleanValue()) {
              log.warn(message.toString());
              if (params.getShowStackTraces().getOrElse(Boolean.FALSE).booleanValue()) {
                log.warn("", e);
              }
            } else {
              throw e;
            }
          }
        }
      } catch (GradleException e) {
        throw e;
      } catch (Exception e) {
        throw new GradleException("Verification failed: SpotBugs execution thrown exception", e);
      }
    }
  }
}
