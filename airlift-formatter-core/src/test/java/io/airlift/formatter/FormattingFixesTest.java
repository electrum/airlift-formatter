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
    void testAssignmentTextBlockOpenerUsesContinuationIndent()
    {
        String oldCode = """
                class Test
                {
                    String policy()
                    {
                        var policy = \"\"\"
                        allow all
                        \"\"\";
                        return policy;
                    }
                }
                """;

        String newCode = """
                class Test
                {
                    String policy()
                    {
                        var policy =
                                \"\"\"
                                allow all
                                \"\"\";
                        return policy;
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

    @Test
    void testThrowTextBlockArgumentStartsOnNewLine()
    {
        String oldCode = """
                class Test
                {
                    void fail()
                    {
                        throw new IllegalStateException(\"\"\"
                                boom
                                \"\"\");
                    }
                }
                """;

        String newCode = """
                class Test
                {
                    void fail()
                    {
                        throw new IllegalStateException(
                                \"\"\"
                                boom
                                \"\"\");
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testYieldTextBlockCanStayOnSameLine()
    {
        String code = """
                class Test
                {
                    String value(int input)
                    {
                        return switch (input) {
                            default -> {
                                yield \"\"\"
                                       value
                                       \"\"\";
                            }
                        };
                    }
                }
                """;

        assertFormatsOldToNew(code, code);
    }

    @Test
    void testTernaryTextBlocksUseSeparateQuestionAndColonLines()
    {
        String oldCode = """
                class Test
                {
                    String value(boolean cond)
                    {
                        var formattedPolicyText = cond ? \"\"\"
                                       xxx
                                       \"\"\" : \"\"\"
                                               yyy
                                               \"\"\";
                        return formattedPolicyText;
                    }
                }
                """;

        String newCode = """
                class Test
                {
                    String value(boolean cond)
                    {
                        var formattedPolicyText = cond
                                ? \"\"\"
                                  xxx
                                  \"\"\"
                                : \"\"\"
                                  yyy
                                  \"\"\";
                        return formattedPolicyText;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testParenthesizedTextBlockStartsOnNewLine()
    {
        String oldCode = """
                class Test
                {
                    String value()
                    {
                        var formattedPolicyText = (\"\"\"
                                abc
                                \"\"\");
                        return formattedPolicyText;
                    }
                }
                """;

        String newCode = """
                class Test
                {
                    String value()
                    {
                        var formattedPolicyText = (
                                \"\"\"
                                abc
                                \"\"\");
                        return formattedPolicyText;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testLambdaTextBlockStartsOnNewLine()
    {
        String oldCode = """
                class Test
                {
                    Runnable action()
                    {
                        return () -> \"\"\"
                                lambda
                                \"\"\";
                    }
                }
                """;

        String newCode = """
                class Test
                {
                    Runnable action()
                    {
                        return () ->
                                \"\"\"
                                lambda
                                \"\"\";
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testSwitchRuleTextBlockCanStayOnSameLine()
    {
        String code = """
                class Test
                {
                    String value(int input)
                    {
                        return switch (input) {
                            case 1 -> \"\"\"
                                      one
                                      \"\"\";
                            default -> \"\"\"
                                       other
                                       \"\"\";
                        };
                    }
                }
                """;

        assertFormatsOldToNew(code, code);
    }

    @Test
    void testBinaryExpressionTextBlockStartsOnNewLine()
    {
        String oldCode = """
                class Test
                {
                    String value()
                    {
                        var formattedPolicyText = \"prefix \" + \"\"\"
                                value
                                \"\"\";
                        return formattedPolicyText;
                    }
                }
                """;

        String newCode = """
                class Test
                {
                    String value()
                    {
                        var formattedPolicyText = \"prefix \" +
                                \"\"\"
                                value
                                \"\"\";
                        return formattedPolicyText;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testCastExpressionTextBlockStartsOnNewLine()
    {
        String oldCode = """
                class Test
                {
                    String value()
                    {
                        var formattedPolicyText = (String) \"\"\"
                                value
                                \"\"\";
                        return formattedPolicyText;
                    }
                }
                """;

        String newCode = """
                class Test
                {
                    String value()
                    {
                        var formattedPolicyText = (String)
                                \"\"\"
                                value
                                \"\"\";
                        return formattedPolicyText;
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testAssertMessageTextBlockStartsOnNewLine()
    {
        String oldCode = """
                class Test
                {
                    void verify(boolean ok)
                    {
                        assert ok : \"\"\"
                                bad
                                \"\"\";
                    }
                }
                """;

        String newCode = """
                class Test
                {
                    void verify(boolean ok)
                    {
                        assert ok :
                                \"\"\"
                                bad
                                \"\"\";
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testArrayInitializerElementTextBlockStartsOnNewLine()
    {
        String oldCode = """
                class Test
                {
                    String[] values()
                    {
                        return new String[] { \"\"\"
                                value
                                \"\"\" };
                    }
                }
                """;

        String newCode = """
                class Test
                {
                    String[] values()
                    {
                        return new String[] {
                                \"\"\"
                                value
                                \"\"\"};
                    }
                }
                """;

        assertFormatsOldToNew(oldCode, newCode);
    }

    @Test
    void testFieldInitializerTextBlockStartsOnNewLine()
    {
        String oldCode = """
                class Test
                {
                    String policy = \"\"\"
                            field
                            \"\"\";
                }
                """;

        String newCode = """
                class Test
                {
                    String policy =
                            \"\"\"
                            field
                            \"\"\";
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
