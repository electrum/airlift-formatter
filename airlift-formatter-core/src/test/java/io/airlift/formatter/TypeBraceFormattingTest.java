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
public class TypeBraceFormattingTest
{
    private AirliftFormatter formatter;

    @BeforeAll
    void setup()
    {
        formatter = new AirliftFormatter();
    }

    @Test
    void testEnumBracePlacement()
    {
        String oldCode = """
                public enum ContentSortBy {
                    NAME,
                    DATE_MODIFIED;
                }
                """;

        String newCode = """
                public enum ContentSortBy
                {
                    NAME,
                    DATE_MODIFIED;
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testClassAndInterfaceBracePlacement()
    {
        String oldCode = """
                class Sample {
                }

                interface Contract {
                    void run();
                }
                """;

        String newCode = """
                class Sample {}

                interface Contract
                {
                    void run();
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testEmptyAnnotationTypeBracePlacement()
    {
        String oldCode = """
                public @interface ForSensitiveData
                {
                }
                """;

        String newCode = """
                public @interface ForSensitiveData {}
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testEmptyMethodBracePlacement()
    {
        String oldCode = """
                class Test
                {
                    void run()
                    {
                    }
                }
                """;

        String newCode = """
                class Test
                {
                    void run() {}
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testEmptyLambdaBracePlacement()
    {
        String oldCode = """
                class Test
                {
                    Runnable action = () -> {
                    };
                }
                """;

        String newCode = """
                class Test
                {
                    Runnable action = () -> {};
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    private void assertFormatsOldToNew(String oldCode, String newCode)
    {
        assertEquals(newCode, formatter.format(oldCode), "Old formatting should be converted to the expected new style");
        assertEquals(newCode, formatter.format(newCode), "Expected new formatting should be idempotent");
    }
}
