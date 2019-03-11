package com.github.spotbugs.internal;

import java.io.File;
import java.util.Optional;

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.logging.Logger;
import org.gradle.api.reporting.internal.CustomizableHtmlReportImpl;
import org.gradle.api.resources.ResourceHandler;
import org.gradle.api.resources.TextResource;
import org.gradle.api.resources.TextResourceFactory;
import org.gradle.api.tasks.Input;

public class SpotBugsHtmlReportImpl extends CustomizableHtmlReportImpl {
    private static final long serialVersionUID = 6474874842199703745L;
    private final ResourceHandler handler;
    private final Configuration configuration;
    private final Logger logger;

    /**
     * Null-able string representing relative file path of XSL packaged in spotbugs.jar.
     */
    private String stylesheet;

    public SpotBugsHtmlReportImpl(String name, Task task) {
        super(name, task);
        handler = task.getProject().getResources();
        configuration = task.getProject().getConfigurations().getAt("spotbugs");
        logger = task.getLogger();
    }

    @Input
    public void setStylesheet(String fileName) {
      this.stylesheet = fileName;
    }

    @Override
    public TextResource getStylesheet() {
        if (stylesheet == null) {
            return super.getStylesheet();
        }

        TextResourceFactory factory = handler.getText();
        Optional<File> spotbugs = configuration.files(this::find).stream().findFirst();
        if (spotbugs.isPresent()) {
            File jar = spotbugs.get();
            logger.debug("Specified stylesheet ({}) found in spotbugs configuration: {}", stylesheet, jar.getAbsolutePath());
            return factory.fromArchiveEntry(jar, stylesheet);
        } else {
            throw new InvalidUserDataException("Specified stylesheet (" + stylesheet + ") does not found in spotbugs configuration");
        }
    }

    private boolean find(Dependency d) {
        return "com.github.spotbugs".equals(d.getGroup()) && "spotbugs".equals(d.getName());
    }
}
