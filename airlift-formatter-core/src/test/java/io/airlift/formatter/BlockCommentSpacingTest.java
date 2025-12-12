/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * Test for Pattern 3: Block comment aligned spacing preservation.
 *
 * The formatter should preserve original spacing after block comments.
 * For example, if original has 2 spaces after the comment, the output should also have 2 spaces.
 */
@TestInstance(PER_CLASS)
public class BlockCommentSpacingTest
{
    private AirliftFormatter formatter;

    @BeforeAll
    void setup()
    {
        formatter = new AirliftFormatter();
    }

    @Test
    void testBlockCommentWithMultipleSpacesAfter()
    {
        // Original from Trino: TestHashAggregationOperator.java
        // /* spill enabled */  true, (2 spaces)
        String input = "class Test {\n" +
                "    void method() {\n" +
                "        call(\n" +
                "                /* spill enabled */  true,\n" +
                "                /* other param */    false);\n" +
                "    }\n" +
                "}\n";

        String output = formatter.format(input);

        System.out.println("=== Block Comment Multiple Spaces ===");
        System.out.println("Input:");
        System.out.println(input);
        System.out.println("Output:");
        System.out.println(output);

        // gofmt-style: preserve original spacing (2 spaces after comment)
        assertTrue(output.contains("*/  true"),
            "Block comment should preserve original spacing (2 spaces after */)");
    }

    @Test
    void testBlockCommentWithSingleSpaceAfter()
    {
        // With single space - should also be preserved
        String input = "class Test {\n" +
                "    void method() {\n" +
                "        call(\n" +
                "                /* spill enabled */ true,\n" +
                "                /* other param */ false);\n" +
                "    }\n" +
                "}\n";

        String output = formatter.format(input);

        System.out.println("=== Block Comment Single Space ===");
        System.out.println("Output:");
        System.out.println(output);

        assertTrue(output.contains("*/ true") && !output.contains("*/  true"),
            "Block comment should preserve original spacing (1 space after */)");
    }

    @Test
    void testAlignedBlockComments()
    {
        // Test aligned block comments with consistent spacing
        String input = "class Test {\n" +
                "    void method() {\n" +
                "        method(\n" +
                "                '\\\\',                       /* esc */\n" +
                "                INEFFECTIVE_META_CHAR,          /* anychar '.' */\n" +
                "                INEFFECTIVE_META_CHAR);         /* anytime '*' */\n" +
                "    }\n" +
                "}\n";

        String output = formatter.format(input);

        System.out.println("=== Aligned Block Comments ===");
        System.out.println("Output:");
        System.out.println(output);

        // The comments should preserve their spacing relative to the code
    }
}
