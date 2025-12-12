/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter;

import io.airlift.formatter.internal.jdt.DefaultCodeFormatterConstants;

/**
 * Brace placement style for Java code formatting.
 *
 * <p>Maps to IntelliJ brace style values and JDT formatter constants.
 */
public enum BraceStyle
{
    /**
     * Brace at end of line (K&amp;R style).
     * <pre>{@code
     * if (condition) {
     *     // body
     * }
     * }</pre>
     * IntelliJ value: 1
     */
    END_OF_LINE(1, DefaultCodeFormatterConstants.END_OF_LINE),

    /**
     * Brace on next line (Allman style).
     * <pre>{@code
     * if (condition)
     * {
     *     // body
     * }
     * }</pre>
     * IntelliJ value: 2
     */
    NEXT_LINE(2, DefaultCodeFormatterConstants.NEXT_LINE),

    /**
     * Brace on next line, shifted right.
     * <pre>{@code
     * if (condition)
     *     {
     *     // body
     *     }
     * }</pre>
     * IntelliJ value: 3
     */
    NEXT_LINE_SHIFTED(3, DefaultCodeFormatterConstants.NEXT_LINE_SHIFTED),

    /**
     * Brace on next line only when wrapped.
     * IntelliJ value: 5
     */
    NEXT_LINE_ON_WRAP(5, DefaultCodeFormatterConstants.NEXT_LINE_ON_WRAP);

    private final int intellijValue;
    private final String jdtValue;

    BraceStyle(int intellijValue, String jdtValue)
    {
        this.intellijValue = intellijValue;
        this.jdtValue = jdtValue;
    }

    /**
     * Returns the IntelliJ numeric value for this brace style.
     */
    public int getIntellijValue()
    {
        return intellijValue;
    }

    /**
     * Returns the JDT formatter constant for this brace style.
     */
    public String getJdtValue()
    {
        return jdtValue;
    }

    /**
     * Converts an IntelliJ brace style value to enum.
     *
     * @param intellijValue IntelliJ brace style value (1-5)
     * @return corresponding BraceStyle, defaults to END_OF_LINE
     */
    public static BraceStyle fromIntellijValue(int intellijValue)
    {
        for (BraceStyle style : values()) {
            if (style.intellijValue == intellijValue) {
                return style;
            }
        }
        return END_OF_LINE;
    }
}
