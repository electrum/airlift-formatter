/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * Test for Pattern 6: Empty brace spacing in anonymous classes.
 *
 * The formatter should preserve original spacing in empty anonymous class bodies:
 * new TypeReference<>(){} should stay as new TypeReference<>(){}
 * new TypeReference<>() {} should stay as new TypeReference<>() {}
 */
@TestInstance(PER_CLASS)
public class EmptyBraceSpacingTest
{
    private AirliftFormatter formatter;

    @BeforeAll
    void setup()
    {
        formatter = new AirliftFormatter();
    }

    @Test
    void testEmptyAnonymousClassNoSpace()
    {
        // Original from Trino: DeltaLakeSchemaSupport.java
        String input = "class Test {\n" +
                "    void method() {\n" +
                "        Object o = new TypeReference<String>(){};\n" +
                "    }\n" +
                "}\n";

        String output = formatter.format(input);

        System.out.println("=== Empty Anonymous Class (no space) ===");
        System.out.println("Input:");
        System.out.println(input);
        System.out.println("Output:");
        System.out.println(output);

        // gofmt-style: preserve original spacing (no space)
        assertTrue(output.contains("(){}"),
            "Empty anonymous class should preserve original spacing (no space before {})");
    }

    @Test
    void testEmptyAnonymousClassWithSpace()
    {
        // Test that space IS preserved if original has it
        String input = "class Test {\n" +
                "    void method() {\n" +
                "        Object o = new TypeReference<String>() {};\n" +
                "    }\n" +
                "}\n";

        String output = formatter.format(input);

        System.out.println("=== Empty Anonymous Class (with space) ===");
        System.out.println("Input:");
        System.out.println(input);
        System.out.println("Output:");
        System.out.println(output);

        assertTrue(output.contains("() {}"),
            "Empty anonymous class should preserve original spacing (space before {})");
    }

    @Test
    void testEmptyArrayInitializerNoSpace()
    {
        // Similar issue with array initializers
        String input = "class Test {\n" +
                "    void method() {\n" +
                "        String[] arr = new String[]{};\n" +
                "    }\n" +
                "}\n";

        String output = formatter.format(input);

        System.out.println("=== Empty Array Initializer (no space) ===");
        System.out.println("Output:");
        System.out.println(output);
    }

    @Test
    void testNonEmptyArrayInitializerNoSpace()
    {
        // Trino case: GcsInputFile.java - non-empty array initializer without space before {
        String input = "class Test {\n" +
                "    void method() {\n" +
                "        String[] arr = new String[]{\"value\"};\n" +
                "    }\n" +
                "}\n";

        String output = formatter.format(input);

        System.out.println("=== Non-Empty Array Initializer (no space) ===");
        System.out.println("Input:");
        System.out.println(input);
        System.out.println("Output:");
        System.out.println(output);

        assertTrue(output.contains("[]{"),
            "Array initializer should preserve original spacing (no space before {})");
    }

    @Test
    void testNonEmptyArrayInitializerWithSpace()
    {
        // With space before {
        String input = "class Test {\n" +
                "    void method() {\n" +
                "        String[] arr = new String[] {\"value\"};\n" +
                "    }\n" +
                "}\n";

        String output = formatter.format(input);

        System.out.println("=== Non-Empty Array Initializer (with space) ===");
        System.out.println("Output:");
        System.out.println(output);

        assertTrue(output.contains("[] {"),
            "Array initializer should preserve original spacing (space before {})");
    }

    @Test
    void testExactTrinoCase()
    {
        // Exact case from Trino: DeltaLakeSchemaSupport.java
        String input = "class Test {\n" +
                "    void method() {\n" +
                "        OBJECT_MAPPER.convertValue(node.get(\"type\"), new TypeReference<>(){});\n" +
                "    }\n" +
                "}\n";

        String output = formatter.format(input);

        System.out.println("=== Exact Trino Case ===");
        System.out.println("Output:");
        System.out.println(output);

        assertTrue(output.contains("(){}"),
            "Trino case: preserve no-space before {}");
    }
}
