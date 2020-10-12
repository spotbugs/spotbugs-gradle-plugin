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

import com.github.spotbugs.snom.internal.SpotBugsHtmlReport;
import com.github.spotbugs.snom.internal.SpotBugsRunnerForJavaExec;
import com.github.spotbugs.snom.internal.SpotBugsRunnerForWorker
import com.github.spotbugs.snom.internal.SpotBugsSarifReport;
import com.github.spotbugs.snom.internal.SpotBugsTextReport;
import com.github.spotbugs.snom.internal.SpotBugsXmlReport;
import edu.umd.cs.findbugs.annotations.NonNull
import edu.umd.cs.findbugs.annotations.Nullable;
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SkipWhenEmpty

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.VerificationTask;
import org.gradle.util.ClosureBackedAction;
import org.gradle.workers.WorkerExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory

import javax.inject.Inject

/**
 * The Gradle task to run the SpotBugs analysis. All properties are optional.
 *
 * <p><strong>Usage for Java project:</strong>
 * <p>After you apply the SpotBugs Gradle plugin to project, {@code SpotBugsTask} is automatically
 * generated for each sourceSet. If you want to configure generated tasks, write build scripts like below:<div><code>
 * spotbugsMain {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;sourceDirs = sourceSets.main.allSource.srcDirs<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;classDirs = sourceSets.main.output<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;auxClassPaths = sourceSets.main.compileClasspath<br>
 * <br>
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

@CacheableTask
class SpotBugsTask extends DefaultTask implements VerificationTask {
    private static final String FEATURE_FLAG_WORKER_API = "com.github.spotbugs.snom.worker";
    private final Logger log = LoggerFactory.getLogger(SpotBugsTask);

    private final WorkerExecutor workerExecutor;

    @NonNull final Property<Boolean> ignoreFailures;
    @NonNull final Property<Boolean> showStackTraces;
    /**
     * Property to enable progress reporting during the analysis. Default value is {@code false}.
     */
    @Input
    @Optional
    @NonNull
    final Property<Boolean> showProgress;
    /**
     * Property to specify the level to report bugs. Default value is {@link Confidence#DEFAULT}.
     */
    @Input
    @Optional
    @NonNull
    final Property<Confidence> reportLevel;
    /**
     * Property to adjust SpotBugs detectors. Default value is {@link Effort#DEFAULT}.
     */
    @Input
    @Optional
    @NonNull
    final Property<Effort> effort;
    /**
     * Property to enable visitors (detectors) for analysis. Default is empty that means all visitors run analysis.
     */
    @Input
    @NonNull
    final ListProperty<String> visitors;
    /**
     * Property to disable visitors (detectors) for analysis. Default is empty that means SpotBugs omits no visitor.
     */
    @Input
    @NonNull
    final ListProperty<String> omitVisitors;

    /**
     * Property to set the directory to generate report files. Default is {@code "$buildDir/reports/spotbugs/$taskName"}.
     */
    @Internal("Refer the destination of each report instead.")
    @NonNull
    final DirectoryProperty reportsDir;

    /**
     * Property to specify which report you need.
     *
     * @see SpotBugsReport
     */
    @Internal
    @NonNull
    final NamedDomainObjectContainer<SpotBugsReport> reports;

    /**
     * Property to set the filter file to limit which bug should be reported.
     *
     * <p>Note that this property will NOT limit which bug should be detected. To limit the target classes to analyze, use {@link #onlyAnalyze} instead.
     * To limit the visitors (detectors) to run, use {@link #visitors} and {@link #omitVisitors} instead.</p>
     *
     * <p>See also <a href="https://spotbugs.readthedocs.io/en/stable/filter.html">SpotBugs Manual about Filter file</a>.</p>
     */
    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
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
    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    @NonNull
    final RegularFileProperty excludeFilter;
    /**
     * Property to set the baseline file. This file is a Spotbugs result file, and all bugs reported in this file will not be
     * reported in the final output.
     */
    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    @NonNull
    final RegularFileProperty baselineFile;
    /**
     * Property to specify the target classes for analysis. Default value is empty that means all classes are analyzed.
     */
    @Input
    @NonNull
    final ListProperty<String> onlyAnalyze;
    /**
     * Property to specify the name of project. Some reporting formats use this property.
     * Default value is {@code "${project.name} (${task.name})"}.
     * <br>
     * Note that this property, if treated as a task input, can break cacheability.<br>
     * As such, it has been marked {@link Internal} to exclude it from task up-to-date and
     * cacheability checks.
     */
    @Internal
    @NonNull
    final Property<String> projectName;
    /**
     * Property to specify the release identifier of project. Some reporting formats use this property. Default value is the version of your Gradle project.
     */
    @Input
    @NonNull
    final Property<String> release;
    /**
     * Property to specify the extra arguments for SpotBugs. Default value is empty so SpotBugs will get no extra argument.
     */
    @Optional
    @Input
    @NonNull
    final ListProperty<String> extraArgs;
    /**
     * Property to specify the extra arguments for JVM process. Default value is empty so JVM process will get no extra argument.
     */
    @Optional
    @Input
    @NonNull
    final ListProperty<String> jvmArgs;
    /**
     * Property to specify the max heap size ({@code -Xmx} option) of JVM process.
     * Default value is empty so the default configuration made by Gradle will be used.
     */
    @Optional
    @Input
    @NonNull
    final Property<String> maxHeapSize;
    /**
     * Property to specify the directories that contain the source of target classes to analyze.
     * Default value is the source directory of the target sourceSet.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    FileCollection sourceDirs;

    /**
     * Property to specify the directories that contains the target classes to analyze.
     * Default value is the output directory of the target sourceSet.
     */
    @Internal
    FileCollection classDirs;

    /**
     * Property to specify the aux class paths that contains the libraries to refer during analysis.
     * Default value is the compile-scope dependencies of the target sourceSet.
     */
    @Classpath
    FileCollection auxClassPaths;

    /**
     * Property to enable auxclasspathFromFile and prevent Argument List Too Long issues in java processes.
     * Default value is {@code false}.
     */
    @Input
    @Optional
    @NonNull
    final Property<Boolean> useAuxclasspathFile

    private FileCollection classes;

    void setClasses(FileCollection fileCollection) {
        this.classes = fileCollection
    }

    /**
     * Property to specify the target classes to analyse by SpotBugs.
     * Default value is the all existing {@code .class} files in {@link #getClassDirs}.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    @SkipWhenEmpty
    @NonNull
    FileCollection getClasses() {
        if (classes == null) {
            if (getClassDirs() == null) {
                throw new InvalidUserDataException("The classDirs property is not set")
            }
            return getClassDirs().asFileTree.filter({ File file ->
                file.name.endsWith(".class")
            })
        } else {
            return classes
        }
    }

    @Inject
    SpotBugsTask(ObjectFactory objects, WorkerExecutor workerExecutor) {
        this.workerExecutor = Objects.requireNonNull(workerExecutor);

        sourceDirs = objects.fileCollection()
        auxClassPaths = objects.fileCollection()
        ignoreFailures = objects.property(Boolean)
        showStackTraces = objects.property(Boolean)
        showProgress = objects.property(Boolean);
        reportLevel = objects.property(Confidence);
        effort = objects.property(Effort);
        visitors = objects.listProperty(String);
        omitVisitors = objects.listProperty(String);
        reportsDir = objects.directoryProperty()
        reports =
                objects.domainObjectContainer(
                SpotBugsReport, {name ->
                    switch (name) {
                        case "html":
                            return objects.newInstance(SpotBugsHtmlReport.class, objects, this)
                        case "xml":
                            return objects.newInstance(SpotBugsXmlReport.class, objects, this)
                        case "text":
                            return objects.newInstance(SpotBugsTextReport.class, objects, this)
                        case "sarif":
                            return objects.newInstance(SpotBugsSarifReport.class, objects, this)
                        default:
                            throw new InvalidUserDataException(name + " is invalid as the report name");
                    }
                });
        includeFilter = objects.fileProperty()
        excludeFilter = objects.fileProperty()
        baselineFile = objects.fileProperty()
        onlyAnalyze = objects.listProperty(String);
        projectName = objects.property(String);
        release = objects.property(String);
        jvmArgs = objects.listProperty(String);
        extraArgs = objects.listProperty(String);
        maxHeapSize = objects.property(String);
        useAuxclasspathFile = objects.property(Boolean)
    }

    /**
     * Set properties from extension right after the task creation. User may overwrite these
     * properties by build script.
     *
     * @param extension the source extension to copy the properties.
     */
    void init(SpotBugsExtension extension) {
        ignoreFailures.convention(extension.ignoreFailures)
        showStackTraces.convention(extension.showStackTraces)
        showProgress.convention(extension.showProgress)
        reportLevel.convention(extension.reportLevel)
        effort.convention(extension.effort)
        visitors.convention(extension.visitors)
        omitVisitors.convention(extension.omitVisitors)
        // the default reportsDir is "$buildDir/reports/spotbugs/"
        reportsDir.convention(extension.reportsDir)
        includeFilter.convention(extension.includeFilter)
        excludeFilter.convention(extension.excludeFilter)
        baselineFile.convention(extension.baselineFile)
        onlyAnalyze.convention(extension.onlyAnalyze)
        projectName.convention(extension.projectName.map({p -> String.format("%s (%s)", p, getName())}))
        release.convention(extension.release)
        jvmArgs.convention(extension.jvmArgs)
        extraArgs.convention(extension.extraArgs)
        maxHeapSize.convention(extension.maxHeapSize)
        useAuxclasspathFile.convention(extension.useAuxclasspathFile)
    }

    @TaskAction
    void run() {
        if (getProject().hasProperty(FEATURE_FLAG_WORKER_API)
        && getProject()
        .property(FEATURE_FLAG_WORKER_API)
        .toString() == "false") {
            log.info("Running SpotBugs by JavaExec...");
            new SpotBugsRunnerForJavaExec().run(this);
        } else {
            log.info("Running SpotBugs by Gradle Worker...");
            new SpotBugsRunnerForWorker(workerExecutor).run(this);
        }
    }

    final NamedDomainObjectContainer<? extends SpotBugsReport> reports(
            Closure<NamedDomainObjectContainer<? extends SpotBugsReport>> closure) {
        return reports(
                new ClosureBackedAction<NamedDomainObjectContainer<? extends SpotBugsReport>>(closure))
    }

    final NamedDomainObjectContainer<? extends SpotBugsReport> reports(
            Action<NamedDomainObjectContainer<? extends SpotBugsReport>> configureAction) {
        configureAction.execute(reports)
        return reports
    }

    @NonNull
    @Internal
    Set<File> getPluginJar() {
        return getProject().getConfigurations().getByName(SpotBugsPlugin.PLUGINS_CONFIG_NAME).getFiles()
    }

    @NonNull
    @Internal
    FileCollection getSpotbugsClasspath() {
        Configuration config = getProject().getConfigurations().getByName(SpotBugsPlugin.CONFIG_NAME)
        Configuration spotbugsSlf4j = getProject().getConfigurations().getByName(SpotBugsPlugin.SLF4J_CONFIG_NAME)

        return getProject().files(config, spotbugsSlf4j)
    }

    @Nullable
    @Optional
    @Nested
    SpotBugsReport getFirstEnabledReport() {
        // use XML report by default, only when SpotBugs plugin is applied
        boolean isSpotBugsPluingApplied = project.pluginManager.hasPlugin("com.github.spotbugs")

        java.util.Optional<SpotBugsReport> report = reports.stream().filter({ report -> report.enabled}).findFirst()
        if (isSpotBugsPluingApplied) {
            return report.orElse(reports.create("xml"))
        } else {
            return report.orElse(null)
        }
    }

    void setReportLevel(@Nullable String name) {
        Confidence confidence = name == null ? null : Confidence.valueOf(name.toUpperCase())
        getReportLevel().set(confidence)
    }

    void setEffort(@Nullable String name) {
        Effort effort = name == null ? null : Effort.valueOf(name.toUpperCase())
        getEffort().set(effort)
    }

    void setIgnoreFailures(Provider<Boolean> b) {
        ignoreFailures.set(b);
    }

    void setIgnoreFailures(boolean b) {
        ignoreFailures.set(b);
    }

    void setShowStackTraces(Provider<Boolean> b) {
        showStackTraces.set(b);
    }

    void setShowStackTraces(boolean b) {
        showStackTraces.set(b)
    }

    @Input
    boolean getIgnoreFailures() {
        ignoreFailures.get();
    }

    @Input
    boolean getShowStackTraces() {
        showStackTraces.get();
    }

    @Internal
    String getBaseName() {
        String prunedName = name.replaceFirst("spotbugs", "")
        if (prunedName.isEmpty()) {
            prunedName = task.getName()
        }
        return new StringBuilder().append(Character.toLowerCase(prunedName.charAt(0))).append(prunedName.substring(1)).toString()
    }
}
