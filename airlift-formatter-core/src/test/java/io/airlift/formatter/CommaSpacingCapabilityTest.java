/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
public class CommaSpacingCapabilityTest
{
    private AirliftFormatter formatter;

    @BeforeAll
    void setup()
    {
        formatter = new AirliftFormatter();
    }

    @Test
    void testSpacingAfterCommaNotAutoFixed()
    {
        String oldCode = """
                class Test
                {
                    void test()
                    {
                        assertThat(runQuery(runner,query));
                    }
                }
                """;

        String newCode = """
                class Test
                {
                    void test()
                    {
                        assertThat(runQuery(runner, query));
                    }
                }
                """;

        String formattedCode = formatter.format(oldCode);
        assertNotEquals(newCode, formattedCode, "Comma spacing is not auto-fixed by the formatter yet");
        assertEquals(oldCode, formattedCode, "Old call-argument spacing should be preserved for unsupported auto-fixes");
    }
}
