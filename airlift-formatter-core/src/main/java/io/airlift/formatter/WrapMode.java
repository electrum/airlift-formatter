/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter;

import io.airlift.formatter.internal.jdt.DefaultCodeFormatterConstants;

/**
 * Line wrapping mode for Java code formatting.
 *
 * <p>Maps to IntelliJ wrap mode values and JDT alignment constants.
 */
public enum WrapMode
{
    /**
     * Do not wrap at all.
     * IntelliJ value: 0
     */
    DO_NOT_WRAP(0, DefaultCodeFormatterConstants.WRAP_NO_SPLIT),

    /**
     * Always wrap - put each element on a new line.
     * IntelliJ value: 1
     */
    WRAP_ALWAYS(1, DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE),

    /**
     * Wrap if long - chop down (wrap all elements if line exceeds limit).
     * IntelliJ value: 2
     */
    WRAP_IF_LONG_CHOP(2, DefaultCodeFormatterConstants.WRAP_NEXT_PER_LINE),

    /**
     * Wrap if long - compact (wrap only as needed).
     * IntelliJ value: 5
     */
    WRAP_IF_LONG_COMPACT(5, DefaultCodeFormatterConstants.WRAP_COMPACT);

    private final int intellijValue;
    private final int jdtValue;

    WrapMode(int intellijValue, int jdtValue)
    {
        this.intellijValue = intellijValue;
        this.jdtValue = jdtValue;
    }

    /**
     * Returns the IntelliJ numeric value for this wrap mode.
     */
    public int getIntellijValue()
    {
        return intellijValue;
    }

    /**
     * Returns the JDT alignment constant for this wrap mode.
     */
    public int getJdtValue()
    {
        return jdtValue;
    }

    /**
     * Creates a JDT alignment value string from this wrap mode.
     *
     * @param forceWrap whether to force wrapping
     * @return JDT alignment value string
     */
    public String toJdtAlignmentValue(boolean forceWrap)
    {
        return DefaultCodeFormatterConstants.createAlignmentValue(forceWrap, jdtValue);
    }

    /**
     * Converts an IntelliJ wrap mode value to enum.
     *
     * @param intellijValue IntelliJ wrap mode value (0, 1, 2, or 5)
     * @return corresponding WrapMode, defaults to DO_NOT_WRAP
     */
    public static WrapMode fromIntellijValue(int intellijValue)
    {
        for (WrapMode mode : values()) {
            if (mode.intellijValue == intellijValue) {
                return mode;
            }
        }
        return DO_NOT_WRAP;
    }
}
