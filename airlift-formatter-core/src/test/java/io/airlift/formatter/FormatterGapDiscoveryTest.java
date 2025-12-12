/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter;

import io.airlift.formatter.checkstyle.CheckstyleValidator;
import io.airlift.formatter.testing.FormatterGapDiscoveryBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * Gap discovery test for the forked JDT-based Airlift formatter.
 *
 * This test is NOT run in CI - it's for manual execution to discover
 * formatting edge cases in real codebases (Trino and Airlift).
 *
 * Usage:
 *   # First, clone the repos
 *   mkdir -p target/repos
 *   git clone --depth 1 https://github.com/trinodb/trino.git target/repos/trino
 *   git clone --depth 1 https://github.com/airlift/airlift.git target/repos/airlift
 *
 *   # CRITICAL TEST: Verify no changes on correctly-formatted code
 *   mvn test -pl airlift-formatter-jdt-core -Dtest=FormatterGapDiscoveryTest#verifyNoChangesOnAirlift -Drepos.dir=$(pwd)/target/repos
 *   mvn test -pl airlift-formatter-jdt-core -Dtest=FormatterGapDiscoveryTest#verifyNoChangesOnTrino -Drepos.dir=$(pwd)/target/repos
 *
 *   # Test idempotency
 *   mvn test -pl airlift-formatter-jdt-core -Dtest=FormatterGapDiscoveryTest#verifyIdempotencyInAirlift -Drepos.dir=$(pwd)/target/repos
 *   mvn test -pl airlift-formatter-jdt-core -Dtest=FormatterGapDiscoveryTest#verifyIdempotencyInTrino -Drepos.dir=$(pwd)/target/repos
 *
 *   # Clean up
 *   rm -rf target/repos
 */
@TestInstance(PER_CLASS)
public class FormatterGapDiscoveryTest
        extends FormatterGapDiscoveryBase
{
    private AirliftFormatter formatter;

    @BeforeAll
    void setup()
    {
        formatter = new AirliftFormatter();
        System.out.println("Using repos directory: " + getReposDir());
        System.out.println("Using forked JDT formatter (jdt-core module)");
    }

    @Test
    void verifyNoChangesOnAirlift()
            throws IOException
    {
        assumeTrue(Files.exists(getAirliftDir()),
                "Airlift repo not cloned. Run: git clone --depth 1 https://github.com/airlift/airlift.git " + getAirliftDir());

        verifyNoChanges("airlift/airlift", getAirliftDir(), formatter::format);
    }

    @Test
    void verifyNoChangesOnTrino()
            throws IOException
    {
        assumeTrue(Files.exists(getTrinoDir()),
                "Trino repo not cloned. Run: git clone --depth 1 https://github.com/trinodb/trino.git " + getTrinoDir());

        verifyNoChanges("trinodb/trino", getTrinoDir(), formatter::format);
    }

    @Test
    void verifyIdempotencyInAirlift()
            throws IOException
    {
        assumeTrue(Files.exists(getAirliftDir()),
                "Airlift repo not cloned. Run: git clone --depth 1 https://github.com/airlift/airlift.git " + getAirliftDir());

        verifyIdempotency("airlift/airlift", getAirliftDir(), formatter::format);
    }

    @Test
    void verifyIdempotencyInTrino()
            throws IOException
    {
        assumeTrue(Files.exists(getTrinoDir()),
                "Trino repo not cloned. Run: git clone --depth 1 https://github.com/trinodb/trino.git " + getTrinoDir());

        verifyIdempotency("trinodb/trino", getTrinoDir(), formatter::format);
    }

    @Test
    void verifyComprehensiveOnAirlift()
            throws IOException
    {
        assumeTrue(Files.exists(getAirliftDir()),
                "Airlift repo not cloned. Run: git clone --depth 1 https://github.com/airlift/airlift.git " + getAirliftDir());

        // Use formatting-focused checkstyle config (excludes PackageDeclaration which
        // always fails for files cloned to target/repos with io.airlift.* packages)
        CheckstyleValidator checkstyleValidator = new CheckstyleValidator("/checkstyle/formatting-checks.xml");
        long seed = 12345L;
        double mutationProbability = 0.3;

        ComprehensiveTestResult result = verifyComprehensive(
                "airlift/airlift",
                getAirliftDir(),
                formatter::format,
                checkstyleValidator,
                seed,
                mutationProbability);

        System.out.printf("%nPass rate: %.1f%%%n", result.passRate());
    }

    @Test
    void verifyComprehensiveOnTrino()
            throws IOException
    {
        assumeTrue(Files.exists(getTrinoDir()),
                "Trino repo not cloned. Run: git clone --depth 1 https://github.com/trinodb/trino.git " + getTrinoDir());

        // Use formatting-focused checkstyle config (excludes PackageDeclaration which
        // always fails for files cloned to target/repos with io.trino.* packages)
        CheckstyleValidator checkstyleValidator = new CheckstyleValidator("/checkstyle/formatting-checks.xml");
        long seed = 12345L;
        double mutationProbability = 0.3;

        ComprehensiveTestResult result = verifyComprehensive(
                "trinodb/trino",
                getTrinoDir(),
                formatter::format,
                checkstyleValidator,
                seed,
                mutationProbability);

        System.out.printf("%nPass rate: %.1f%%%n", result.passRate());
    }
}
