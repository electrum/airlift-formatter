/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link AirliftImportOrganizer}.
 */
class AirliftImportOrganizerTest
{
    private final AirliftImportOrganizer organizer = new AirliftImportOrganizer();

    @Test
    void testBasicImportOrdering()
    {
        String input = """
                package io.airlift.test;

                import java.util.List;
                import com.google.common.collect.ImmutableList;
                import javax.annotation.Nullable;

                public class Test {}
                """;

        String expected = """
                package io.airlift.test;

                import com.google.common.collect.ImmutableList;

                import javax.annotation.Nullable;

                import java.util.List;

                public class Test {}
                """;

        String actual = organizer.organizeImports(input);
        assertEquals(expected, actual);
    }

    @Test
    void testStaticImportsAtBottom()
    {
        String input = """
                package io.airlift.test;

                import static org.junit.jupiter.api.Assertions.assertEquals;
                import com.google.common.collect.ImmutableList;
                import java.util.List;

                public class Test {}
                """;

        String expected = """
                package io.airlift.test;

                import com.google.common.collect.ImmutableList;

                import java.util.List;

                import static org.junit.jupiter.api.Assertions.assertEquals;

                public class Test {}
                """;

        String actual = organizer.organizeImports(input);
        assertEquals(expected, actual);
    }

    @Test
    void testAlphabeticalSortingWithinGroups()
    {
        String input = """
                package io.airlift.test;

                import com.google.common.collect.ImmutableSet;
                import com.google.common.collect.ImmutableList;
                import com.google.common.collect.ImmutableMap;
                import java.util.Set;
                import java.util.List;
                import java.util.Map;

                public class Test {}
                """;

        String expected = """
                package io.airlift.test;

                import com.google.common.collect.ImmutableList;
                import com.google.common.collect.ImmutableMap;
                import com.google.common.collect.ImmutableSet;

                import java.util.List;
                import java.util.Map;
                import java.util.Set;

                public class Test {}
                """;

        String actual = organizer.organizeImports(input);
        assertEquals(expected, actual);
    }

    @Test
    void testScrambledImportsRestored()
    {
        // This simulates what ShuffleImportsMutator does
        String scrambled = """
                package io.airlift.test;

                import java.util.Map;
                import io.airlift.http.client.HttpClient;
                import java.security.PublicKey;
                import java.util.concurrent.atomic.AtomicReference;
                import io.airlift.units.Duration;
                import io.airlift.http.client.Response;
                import java.net.URI;
                import org.junit.jupiter.api.Test;
                import static org.assertj.core.api.Assertions.assertThat;

                public class Test {}
                """;

        String expected = """
                package io.airlift.test;

                import io.airlift.http.client.HttpClient;
                import io.airlift.http.client.Response;
                import io.airlift.units.Duration;
                import org.junit.jupiter.api.Test;

                import java.net.URI;
                import java.security.PublicKey;
                import java.util.Map;
                import java.util.concurrent.atomic.AtomicReference;

                import static org.assertj.core.api.Assertions.assertThat;

                public class Test {}
                """;

        String actual = organizer.organizeImports(scrambled);
        assertEquals(expected, actual);
    }

    @Test
    void testPreservesCodeAfterImports()
    {
        String input = """
                package io.airlift.test;

                import java.util.List;
                import com.google.common.collect.ImmutableList;

                public class Test
                {
                    public void method()
                    {
                        // Some code
                    }
                }
                """;

        String actual = organizer.organizeImports(input);

        // Verify the code after imports is preserved
        assertEquals(true, actual.contains("public class Test"));
        assertEquals(true, actual.contains("public void method()"));
        assertEquals(true, actual.contains("// Some code"));
    }

    @Test
    void testNoImports()
    {
        String input = """
                package io.airlift.test;

                public class Test {}
                """;

        String actual = organizer.organizeImports(input);
        assertEquals(input, actual);
    }

    @Test
    void testOnlyJavaImports()
    {
        String input = """
                package io.airlift.test;

                import java.util.Set;
                import java.util.List;
                import java.util.Map;

                public class Test {}
                """;

        String expected = """
                package io.airlift.test;

                import java.util.List;
                import java.util.Map;
                import java.util.Set;

                public class Test {}
                """;

        String actual = organizer.organizeImports(input);
        assertEquals(expected, actual);
    }

    @Test
    void testOnlyOtherImports()
    {
        String input = """
                package io.airlift.test;

                import com.google.common.collect.ImmutableSet;
                import com.google.common.collect.ImmutableList;

                public class Test {}
                """;

        String expected = """
                package io.airlift.test;

                import com.google.common.collect.ImmutableList;
                import com.google.common.collect.ImmutableSet;

                public class Test {}
                """;

        String actual = organizer.organizeImports(input);
        assertEquals(expected, actual);
    }

    @Test
    void testJavaxImports()
    {
        String input = """
                package io.airlift.test;

                import java.util.List;
                import com.google.inject.Inject;
                import javax.annotation.Nullable;
                import javax.annotation.PostConstruct;

                public class Test {}
                """;

        String expected = """
                package io.airlift.test;

                import com.google.inject.Inject;

                import javax.annotation.Nullable;
                import javax.annotation.PostConstruct;

                import java.util.List;

                public class Test {}
                """;

        String actual = organizer.organizeImports(input);
        assertEquals(expected, actual);
    }
}
