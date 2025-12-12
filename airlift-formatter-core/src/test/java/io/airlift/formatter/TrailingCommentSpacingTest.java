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
 * Test for Pattern 2: Trailing line comment spacing preservation.
 *
 * The formatter should preserve spacing between code and trailing comments:
 * ); // comment  should stay as  ); // comment
 * NOT become:  );// comment
 */
@TestInstance(PER_CLASS)
public class TrailingCommentSpacingTest
{
    private AirliftFormatter formatter;

    @BeforeAll
    void setup()
    {
        formatter = new AirliftFormatter();
    }

    @Test
    void testTrailingCommentSpacingPreserved()
    {
        // Use Airlift-style braces (NEXT_LINE for class/method)
        String input = "class Test\n" +
                "{\n" +
                "    void method()\n" +
                "    {\n" +
                "        int x = getValue(); // comment\n" +
                "    }\n" +
                "}\n";

        String output = formatter.format(input);

        // The space before // should be preserved
        assertEquals(input, output, "Trailing comment spacing should be preserved");
    }

    @Test
    void testTrailingCommentWithMultipleSpaces()
    {
        // Use Airlift-style braces (NEXT_LINE for class/method)
        String input = "class Test\n" +
                "{\n" +
                "    void method()\n" +
                "    {\n" +
                "        int x = getValue();  // comment with 2 spaces before\n" +
                "    }\n" +
                "}\n";

        String output = formatter.format(input);

        // The original spacing should be preserved (gofmt-style)
        assertEquals(input, output, "Multiple spaces before comment should be preserved");
    }

    @Test
    void testRealTrinoCase()
    {
        // Actual case from trino: BenchmarkGroupByHashOnSimulatedData.java:162
        // Use Airlift-style braces (NEXT_LINE for class/method)
        String input = "class Test\n" +
                "{\n" +
                "    void method()\n" +
                "    {\n" +
                "        BigintType.BIGINT.writeLong(blockBuilder, r.nextLong() >>> 1); // Only positives\n" +
                "    }\n" +
                "}\n";

        String output = formatter.format(input);

        System.out.println("Input:");
        System.out.println(input);
        System.out.println("Output:");
        System.out.println(output);

        // Check that the space before // is preserved
        assertEquals(input, output, "Real Trino case: space before // should be preserved");
    }

    @Test
    void testAlignedCommentSpacing()
    {
        // Test case for aligned comments
        String input = "class Test {\n" +
                "    void method() {\n" +
                "        /*  spill enabled  */  true,\n" +
                "    }\n" +
                "}\n";

        String output = formatter.format(input);

        System.out.println("Input:");
        System.out.println(input);
        System.out.println("Output:");
        System.out.println(output);
    }

    @Test
    void testExactTrinoEnumLambdaCase()
    {
        // EXACT context from trino: BenchmarkGroupByHashOnSimulatedData.java
        // The comment is inside a lambda inside an enum constructor
        String input = "enum ColumnType\n" +
                "{\n" +
                "    BIGINT(BigintType.BIGINT, (blockBuilder, positionCount, seed) -> {\n" +
                "        Random r = new Random(seed);\n" +
                "        for (int i = 0; i < positionCount; i++) {\n" +
                "            BigintType.BIGINT.writeLong(blockBuilder, r.nextLong() >>> 1); // Only positives\n" +
                "        }\n" +
                "    });\n" +
                "}\n";

        String output = formatter.format(input);

        System.out.println("=== Exact Trino Enum Lambda Test ===");
        System.out.println("Input:");
        System.out.println(input);
        System.out.println("Output:");
        System.out.println(output);

        // Check that the space before // is preserved
        assertTrue(output.contains("); // Only positives"),
            "Space before // should be preserved in enum lambda context");
    }

    @Test
    void testCommentInForLoop()
    {
        // Another real case pattern - comment in a for loop body
        String input = "class Test {\n" +
                "    void method() {\n" +
                "        for (int i = 0; i < 10; i++) {\n" +
                "            doSomething(i >>> 1); // comment\n" +
                "        }\n" +
                "    }\n" +
                "}\n";

        String output = formatter.format(input);

        System.out.println("=== For Loop Comment Test ===");
        System.out.println("Input:");
        System.out.println(input);
        System.out.println("Output:");
        System.out.println(output);

        assertTrue(output.contains("); // comment"),
            "Space before // should be preserved in for loop");
    }

    @Test
    void testLambdaOnly()
    {
        String input = "class Test {\n" +
                "    Runnable r = () -> {\n" +
                "        doSomething(); // comment\n" +
                "    };\n" +
                "}\n";

        String output = formatter.format(input);

        System.out.println("=== Lambda Only Test ===");
        System.out.println("Output:");
        System.out.println(output);

        assertTrue(output.contains("); // comment"),
            "Space before // should be preserved in lambda");
    }

