/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter.checkstyle;

/**
 * Exception thrown when checkstyle validation fails due to configuration
 * or processing errors.
 */
public class CheckstyleValidationException
        extends RuntimeException
{
    public CheckstyleValidationException(String message)
    {
        super(message);
    }

    public CheckstyleValidationException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
