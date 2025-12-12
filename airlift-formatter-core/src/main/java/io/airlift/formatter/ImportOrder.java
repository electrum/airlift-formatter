/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter;

import java.util.List;
import java.util.Objects;

/**
 * Defines the order in which imports should be organized.
 *
 * <p>Import order is defined as a list of {@link ImportGroup}s. Imports are
 * categorized into groups based on package prefix matching, with blank lines
 * inserted between groups.
 *
 * <h2>Predefined Orders</h2>
 * <ul>
 *   <li>{@link #AIRLIFT} - Airlift style: others, javax, java, static</li>
 *   <li>{@link #GOOGLE} - Google style: static first, then all others</li>
 *   <li>{@link #INTELLIJ_DEFAULT} - IntelliJ default: others, java, javax, static</li>
 * </ul>
 */
public final class ImportOrder
{
    /**
     * Airlift import order:
     * <ol>
     *   <li>All other packages</li>
     *   <li>javax.* packages</li>
     *   <li>java.* packages</li>
     *   <li>Static imports</li>
     * </ol>
     */
    public static final ImportOrder AIRLIFT = new ImportOrder(List.of(
            new ImportGroup("", false),      // All other
            new ImportGroup("javax", false), // javax.*
            new ImportGroup("java", false),  // java.*
            new ImportGroup("", true)));     // static

    /**
     * Google Java Style import order:
     * <ol>
     *   <li>Static imports</li>
     *   <li>All other packages (no separation)</li>
     * </ol>
     */
    public static final ImportOrder GOOGLE = new ImportOrder(List.of(
            new ImportGroup("", true),       // static first
            new ImportGroup("", false)));    // then all others

    /**
     * IntelliJ default import order:
     * <ol>
     *   <li>All other packages</li>
     *   <li>java.* packages</li>
     *   <li>javax.* packages</li>
     *   <li>Static imports</li>
     * </ol>
     */
    public static final ImportOrder INTELLIJ_DEFAULT = new ImportOrder(List.of(
            new ImportGroup("", false),      // All other
            new ImportGroup("java", false),  // java.* (note: different from Airlift)
            new ImportGroup("javax", false), // javax.*
            new ImportGroup("", true)));     // static

    private final List<ImportGroup> groups;

    /**
     * Creates a custom import order.
     *
     * @param groups the ordered list of import groups
     */
    public ImportOrder(List<ImportGroup> groups)
    {
        Objects.requireNonNull(groups, "groups is null");
        if (groups.isEmpty()) {
            throw new IllegalArgumentException("groups cannot be empty");
        }
        this.groups = List.copyOf(groups);
    }

    /**
     * Returns the ordered list of import groups.
     */
    public List<ImportGroup> getGroups()
    {
        return groups;
    }

    /**
     * Returns the number of groups (used for blank line separators).
     */
    public int getGroupCount()
    {
        return groups.size();
    }

    /**
     * Finds the group index for an import statement.
     *
     * @param importPackage the package being imported (e.g., "com.google.common.collect.ImmutableList")
     * @param isStatic whether the import is static
     * @return the index of the matching group, or -1 if no match
     */
    public int findGroupIndex(String importPackage, boolean isStatic)
    {
        // First pass: find exact prefix matches for the correct static type
        for (int i = 0; i < groups.size(); i++) {
            ImportGroup group = groups.get(i);
            if (group.isStatic() == isStatic && !group.prefix().isEmpty() && group.matches(importPackage)) {
                return i;
            }
        }

        // Second pass: find catch-all (empty prefix) for the correct static type
        for (int i = 0; i < groups.size(); i++) {
            ImportGroup group = groups.get(i);
            if (group.isStatic() == isStatic && group.prefix().isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ImportOrder that = (ImportOrder) o;
        return Objects.equals(groups, that.groups);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(groups);
    }

    @Override
    public String toString()
    {
        return "ImportOrder{groups=" + groups + "}";
    }
}
