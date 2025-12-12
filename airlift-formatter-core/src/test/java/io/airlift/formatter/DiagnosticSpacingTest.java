/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter;

import org.junit.jupiter.api.Test;

/**
 * Diagnostic test to understand why enum lambda contexts lose trailing comment spacing
 * while class lambda contexts preserve it.
 */
public class DiagnosticSpacingTest
{
    @Test
    void diagnoseDifference()
    {
        AirliftFormatter formatter = new AirliftFormatter();

        // Minimal test cases - one enum, one class
        String enumInput = "enum E{V(()->{x(); // c\n});}";
        String classInput = "class C{Runnable r=()->{x(); // c\n};}";

        System.out.println("=== ENUM LAMBDA TEST ===");
        System.out.println("Input: " + enumInput.replace("\n", "\\n"));
        String enumOutput = formatter.format(enumInput);
        System.out.println("Output: " + enumOutput.replace("\n", "\\n"));
        System.out.println("Has '; // c': " + enumOutput.contains("; // c"));
        System.out.println("Has ';// c': " + enumOutput.contains(";// c"));

        System.out.println();
        System.out.println("=== CLASS LAMBDA TEST ===");
        System.out.println("Input: " + classInput.replace("\n", "\\n"));
        String classOutput = formatter.format(classInput);
        System.out.println("Output: " + classOutput.replace("\n", "\\n"));
        System.out.println("Has '; // c': " + classOutput.contains("; // c"));
        System.out.println("Has ';// c': " + classOutput.contains(";// c"));

        // Let's also test a more explicit version with proper formatting
        System.out.println();
        System.out.println("=== EXPLICIT TESTS ===");

        // Enum method (not in constructor) - should work
        String enumMethodInput =
                "enum E {\n" +
                "    V;\n" +
                "    void method() {\n" +
                "        x(); // comment\n" +
                "    }\n" +
                "}\n";
        String enumMethodOutput = formatter.format(enumMethodInput);
        System.out.println("Enum method has '; // comment': " + enumMethodOutput.contains("; // comment"));

        // Enum constructor lambda
        String enumCtorLambdaInput =
                "enum E {\n" +
                "    V(() -> {\n" +
                "        x(); // comment\n" +
                "    });\n" +
                "    E(Runnable r) {}\n" +
                "}\n";
        String enumCtorLambdaOutput = formatter.format(enumCtorLambdaInput);
        System.out.println("Enum ctor lambda input:");
        System.out.println(enumCtorLambdaInput);
        System.out.println("Enum ctor lambda output:");
        System.out.println(enumCtorLambdaOutput);
        System.out.println("Enum ctor lambda has '; // comment': " + enumCtorLambdaOutput.contains("; // comment"));

        // Class field lambda
        String classLambdaInput =
                "class C {\n" +
                "    Runnable r = () -> {\n" +
                "        x(); // comment\n" +
                "    };\n" +
                "}\n";
        String classLambdaOutput = formatter.format(classLambdaInput);
        System.out.println("Class lambda input:");
        System.out.println(classLambdaInput);
        System.out.println("Class lambda output:");
        System.out.println(classLambdaOutput);
        System.out.println("Class lambda has '; // comment': " + classLambdaOutput.contains("; // comment"));
    }
}
