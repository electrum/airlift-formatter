/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
public class MarkdownJavadocCommentTest
{
    private AirliftFormatter formatter;

    @BeforeAll
    void setup()
    {
        formatter = new AirliftFormatter();
    }

    @Test
    void testMarkdownJavadocCommentsPreserveLineWrapping()
    {
        String input = """
                class Test
                {
                    /// First paragraph line one
                    /// line two should not be joined.
                    ///
                    /// - first item
                    /// - second item
                    void method()
                    {
                    }
                }
                """;

        String output = formatter.format(input);
        assertEquals(input, output, "Markdown Javadoc comments should preserve original line wrapping");
    }
}
