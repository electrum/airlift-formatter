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
public class FormattingFixesTest
{
    private AirliftFormatter formatter;

    @BeforeAll
    void setup()
    {
        formatter = new AirliftFormatter();
    }

    @Test
    void testAnnotationTextBlockStartsOnNewLine()
    {
        String oldCode = """
                @interface Query
                {
                    String value();
                }

                class Test
                {
                    @Query(\"\"\"
                            SELECT true
                            \"\"\")
                    boolean isEnabled()
                    {
                        return true;
                    }
                }
                """;

        String newCode = """
                @interface Query
                {
                    String value();
                }

                class Test
                {
                    @Query(
                            \"\"\"
                            SELECT true
                            \"\"\")
                    boolean isEnabled()
                    {
                        return true;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testSqlQueryAnnotationTextBlockStartsOnNewLine()
    {
        String oldCode = """
                @interface SqlQuery
                {
                    String value();
                }

                interface Test
                {
                    @SqlQuery(\"\"\"
                            SELECT 123
                            \"\"\")
                    int getNames();
                }
                """;

        String newCode = """
                @interface SqlQuery
                {
                    String value();
                }

                interface Test
                {
                    @SqlQuery(
                            \"\"\"
                            SELECT 123
                            \"\"\")
                    int getNames();
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testLongSqlQueryAnnotationTextBlockAlignsWithOpeningDelimiter()
    {
        String oldCode = """
                @interface SqlQuery
                {
                    String value();
                }

                interface Test
                {
                    @SqlQuery(\"\"\"
                              SELECT names
                              FROM abc
                              WHERE id = 123
                                AND key = 'foo'
                              \"\"\")
                    int getNames();
                }
                """;

        String newCode = """
                @interface SqlQuery
                {
                    String value();
                }

                interface Test
                {
                    @SqlQuery(
                            \"\"\"
                            SELECT names
                            FROM abc
                            WHERE id = 123
                              AND key = 'foo'
                            \"\"\")
                    int getNames();
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testAlreadyWrappedAnnotationTextBlockRealignsContent()
    {
        String oldCode = """
                @interface SqlQuery
                {
                    String value();
                }

                interface Test
                {
                    @SqlQuery(
                            \"\"\"
                                      SELECT names
                                      FROM abc
                                      WHERE id = 123
                                        AND key = 'foo'
                                      \"\"\")
                    int getNames();
                }
                """;

        String newCode = """
                @interface SqlQuery
                {
                    String value();
                }

                interface Test
                {
                    @SqlQuery(
                            \"\"\"
                            SELECT names
                            FROM abc
                            WHERE id = 123
                              AND key = 'foo'
                            \"\"\")
                    int getNames();
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testUnderindentedAnnotationTextBlockContentIsRealigned()
    {
        String oldCode = """
                @interface SqlQuery
                {
                    String value();
                }

                interface Test
                {
                    @SqlQuery(
                            \"\"\"
                      SELECT values
                      FROM xyz
                      \"\"\")
                    int getValues();
                }
                """;

        String newCode = """
                @interface SqlQuery
                {
                    String value();
                }

                interface Test
                {
                    @SqlQuery(
                            \"\"\"
                            SELECT values
                            FROM xyz
                            \"\"\")
                    int getValues();
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testMethodArgumentTextBlockStartsOnNewLine()
    {
        String oldCode = """
                import java.util.Optional;

                class Test
                {
                    String value()
                    {
                        return Optional.of(\"\"\"
                                [Unit]
                                WantedBy=multi-user.target
                                \"\"\").orElseThrow();
                    }
                }
                """;

        String newCode = """
                import java.util.Optional;

                class Test
                {
                    String value()
                    {
                        return Optional.of(
                                \"\"\"
                                [Unit]
                                WantedBy=multi-user.target
                                \"\"\").orElseThrow();
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testAssignmentTextBlockStartsOnNewLine()
    {
        String oldCode = """
                class Test
                {
                    String query()
                    {
                        String sql = \"\"\"
                        SELECT 1
                        \"\"\";
                        return sql;
                    }
                }
                """;

        String newCode = """
                class Test
                {
                    String query()
                    {
                        String sql =
                        \"\"\"
                        SELECT 1
                        \"\"\";
                        return sql;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testReturnTextBlockCanStayOnSameLine()
    {
        String code = """
                class Test
                {
                    String query()
                    {
                        return \"\"\"
                               SELECT 1
                               \"\"\";
                    }
                }
                """;

        assertFormatsOldToNew(code, code);
    }

    @Test
    void testReturnTextBlockWithIndentedContentStaysOnSameLine()
    {
        String code = """
                class Test
                {
                    String query()
                    {
                        return \"\"\"
                               Resources with required tags:
                               - tag_a
                               \"\"\";
                    }
                }
                """;

        assertFormatsOldToNew(code, code);
    }

    @Test
    void testReturnFormattedTextBlockStaysOnSameLine()
    {
        String code = """
                class Test
                {
                    String query(String tags)
                    {
                        return \"\"\"
                               Resources with required tags:

                               %s
                               \"\"\".formatted(tags);
                    }
                }
                """;

        assertFormatsOldToNew(code, code);
    }

    private void assertFormatsOldToNew(String oldCode, String newCode)
    {
        assertEquals(newCode, formatter.format(oldCode), "Old formatting should be converted to the expected new style");
        assertEquals(newCode, formatter.format(newCode), "Expected new formatting should be idempotent");
    }
}
