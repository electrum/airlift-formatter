/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for nested lambda continuation indentation preservation.
 */
@TestInstance(PER_CLASS)
public class NestedLambdaIndentTest
{
    private AirliftFormatter formatter;

    @BeforeAll
    void setup()
    {
        formatter = new AirliftFormatter();
    }

    @Test
    void testNestedLambdaIndentation()
    {
        // Actual test case from TestEmbedVersion.java lines 36-39
        String input = """
                class Test
                {
                    void test()
                    {
                        assertThatThrownBy(() ->
                                embedVersion.embedVersion((Runnable) () -> {
                                    throw new RuntimeException("Zonky zonk");
                                }).run())
                                .isInstanceOf(RuntimeException.class);
                    }
                }
                """;
        
        String output = formatter.format(input);
        
        System.out.println("=== INPUT ===");
        System.out.println(input);
        System.out.println("=== OUTPUT ===");
        System.out.println(output);
        
        // The line with }).run()) should have 16 leading spaces (4 tabs = 16)
        String[] lines = output.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("}).run()")) {
                int spaces = 0;
                for (char c : lines[i].toCharArray()) {
                    if (c == ' ') spaces++;
                    else break;
                }
                System.out.println("\nLine " + (i+1) + " with }).run(): " + spaces + " leading spaces");
                System.out.println("Expected: 16 spaces (to match embedVersion.embedVersion line)");
                
                // This assertion shows the current problem
                assertEquals(16, spaces, "Closing brace should align with embedVersion.embedVersion");
            }
        }
    }
}
