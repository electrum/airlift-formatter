/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter.checkstyle;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;

import org.xml.sax.InputSource;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Validates Java source files against Airlift checkstyle rules.
 *
 * <p>This class uses the Checkstyle library programmatically to validate
 * Java source files against the bundled airbase-checks.xml configuration.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * CheckstyleValidator validator = new CheckstyleValidator();
 * List<CheckstyleViolation> violations = validator.validate(List.of(
 *     Path.of("src/main/java/MyClass.java"),
 *     Path.of("src/main/java/OtherClass.java")
 * ));
 * for (CheckstyleViolation violation : violations) {
 *     System.out.println(violation);
 * }
 * }</pre>
 */
public class CheckstyleValidator
{
    private static final String DEFAULT_CONFIG_LOCATION = "/checkstyle/airbase-checks.xml";

    private final Configuration configuration;

    /**
     * Creates a new CheckstyleValidator using the default airbase-checks.xml configuration.
     *
     * @throws CheckstyleValidationException if the configuration cannot be loaded
     */
    public CheckstyleValidator()
    {
        this(DEFAULT_CONFIG_LOCATION);
    }

    /**
     * Creates a new CheckstyleValidator using a custom configuration from the classpath.
     *
     * @param configClasspathLocation the classpath location of the checkstyle configuration
     * @throws CheckstyleValidationException if the configuration cannot be loaded
     */
    public CheckstyleValidator(String configClasspathLocation)
    {
        try {
            InputStream configStream = getClass().getResourceAsStream(configClasspathLocation);
            if (configStream == null) {
                throw new CheckstyleValidationException(
                        "Checkstyle configuration not found on classpath: " + configClasspathLocation);
            }
            InputSource inputSource = new InputSource(configStream);
            this.configuration = ConfigurationLoader.loadConfiguration(
                    inputSource,
                    new PropertiesExpander(new Properties()),
                    ConfigurationLoader.IgnoredModulesOptions.OMIT);
        }
        catch (CheckstyleException e) {
            throw new CheckstyleValidationException("Failed to load checkstyle configuration", e);
        }
    }

    /**
     * Validates the given files against the checkstyle configuration.
     *
     * @param files the Java source files to validate
     * @return a list of violations found, empty if all files pass validation
     * @throws CheckstyleValidationException if validation fails due to an internal error
     */
    public List<CheckstyleViolation> validate(List<Path> files)
    {
        if (files.isEmpty()) {
            return List.of();
        }

        List<CheckstyleViolation> violations = new ArrayList<>();
        ViolationCollector collector = new ViolationCollector(violations);

        Checker checker = new Checker();
        try {
            checker.setModuleClassLoader(Thread.currentThread().getContextClassLoader());
            checker.configure(configuration);
            checker.addListener(collector);

            List<File> fileList = files.stream()
                    .map(Path::toFile)
                    .toList();

            checker.process(fileList);
        }
        catch (CheckstyleException e) {
            throw new CheckstyleValidationException("Checkstyle validation failed", e);
        }
        finally {
            checker.destroy();
        }

        return violations;
    }

    /**
     * Validates a single file against the checkstyle configuration.
     *
     * @param file the Java source file to validate
     * @return a list of violations found, empty if the file passes validation
     * @throws CheckstyleValidationException if validation fails due to an internal error
     */
    public List<CheckstyleViolation> validate(Path file)
    {
        return validate(List.of(file));
    }

    /**
     * AuditListener implementation that collects violations into a list.
     */
    private static class ViolationCollector
            implements AuditListener
    {
        private final List<CheckstyleViolation> violations;

        ViolationCollector(List<CheckstyleViolation> violations)
        {
            this.violations = violations;
        }

        @Override
        public void auditStarted(AuditEvent event)
        {
            // No action needed
        }

        @Override
        public void auditFinished(AuditEvent event)
        {
            // No action needed
        }

        @Override
        public void fileStarted(AuditEvent event)
        {
            // No action needed
        }

        @Override
        public void fileFinished(AuditEvent event)
        {
            // No action needed
        }

        @Override
        public void addError(AuditEvent event)
        {
            violations.add(new CheckstyleViolation(
                    event.getFileName(),
                    event.getLine(),
                    event.getColumn(),
                    event.getMessage(),
                    extractCheckName(event.getSourceName())));
        }

        @Override
        public void addException(AuditEvent event, Throwable throwable)
        {
            violations.add(new CheckstyleViolation(
                    event.getFileName(),
                    0,
                    0,
                    "Exception during validation: " + throwable.getMessage(),
                    "CheckstyleException"));
        }

        /**
         * Extracts the simple check name from the fully qualified source name.
         * For example: "com.puppycrawl.tools.checkstyle.checks.whitespace.WhitespaceAfterCheck"
         * becomes "WhitespaceAfterCheck"
         */
        private String extractCheckName(String sourceName)
        {
            if (sourceName == null) {
                return "Unknown";
            }
            int lastDot = sourceName.lastIndexOf('.');
            return lastDot >= 0 ? sourceName.substring(lastDot + 1) : sourceName;
        }
    }
}
