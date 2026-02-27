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
public class EnumConstantAnnotationFormattingTest
{
    private AirliftFormatter formatter;

    @BeforeAll
    void setup()
    {
        formatter = new AirliftFormatter();
    }

    @Test
    void testAnnotationAndEnumConstantStayOnSameLine()
    {
        String input = """
                import com.fasterxml.jackson.annotation.JsonProperty;

                enum SortOrder
                {
                    @JsonProperty("asc") ASC,
                    @JsonProperty("desc") DESC;
                }
                """;

        String output = formatter.format(input);
        assertEquals(input, output, "Enum constant annotations should stay on the same line as the constant");
    }
}
