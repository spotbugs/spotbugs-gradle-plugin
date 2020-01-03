/*
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

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.nio.file.Paths;
import javax.inject.Inject;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

class SpotBugsExtension {
    @NonNull final Property<Boolean> ignoreFailures;
    @NonNull final Property<Boolean> showProgress;
    @NonNull final Property<Confidence> reportLevel;
    @NonNull final Property<Effort> effort;
    @NonNull final ListProperty<String> visitors;
    @NonNull final ListProperty<String> omitVisitors;
    @NonNull final Property<File> reportsDir;
    @NonNull final Property<File> includeFilter;
    @NonNull final Property<File> excludeFilter;
    @NonNull final ListProperty<String> onlyAnalyze;
    @NonNull final Property<String> projectName;
    @NonNull final Property<String> release;
    @NonNull final ListProperty<String> extraArgs;
    @NonNull final ListProperty<String> jvmArgs;
    @NonNull final Property<String> maxHeapSize;

    @Inject
    SpotBugsExtension(Project project, ObjectFactory objects) {
        ignoreFailures = objects.property(Boolean);
        showProgress = objects.property(Boolean);
        reportLevel = objects.property(Confidence);
        effort = objects.property(Effort);
        visitors = objects.listProperty(String);
        omitVisitors = objects.listProperty(String);
        // the default reportsDir is "$buildDir/reports/spotbugs"
        File reports = new File(project.buildDir, "reports")
        reportsDir = objects.property(File).convention(new File(reports, "spotbugs"));
        includeFilter = objects.property(File);
        excludeFilter = objects.property(File);
        onlyAnalyze = objects.listProperty(String);
        projectName = objects.property(String);
        release = objects.property(String);
        project.afterEvaluate(
                {p ->
                    projectName.convention(p.getName());
                    release.convention(p.getVersion().toString());
                });
        jvmArgs = objects.listProperty(String);
        extraArgs = objects.listProperty(String);
        maxHeapSize = objects.property(String);
    }
}
