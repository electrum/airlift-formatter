/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter.checkstyle;

/**
 * Represents a single checkstyle violation found during validation.
 *
 * @param filePath the path to the file containing the violation
 * @param line the line number where the violation occurred (1-based)
 * @param column the column number where the violation occurred (1-based)
 * @param message the violation message
 * @param sourceCheck the name of the checkstyle check that produced this violation
 */
public record CheckstyleViolation(
        String filePath,
        int line,
        int column,
        String message,
        String sourceCheck)
{
    @Override
    public String toString()
    {
        return String.format("%s:%d:%d: %s [%s]", filePath, line, column, message, sourceCheck);
    }
}
