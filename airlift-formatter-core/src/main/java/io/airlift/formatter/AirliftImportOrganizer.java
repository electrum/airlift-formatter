/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Organizes Java imports according to Airlift style.
 *
 * <p>Airlift import order:
 * <ol>
 *   <li>All other packages (sorted alphabetically)</li>
 *   <li>(blank line)</li>
 *   <li>javax.* packages (sorted alphabetically)</li>
 *   <li>(blank line)</li>
 *   <li>java.* packages (sorted alphabetically)</li>
 *   <li>(blank line)</li>
 *   <li>Static imports (sorted alphabetically)</li>
 * </ol>
 *
 * <p>Example:
 * <pre>{@code
 * AirliftImportOrganizer organizer = new AirliftImportOrganizer();
 * String organized = organizer.organizeImports(sourceCode);
 * }</pre>
 */
public class AirliftImportOrganizer
{
    // Pattern to match the entire import block (from first import to last import)
    private static final Pattern IMPORT_PATTERN = Pattern.compile(
            "^(import\\s+(?:static\\s+)?[\\w.]+(?:\\*)?;)\\s*$", Pattern.MULTILINE);

    // Pattern to identify static imports
    private static final Pattern STATIC_IMPORT_PATTERN = Pattern.compile(
            "^import\\s+static\\s+");

    // Pattern to identify javax imports
    private static final Pattern JAVAX_IMPORT_PATTERN = Pattern.compile(
            "^import\\s+javax\\.");

    // Pattern to identify java imports
    private static final Pattern JAVA_IMPORT_PATTERN = Pattern.compile(
            "^import\\s+java\\.");

    /**
     * Organizes imports in the source code according to Airlift style.
     *
     * @param source the Java source code
     * @return source code with organized imports
     */
    public String organizeImports(String source)
    {
        if (source == null || source.isEmpty()) {
            return source;
        }

        // Parse the source to find import section
        String[] lines = source.split("\n", -1);

        int importStart = -1;
        int importEnd = -1;
        List<String> allImports = new ArrayList<>();

        boolean inImportSection = false;
        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();

            if (trimmed.startsWith("import ")) {
                if (importStart == -1) {
                    importStart = i;
                }
                importEnd = i;
                allImports.add(trimmed);
                inImportSection = true;
            }
            else if (inImportSection) {
                // Allow blank lines within import section
                if (trimmed.isEmpty()) {
                    continue;
                }
                // Non-blank, non-import line ends the import section
                break;
            }
        }

        // No imports found
        if (allImports.isEmpty()) {
            return source;
        }

        // Categorize imports
        List<String> otherImports = new ArrayList<>();
        List<String> javaxImports = new ArrayList<>();
        List<String> javaImports = new ArrayList<>();
        List<String> staticImports = new ArrayList<>();

        for (String imp : allImports) {
            if (STATIC_IMPORT_PATTERN.matcher(imp).find()) {
                staticImports.add(imp);
            }
            else if (JAVAX_IMPORT_PATTERN.matcher(imp).find()) {
                javaxImports.add(imp);
            }
            else if (JAVA_IMPORT_PATTERN.matcher(imp).find()) {
                javaImports.add(imp);
            }
            else {
                otherImports.add(imp);
            }
        }

        // Sort each group alphabetically
        Comparator<String> importComparator = Comparator.comparing(this::getImportSortKey);
        otherImports.sort(importComparator);
        javaxImports.sort(importComparator);
        javaImports.sort(importComparator);
        staticImports.sort(importComparator);

        // Build the organized import block
        StringBuilder organizedImports = new StringBuilder();

        // Other imports (first group)
        for (String imp : otherImports) {
            organizedImports.append(imp).append("\n");
        }

        // javax imports (second group)
        if (!javaxImports.isEmpty()) {
            if (!otherImports.isEmpty()) {
                organizedImports.append("\n");
            }
            for (String imp : javaxImports) {
                organizedImports.append(imp).append("\n");
            }
        }

        // java imports (third group)
        if (!javaImports.isEmpty()) {
            if (!otherImports.isEmpty() || !javaxImports.isEmpty()) {
                organizedImports.append("\n");
            }
            for (String imp : javaImports) {
                organizedImports.append(imp).append("\n");
            }
        }

        // Static imports (fourth group)
        if (!staticImports.isEmpty()) {
            if (!otherImports.isEmpty() || !javaxImports.isEmpty() || !javaImports.isEmpty()) {
                organizedImports.append("\n");
            }
            for (String imp : staticImports) {
                organizedImports.append(imp).append("\n");
            }
        }

        // Rebuild source with organized imports
        StringBuilder result = new StringBuilder();

        // Add lines before imports
        for (int i = 0; i < importStart; i++) {
            result.append(lines[i]).append("\n");
        }

        // Add organized imports (without trailing newline - we'll handle that)
        String organizedStr = organizedImports.toString();
        if (organizedStr.endsWith("\n")) {
            organizedStr = organizedStr.substring(0, organizedStr.length() - 1);
        }
        result.append(organizedStr);

        // Add lines after imports
        for (int i = importEnd + 1; i < lines.length; i++) {
            result.append("\n").append(lines[i]);
        }

        return result.toString();
    }

    /**
     * Extracts the sort key from an import statement.
     * For static imports, removes the "static " prefix for sorting.
     */
    private String getImportSortKey(String importStatement)
    {
        String key = importStatement;

        // Remove "import " prefix
        if (key.startsWith("import static ")) {
            key = key.substring("import static ".length());
        }
        else if (key.startsWith("import ")) {
            key = key.substring("import ".length());
        }

        // Remove trailing semicolon
        if (key.endsWith(";")) {
            key = key.substring(0, key.length() - 1);
        }

        return key;
    }
}
