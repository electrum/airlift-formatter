package io.airlift.formatter.testing;

/**
 * Common interface for Java code formatters.
 *
 * <p>This interface allows the testing infrastructure to work with
 * any formatter implementation (JDT-based, Google-based, etc.)
 */
@FunctionalInterface
public interface JavaFormatter
{
    /**
     * Formats Java source code.
     *
     * @param source the source code to format
     * @return the formatted source code
     * @throws RuntimeException if the source cannot be formatted
     */
    String format(String source);
}
