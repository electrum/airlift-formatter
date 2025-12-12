/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter.maven;

import io.airlift.formatter.AirliftFormatter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Formats Java source files using Airlift style formatting.
 *
 * <p>This mojo formats all Java files in the project's source directories,
 * applying Airlift-style formatting rules.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * mvn airlift-formatter:format
 * }</pre>
 */
@Mojo(name = "format", defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true)
public class FormatMojo
        extends AbstractMojo
{
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * List of source directories to format.
     * Defaults to the project's compile source roots.
     */
    @Parameter(property = "airlift.formatter.sourceDirectories")
    private List<String> sourceDirectories;

    /**
     * List of test source directories to format.
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
     * Skip formatting execution.
     */
    @Parameter(property = "airlift.formatter.skip", defaultValue = "false")
    private boolean skip;

    /**
     * Include test sources in formatting.
     */
    @Parameter(property = "airlift.formatter.includeTestSources", defaultValue = "true")
    private boolean includeTestSources;

    @Override
    public void execute()
            throws MojoExecutionException
    {
        if (skip) {
            getLog().info("Airlift formatter skipped");
            return;
        }

        List<Path> directories = collectSourceDirectories();

        if (directories.isEmpty()) {
            getLog().info("No source directories found");
            return;
        }

        AirliftFormatter formatter = new AirliftFormatter();
        int filesProcessed = 0;
        int filesFormatted = 0;

        for (Path directory : directories) {
            if (!Files.isDirectory(directory)) {
                getLog().debug("Skipping non-existent directory: " + directory);
                continue;
            }

            List<Path> javaFiles = collectJavaFiles(directory);

            for (Path file : javaFiles) {
                try {
                    filesProcessed++;
                    if (formatFile(formatter, file)) {
                        filesFormatted++;
                        getLog().info("Formatted: " + file);
                    }
                }
                catch (IOException e) {
                    throw new MojoExecutionException("Error formatting file: " + file, e);
                }
            }
        }

        getLog().info("Processed " + filesProcessed + " files, formatted " + filesFormatted);
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
     * Formats a single file, returning true if the file was modified.
     */
    protected boolean formatFile(AirliftFormatter formatter, Path file)
            throws IOException
    {
        String original = Files.readString(file, StandardCharsets.UTF_8);
        String formatted = formatter.format(original);

        if (!original.equals(formatted)) {
            Files.writeString(file, formatted, StandardCharsets.UTF_8);
            return true;
        }

        return false;
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
