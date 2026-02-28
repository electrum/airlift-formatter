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

    @Test
    void testNoBlankLinesImmediatelyInsideTypeBraces()
    {
        String oldCode = """
                class Foo
                {

                    String abc;

                }
                """;

        String newCode = """
                class Foo
                {
                    String abc;
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testNoBlankLinesImmediatelyInsideIfBlockBraces()
    {
        String oldCode = """
                class Foo
                {
                    void run()
                    {
                        if (abc) {

                            exec();

                        }
                    }
                }
                """;

        String newCode = """
                class Foo
                {
                    void run()
                    {
                        if (abc) {
                            exec();
                        }
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testNoBlankLinesImmediatelyInsideLambdaAndSwitchExpressionBraces()
    {
        String oldCode = """
                class Foo
                {
                    Runnable create(int value)
                    {
                        return () -> {
                            if (value > 0) {

                                int result = switch (value) {

                                    case 1 -> 1;
                                    default -> 2;

                                };
                            }

                        };
                    }
                }
                """;

        String newCode = """
                class Foo
                {
                    Runnable create(int value)
                    {
                        return () -> {
                            if (value > 0) {
                                int result = switch (value) {
                                    case 1 -> 1;
                                    default -> 2;
                                };
                            }
                        };
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testCompressMultipleBlankLinesBetweenStatementsToSingle()
    {
        String oldCode = """
                class Foo
                {
                    void run(boolean first, boolean second)
                    {

                        if (first) {
                            executeFirst();
                        }


                        if (second) {
                            executeSecond();
                        }

                    }
                }
                """;

        String newCode = """
                class Foo
                {
                    void run(boolean first, boolean second)
                    {
                        if (first) {
                            executeFirst();
                        }

                        if (second) {
                            executeSecond();
                        }
                    }
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
