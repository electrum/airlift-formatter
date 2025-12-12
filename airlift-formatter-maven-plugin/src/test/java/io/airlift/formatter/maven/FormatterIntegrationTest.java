/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter.maven;

import io.airlift.formatter.AirliftFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the formatter Maven plugin mojos.
 */
class FormatterIntegrationTest
{
    @TempDir
    Path tempDir;

    @Test
    void testFormatterChangesUnformattedFile()
            throws IOException
    {
        // Create an unformatted file (missing space before control flow parens)
        String unformatted = """
                package test;

                public class Test {
                    public void method() {
                        if(true){
                            System.out.println("hello");
                        }
                    }
                }
                """;

        Path javaFile = tempDir.resolve("Test.java");
        Files.writeString(javaFile, unformatted, StandardCharsets.UTF_8);

        // Format it
        AirliftFormatter formatter = new AirliftFormatter();
        String formatted = formatter.format(unformatted);

        // Verify it changed (at least whitespace was fixed)
        assertThat(formatted).isNotEqualTo(unformatted);
        // Verify space was added before parenthesis in if statement
        assertThat(formatted).contains("if (true)");
    }

    @Test
    void testFormatterPreservesAlreadyFormattedFile()
            throws IOException
    {
        // Create an already-formatted file (standard style with proper spacing)
        String formatted = """
                package test;

                public class Test {
                    public void method() {
                        if (true) {
                            System.out.println("hello");
                        }
                    }
                }
                """;

        // Format it again
        AirliftFormatter formatter = new AirliftFormatter();
        String reformatted = formatter.format(formatted);

        // Verify it didn't change (idempotent)
        assertThat(reformatted).isEqualTo(formatted);
    }

    @Test
    void testFormatterHandlesEmptyClass()
            throws IOException
    {
        String source = """
                package test;

                public class Empty {}
                """;

        AirliftFormatter formatter = new AirliftFormatter();
        String formatted = formatter.format(source);

        // Verify it produces valid output
        assertThat(formatted).contains("class Empty");
    }

    @Test
    void testFormatterHandlesControlFlow()
    {
        String source = """
                package test;

                public class ControlFlow {
                    public void method() {
                        if (true) {
                            System.out.println("true");
                        } else {
                            System.out.println("false");
                        }
                    }
                }
                """;

        AirliftFormatter formatter = new AirliftFormatter();
        String formatted = formatter.format(source);

        // Verify formatter output contains valid if-else structure
        assertThat(formatted).contains("if (true)");
        assertThat(formatted).contains("else");
    }

    @Test
    void testFormatterHandlesLambda()
    {
        String source = """
                package test;

                import java.util.List;

                public class Lambda {
                    public void method() {
                        List.of("a", "b").forEach(s -> {
                            System.out.println(s);
                        });
                    }
                }
                """;

        AirliftFormatter formatter = new AirliftFormatter();
        String formatted = formatter.format(source);

        // Lambda brace should stay on same line
        assertThat(formatted).contains("-> {");
    }
}