    @Test
    void testEnumMethod()
    {
        String input = "enum Test {\n" +
                "    VALUE;\n" +
                "    void method() {\n" +
                "        doSomething(); // comment\n" +
                "    }\n" +
                "}\n";

        String output = formatter.format(input);

        System.out.println("=== Enum Method Test ===");
        System.out.println("Output:");
        System.out.println(output);

        assertTrue(output.contains("); // comment"),
            "Space before // should be preserved in enum method");
    }

    @Test
    void testEnumConstructorWithLambda()
    {
        String input = "enum Test {\n" +
                "    VALUE(() -> {\n" +
                "        doSomething(); // comment\n" +
                "    });\n" +
                "    Test(Runnable r) {}\n" +
                "}\n";

        String output = formatter.format(input);

        System.out.println("=== Enum + Lambda Test ===");
        System.out.println("Output:");
        System.out.println(output);

        assertTrue(output.contains("); // comment"),
            "Space before // should be preserved in enum constructor lambda");
    }

    @Test
    void testEnumConstructorWithLambdaMinimal()
    {
        // Test: lambda inside enum constant (no space before comment in input)
        String input1 = "enum E{V(()->{x();// c\n});}";
        // Test: lambda inside class (no space before comment in input)
        String input2 = "class C{Runnable r=()->{x();// c\n};}";
        // Test: lambda inside enum constant (WITH space before comment in input)
        String input3 = "enum E{V(()->{x(); // c\n});}";
        // Test: lambda inside class (WITH space before comment in input)
        String input4 = "class C{Runnable r=()->{x(); // c\n};}";

        System.out.println("=== Lambda Comment Spacing Comparison ===");
        System.out.println();

        System.out.println("1. Enum lambda, NO space in input:");
        System.out.println("   Input:  x();// c");
        String output1 = formatter.format(input1);
        System.out.println("   Output: " + (output1.contains(";// c") ? "x();// c (no space)" : "x(); // c (space added)"));

        System.out.println();
        System.out.println("2. Class lambda, NO space in input:");
        System.out.println("   Input:  x();// c");
        String output2 = formatter.format(input2);
        System.out.println("   Output: " + (output2.contains(";// c") ? "x();// c (no space)" : "x(); // c (space added)"));

        System.out.println();
        System.out.println("3. Enum lambda, WITH space in input:");
        System.out.println("   Input:  x(); // c");
        String output3 = formatter.format(input3);
        System.out.println("   Output: " + (output3.contains("; // c") ? "x(); // c (space preserved)" : "x();// c (space LOST)"));

        System.out.println();
        System.out.println("4. Class lambda, WITH space in input:");
        System.out.println("   Input:  x(); // c");
        String output4 = formatter.format(input4);
        System.out.println("   Output: " + (output4.contains("; // c") ? "x(); // c (space preserved)" : "x();// c (space LOST)"));

        System.out.println();
        System.out.println("Full output for case 3 (enum lambda with space):");
        System.out.println(output3);
    }

    @Test
    void testForLoopInEnumMethod()
    {
        // For loop inside enum method
        String input = "enum Test {\n" +
                "    VALUE;\n" +
                "    void method() {\n" +
                "        for (int i = 0; i < 10; i++) {\n" +
                "            doSomething(i); // comment\n" +
                "        }\n" +
                "    }\n" +
                "}\n";

        String output = formatter.format(input);

        System.out.println("=== For Loop in Enum Method Test ===");
        System.out.println("Output:");
        System.out.println(output);

        assertTrue(output.contains("; // comment"),
            "Space before // should be preserved in for loop inside enum method");
    }

    @Test
    void testForLoopInLambda()
    {
        // For loop inside lambda - simpler than full enum case
        String input = "class Test {\n" +
                "    Runnable r = () -> {\n" +
                "        for (int i = 0; i < 10; i++) {\n" +
                "            doSomething(i); // comment\n" +
                "        }\n" +
                "    };\n" +
                "}\n";

        String output = formatter.format(input);

        System.out.println("=== For Loop in Lambda Test ===");
        System.out.println("Output:");
        System.out.println(output);

        assertTrue(output.contains("; // comment"),
            "Space before // should be preserved in for loop inside lambda");
    }

    @Test
    void testSimpleTrailingComment()
    {
        // Simplest possible case - verify the basic case works
        String input = "class Test {\n" +
                "    void method() {\n" +
                "        x = 1; // comment\n" +
                "    }\n" +
                "}\n";

        String output = formatter.format(input);

        System.out.println("=== Simple Trailing Comment Test ===");
        System.out.println("Input:");
        System.out.println(input);
        System.out.println("Output:");
        System.out.println(output);

        assertTrue(output.contains("; // comment"),
            "Space before // should be preserved in simple case");
    }
}
