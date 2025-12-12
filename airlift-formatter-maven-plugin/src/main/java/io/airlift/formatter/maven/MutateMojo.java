/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter.maven;

import io.airlift.formatter.util.JavaSourceMutator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Randomly mutates Java source files in syntactically valid ways that only affect formatting.
 * This is useful for testing that the formatter produces consistent output regardless of input formatting.
 *
 * <p>The mutations include:
 * <ul>
 *   <li>Removing/adding spaces around keywords and operators</li>
 *   <li>Changing brace placement (same line vs new line)</li>
 *   <li>Adding/removing blank lines</li>
 *   <li>Messing up indentation</li>
 *   <li>Shuffling import order</li>
 * </ul>
 *
 * <p>Usage: Run this goal, then run the format goal, and verify the result matches the original.
 */
@Mojo(name = "mutate", defaultPhase = LifecyclePhase.NONE, threadSafe = true)
public class MutateMojo
        extends AbstractMojo
{
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Source directories to mutate.
     */
    @Parameter(defaultValue = "${project.compileSourceRoots}", property = "sourceDirectories")
    private List<String> sourceDirectories;

    /**
     * Test source directories to mutate.
     */
    @Parameter(defaultValue = "${project.testCompileSourceRoots}", property = "testSourceDirectories")
    private List<String> testSourceDirectories;

    /**
     * File patterns to include.
     */
    @Parameter(defaultValue = "**/*.java", property = "includes")
    private String[] includes;

    /**
     * File patterns to exclude.
     */
    @Parameter(property = "excludes")
    private String[] excludes;

    /**
     * Skip execution.
     */
    @Parameter(defaultValue = "false", property = "skip")
    private boolean skip;

    /**
     * Include test sources.
     */
    @Parameter(defaultValue = "true", property = "includeTestSources")
    private boolean includeTestSources;

    /**
     * Random seed for reproducible mutations. If not set, uses current time.
     */
    @Parameter(property = "seed")
    private Long seed;

    /**
     * Probability of applying each mutation (0.0 to 1.0).
     */
    @Parameter(defaultValue = "0.3", property = "mutationProbability")
    private double mutationProbability;

    /**
     * Maximum number of files to mutate. -1 for unlimited.
     */
    @Parameter(defaultValue = "-1", property = "maxFiles")
    private int maxFiles;

    /**
     * Backup original files with .orig extension.
     */
    @Parameter(defaultValue = "true", property = "backup")
    private boolean backup;

    private Random random;
    private JavaSourceMutator mutator;

    @Override
    public void execute()
            throws MojoExecutionException
    {
        if (skip) {
            getLog().info("Skipping mutation");
            return;
        }

        // Initialize random with seed
        long actualSeed = seed != null ? seed : System.currentTimeMillis();
        random = new Random(actualSeed);
        getLog().info("Using random seed: " + actualSeed);

        // Initialize mutator
        mutator = new JavaSourceMutator();

        List<Path> files = collectFiles();
        if (files.isEmpty()) {
            getLog().info("No files to mutate");
            return;
        }

        // Limit files if requested
        if (maxFiles > 0 && files.size() > maxFiles) {
            Collections.shuffle(files, random);
            files = files.subList(0, maxFiles);
        }

        getLog().info("Mutating " + files.size() + " files with probability " + mutationProbability);

        int mutatedCount = 0;
        int totalMutations = 0;

        for (Path file : files) {
            try {
                int mutations = mutateFile(file);
                if (mutations > 0) {
                    mutatedCount++;
                    totalMutations += mutations;
                    getLog().debug("Mutated " + file + " (" + mutations + " mutations)");
                }
            }
            catch (IOException e) {
                throw new MojoExecutionException("Failed to mutate file: " + file, e);
            }
        }

        getLog().info("Mutated " + mutatedCount + " files with " + totalMutations + " total mutations");
    }

    private int mutateFile(Path file)
            throws IOException
    {
        String original = Files.readString(file, StandardCharsets.UTF_8);

        if (backup) {
            Files.writeString(file.resolveSibling(file.getFileName() + ".orig"), original, StandardCharsets.UTF_8);
        }

        JavaSourceMutator.MutationResult result = mutator.mutateWithCount(original, random, mutationProbability);

        if (!result.getSource().equals(original)) {
            Files.writeString(file, result.getSource(), StandardCharsets.UTF_8);
        }

        return result.getMutationCount();
    }

    private List<Path> collectFiles()
    {
        List<Path> files = new ArrayList<>();

        for (String dir : sourceDirectories) {
            collectFilesFromDirectory(Path.of(dir), files);
        }

        if (includeTestSources && testSourceDirectories != null) {
            for (String dir : testSourceDirectories) {
                collectFilesFromDirectory(Path.of(dir), files);
            }
        }

        return files;
    }

    private void collectFilesFromDirectory(Path directory, List<Path> files)
    {
        if (!Files.exists(directory)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(directory)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> matchesIncludes(path) && !matchesExcludes(path))
                    .forEach(files::add);
        }
        catch (IOException e) {
            getLog().warn("Failed to scan directory: " + directory, e);
        }
    }

    private boolean matchesIncludes(Path path)
    {
        if (includes == null || includes.length == 0) {
            return true;
        }
        String pathStr = path.toString();
        for (String pattern : includes) {
            if (matchesGlob(pathStr, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesExcludes(Path path)
    {
        if (excludes == null || excludes.length == 0) {
            return false;
        }
        String pathStr = path.toString();
        for (String pattern : excludes) {
            if (matchesGlob(pathStr, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesGlob(String path, String pattern)
    {
        String regex = pattern
                .replace(".", "\\.")
                .replace("**/", "(.*/)?")
                .replace("*", "[^/]*")
                .replace("?", ".");
        return path.matches(".*" + regex);
    }
}
