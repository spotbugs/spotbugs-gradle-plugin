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

import edu.umd.cs.findbugs.annotations.NonNull
import edu.umd.cs.findbugs.annotations.Nullable
import org.gradle.api.Action
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.reporting.ReportingExtension

import javax.inject.Inject;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/**
 * The extension to configure the SpotBugs Gradle plugin. Most of properties in this extension will be used as the default property of all {@link SpotBugsTask}.
 * All properties are optional.
 *
 * <p><strong>Usage:</strong>
 * <p>After you apply the SpotBugs Gradle plugin to project, write extension like below:<div><code>
 * spotbugs {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;ignoreFailures = false<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;showStackTraces = true<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;showProgress = false<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;reportLevel = 'default'<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;effort = 'default'<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;visitors = [ 'FindSqlInjection', 'SwitchFallthrough' ]<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;omitVisitors = [ 'FindNonShortCircuit' ]<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;reportsDir = file("$buildDir/reports/spotbugs")<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;includeFilter = file('spotbugs-include.xml')<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;excludeFilter = file('spotbugs-exclude.xml')<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;onlyAnalyze = ['com.foobar.MyClass', 'com.foobar.mypkg.*']<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;projectName = name<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;release = version<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;extraArgs = [ '-nested:false' ]<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;jvmArgs = [ '-Duser.language=ja' ]<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;maxHeapSize = '512m'<br>
 *}</code></div>
 *
 * <p>See also <a href="https://spotbugs.readthedocs.io/en/stable/running.html">SpotBugs Manual about configuration</a>.</p>
 */
class SpotBugsExtension {
    static final String DEFAULT_REPORTS_DIR_NAME = "spotbugs";

    @NonNull
    final Property<Boolean> ignoreFailures;
    @NonNull
    final Property<Boolean> showStackTraces;
    /**
     * Property to enable progress reporting during the analysis. Default value is {@code false}.
     */
    @NonNull
    final Property<Boolean> showProgress;
    /**
     * Property to specify the level to report bugs. Default value is {@link Confidence#DEFAULT}.
     */
    @NonNull
    final Property<Confidence> reportLevel;
    /**
     * Property to adjust SpotBugs detectors. Default value is {@link Effort#DEFAULT}.
     */
    @NonNull
    final Property<Effort> effort;
    /**
     * Property to enable visitors (detectors) for analysis. Default is empty that means all visitors run analysis.
     */
    @NonNull
    final ListProperty<String> visitors;
    /**
     * Property to disable visitors (detectors) for analysis. Default is empty that means SpotBugs omits no visitor.
     */
    @NonNull
    final ListProperty<String> omitVisitors;
    /**
     * Property to set the directory to generate report files. Default is {@code "$buildDir/reports/spotbugs"}.
     *
     * <p>Note that each {@link SpotBugsTask} creates own sub-directory in this directory.</p>
     */
    @NonNull
    final DirectoryProperty reportsDir;
    /**
     * Property to set the filter file to limit which bug should be reported.
     *
     * <p>Note that this property will NOT limit which bug should be detected. To limit the target classes to analyze, use {@link #onlyAnalyze} instead.
     * To limit the visitors (detectors) to run, use {@link #visitors} and {@link #omitVisitors} instead.</p>
     *
     * <p>See also <a href="https://spotbugs.readthedocs.io/en/stable/filter.html">SpotBugs Manual about Filter file</a>.</p>
     */
    @NonNull
    final RegularFileProperty includeFilter;
    /**
     * Property to set the filter file to limit which bug should be reported.
     *
     * <p>Note that this property will NOT limit which bug should be detected. To limit the target classes to analyze, use {@link #onlyAnalyze} instead.
     * To limit the visitors (detectors) to run, use {@link #visitors} and {@link #omitVisitors} instead.</p>
     *
     * <p>See also <a href="https://spotbugs.readthedocs.io/en/stable/filter.html">SpotBugs Manual about Filter file</a>.</p>
     */
    @NonNull
    final RegularFileProperty excludeFilter;
    /**
     * Property to set the baseline file. This file is a Spotbugs result file, and all bugs reported in this file will not be
     * reported in the final output.
     */
    @NonNull
    final RegularFileProperty baselineFile;
    /**
     * Property to specify the target classes for analysis. Default value is empty that means all classes are analyzed.
     */
    @NonNull
    final ListProperty<String> onlyAnalyze;
    /**
     * Property to specify the name of project. Some reporting formats use this property. Default value is the name of your Gradle project.
     */
    @NonNull
    final Property<String> projectName;
    /**
     * Property to specify the release identifier of project. Some reporting formats use this property. Default value is the version of your Gradle project.
     */
    @NonNull
    final Property<String> release;
    /**
     * Property to specify the extra arguments for SpotBugs. Default value is empty so SpotBugs will get no extra argument.
     */
    @NonNull
    final ListProperty<String> extraArgs;
    /**
     * Property to specify the extra arguments for JVM process. Default value is empty so JVM process will get no extra argument.
     */
    @NonNull
    final ListProperty<String> jvmArgs;
    /**
     * Property to specify the max heap size ({@code -Xmx} option) of JVM process.
     * Default value is empty so the default configuration made by Gradle will be used.
     */
    @NonNull
    final Property<String> maxHeapSize;

    @NonNull
    final Property<String> toolVersion

    @NonNull
    final Property<Boolean> useAuxclasspathFile;

    @Inject
    SpotBugsExtension(Project project, ObjectFactory objects) {
        ignoreFailures = objects.property(Boolean).convention(false);
        showStackTraces = objects.property(Boolean).convention(true);
        showProgress = objects.property(Boolean);
        reportLevel = objects.property(Confidence);
        effort = objects.property(Effort);
        visitors = objects.listProperty(String);
        omitVisitors = objects.listProperty(String);
        reportsDir = objects.directoryProperty()
        includeFilter = objects.fileProperty()
        excludeFilter = objects.fileProperty()
        baselineFile = objects.fileProperty()
        onlyAnalyze = objects.listProperty(String);
        projectName = objects.property(String);
        release = objects.property(String);
        configureFromProject(project, { p ->
            projectName.convention(p.getName())
            release.convention(p.getVersion().toString())
        })

        // ReportingBasePlugin should be applied before we create this SpotBugsExtension instance
        DirectoryProperty baseReportsDir = project.extensions.getByType(ReportingExtension).baseDirectory
        reportsDir = objects.directoryProperty().convention(baseReportsDir.map({
            it.dir(DEFAULT_REPORTS_DIR_NAME)
        }))

        jvmArgs = objects.listProperty(String);
        extraArgs = objects.listProperty(String);
        maxHeapSize = objects.property(String);
        toolVersion = objects.property(String)
        useAuxclasspathFile = objects.property(Boolean).convention(false)
    }

    void setReportLevel(@Nullable String name) {
        Confidence confidence = name == null ? null : Confidence.valueOf(name.toUpperCase())
        getReportLevel().set(confidence)
    }

    void setEffort(@Nullable String name) {
        Effort effort = name == null ? null : Effort.valueOf(name.toUpperCase())
        getEffort().set(effort)
    }

    private void configureFromProject(Project project, Action<Project> action) {
        if (project.state.executed && project.rootProject.state.executed) {
            action.execute project
        } else if (!project.rootProject.state.executed) {
            project.rootProject.afterEvaluate action
        } else {
            project.afterEvaluate action
        }
    }
}
