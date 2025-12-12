/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter;

import java.util.Objects;

/**
 * Represents a group of imports with a common package prefix.
 *
 * <p>Used to define import ordering rules, where imports matching the prefix
 * are grouped together.
 *
 * @param prefix the package prefix (empty string matches all packages)
 * @param isStatic whether this group is for static imports
 */
public record ImportGroup(String prefix, boolean isStatic)
{
    public ImportGroup
    {
        Objects.requireNonNull(prefix, "prefix is null");
    }

    /**
     * Checks if an import statement matches this group.
     *
     * @param importStatement the import statement (without "import " prefix)
     * @return true if the import matches this group
     */
    public boolean matches(String importStatement)
    {
        if (prefix.isEmpty()) {
            return true; // Empty prefix matches everything
        }
        return importStatement.startsWith(prefix + ".");
    }
}
