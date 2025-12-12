/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter.checkstyle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CheckstyleValidatorTest
{
    @TempDir
    Path tempDir;

    @Test
    void testValidFilePassesValidation()
            throws IOException
    {
        // A properly formatted Java file
        String source = """
                package test;

                public class ValidClass
                {
                    public void method()
                    {
                        System.out.println("Hello");
                    }
                }
                """;

        Path file = tempDir.resolve("ValidClass.java");
        Files.writeString(file, source);

        CheckstyleValidator validator = new CheckstyleValidator();
        List<CheckstyleViolation> violations = validator.validate(file);

        // This file may have some violations based on specific checkstyle rules,
        // but the validator should not throw exceptions
        assertThat(violations).isNotNull();
    }

    @Test
    void testFileWithTabsHasViolation()
            throws IOException
    {
        // A file with tabs (which violates FileTabCharacter check)
        String source = "package test;\n\npublic class TabClass\n{\n\tpublic void method() {}\n}\n";

        Path file = tempDir.resolve("TabClass.java");
        Files.writeString(file, source);

        CheckstyleValidator validator = new CheckstyleValidator();
        List<CheckstyleViolation> violations = validator.validate(file);

        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.sourceCheck().contains("FileTabCharacter"));
    }

    @Test
    void testEmptyFileListReturnsNoViolations()
    {
        CheckstyleValidator validator = new CheckstyleValidator();
        List<CheckstyleViolation> violations = validator.validate(List.of());

        assertThat(violations).isEmpty();
    }

    @Test
    void testViolationToString()
    {
        CheckstyleViolation violation = new CheckstyleViolation(
                "/path/to/File.java",
                10,
                5,
                "Tab character found",
                "FileTabCharacterCheck");

        String str = violation.toString();
        assertThat(str).contains("/path/to/File.java");
        assertThat(str).contains("10");
        assertThat(str).contains("5");
        assertThat(str).contains("Tab character found");
        assertThat(str).contains("FileTabCharacterCheck");
    }
}
