/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter.maven;

import io.airlift.formatter.checkstyle.CheckstyleValidator;
import io.airlift.formatter.checkstyle.CheckstyleViolation;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Checks that Java source files comply with Airlift checkstyle rules.
 *
 * <p>This mojo validates that all Java files in the project's source directories
 * pass the Airlift checkstyle configuration. If any violations are found, the build fails.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * mvn airlift-formatter:check
 * }</pre>
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class CheckMojo
        extends AbstractMojo
{
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * List of source directories to check.
     * Defaults to the project's compile source roots.
     */
    @Parameter(property = "airlift.formatter.sourceDirectories")
    private List<String> sourceDirectories;

    /**
     * List of test source directories to check.
     * Defaults to the project's test compile source roots.
     */
    @Parameter(property = "airlift.formatter.testSourceDirectories")
    private List<String> testSourceDirectories;

    /**
     * File patterns to include. Supports glob patterns.
     * Default: {@code **&#47;*.java}
     */
    @Parameter(property = "airlift.formatter.includes", defaultValue = "**/*.java")
    private List<String> includes;

    /**
     * File patterns to exclude. Supports glob patterns.
     */
    @Parameter(property = "airlift.formatter.excludes")
    private List<String> excludes;

    /**
     * Skip check execution.
     */
    @Parameter(property = "airlift.formatter.skip", defaultValue = "false")
    private boolean skip;

    /**
     * Include test sources in check.
     */
    @Parameter(property = "airlift.formatter.includeTestSources", defaultValue = "true")
    private boolean includeTestSources;

    /**
     * Whether to fail the build if checkstyle violations are found.
     */
    @Parameter(property = "airlift.formatter.failOnViolation", defaultValue = "true")
    private boolean failOnViolation;

    @Override
    public void execute()
            throws MojoExecutionException, MojoFailureException
    {
        if (skip) {
            getLog().info("Airlift checkstyle check skipped");
            return;
        }

        List<Path> directories = collectSourceDirectories();

        if (directories.isEmpty()) {
            getLog().info("No source directories found");
            return;
        }

        List<Path> allJavaFiles = new ArrayList<>();
        for (Path directory : directories) {
            if (!Files.isDirectory(directory)) {
                getLog().debug("Skipping non-existent directory: " + directory);
                continue;
            }

            allJavaFiles.addAll(collectJavaFiles(directory));
        }

        if (allJavaFiles.isEmpty()) {
            getLog().info("No Java files found to check");
            return;
        }

        getLog().info("Checking " + allJavaFiles.size() + " files with Airlift checkstyle rules");

        CheckstyleValidator validator = new CheckstyleValidator();
        List<CheckstyleViolation> violations = validator.validate(allJavaFiles);

        if (violations.isEmpty()) {
            getLog().info("All " + allJavaFiles.size() + " files passed checkstyle validation");
            return;
        }

        // Group violations by file for better output
        Map<String, List<CheckstyleViolation>> violationsByFile = violations.stream()
                .collect(Collectors.groupingBy(CheckstyleViolation::filePath));

        getLog().warn("Found " + violations.size() + " checkstyle violation(s) in " +
                violationsByFile.size() + " file(s):");

        for (Map.Entry<String, List<CheckstyleViolation>> entry : violationsByFile.entrySet()) {
            getLog().warn("  " + entry.getKey());
            for (CheckstyleViolation violation : entry.getValue()) {
                getLog().warn("    " + violation.line() + ":" + violation.column() +
                        " " + violation.message() + " [" + violation.sourceCheck() + "]");
            }
        }

        if (failOnViolation) {
            throw new MojoFailureException(
                    "Found " + violations.size() + " checkstyle violation(s) in " +
                    violationsByFile.size() + " file(s).");
        }
    }

    /**
     * Collects all source directories to process.
     */
    protected List<Path> collectSourceDirectories()
    {
        List<Path> directories = new ArrayList<>();

        // Use configured source directories or fall back to project defaults
        if (sourceDirectories != null && !sourceDirectories.isEmpty()) {
            for (String dir : sourceDirectories) {
                directories.add(Path.of(dir));
            }
        }
        else if (project.getCompileSourceRoots() != null) {
            for (Object root : project.getCompileSourceRoots()) {
                directories.add(Path.of(root.toString()));
            }
        }

        // Include test sources if enabled
        if (includeTestSources) {
            if (testSourceDirectories != null && !testSourceDirectories.isEmpty()) {
                for (String dir : testSourceDirectories) {
                    directories.add(Path.of(dir));
                }
            }
            else if (project.getTestCompileSourceRoots() != null) {
                for (Object root : project.getTestCompileSourceRoots()) {
                    directories.add(Path.of(root.toString()));
                }
            }
        }

        return directories;
    }

    /**
     * Collects all Java files in a directory that match include/exclude patterns.
     */
    protected List<Path> collectJavaFiles(Path directory)
            throws MojoExecutionException
    {
        List<Path> files = new ArrayList<>();
        List<Pattern> includePatterns = compilePatterns(includes);
        List<Pattern> excludePatterns = excludes != null ? compilePatterns(excludes) : List.of();

        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                {
                    if (file.toString().endsWith(".java")) {
                        Path relativePath = directory.relativize(file);
                        String pathString = relativePath.toString().replace('\\', '/');

                        boolean included = includePatterns.isEmpty() ||
                                includePatterns.stream().anyMatch(p -> p.matcher(pathString).matches());
                        boolean excluded = excludePatterns.stream().anyMatch(p -> p.matcher(pathString).matches());

                        if (included && !excluded) {
                            files.add(file);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException e) {
            throw new MojoExecutionException("Error scanning directory: " + directory, e);
        }

        return files;
    }

    /**
     * Compiles glob patterns to regex patterns.
     */
    private List<Pattern> compilePatterns(List<String> globs)
    {
        if (globs == null) {
            return List.of();
        }

        return globs.stream()
                .map(this::globToRegex)
                .map(Pattern::compile)
                .toList();
    }

    /**
     * Converts a glob pattern to a regex pattern.
     */
    private String globToRegex(String glob)
    {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*':
                    if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                        regex.append(".*");
                        i++; // Skip next *
                        if (i + 1 < glob.length() && glob.charAt(i + 1) == '/') {
                            i++; // Skip following /
                        }
                    }
                    else {
                        regex.append("[^/]*");
                    }
                    break;
                case '?':
                    regex.append("[^/]");
                    break;
                case '.':
                case '(':
                case ')':
                case '[':
                case ']':
                case '{':
                case '}':
                case '+':
                case '^':
                case '$':
                case '|':
                case '\\':
                    regex.append('\\').append(c);
                    break;
                default:
                    regex.append(c);
            }
        }
        return regex.toString();
    }
}
