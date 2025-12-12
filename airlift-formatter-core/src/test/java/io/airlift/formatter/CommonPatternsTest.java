/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * Tests for common coding patterns that require careful formatting preservation.
 *
 * <p>These tests verify that the formatter correctly handles real-world patterns
 * that are commonly used in Airlift/Trino codebases.
 */
@TestInstance(PER_CLASS)
@DisplayName("Common Formatting Patterns")
public class CommonPatternsTest
{
    private AirliftFormatter formatter;

    @BeforeAll
    void setup()
    {
        formatter = new AirliftFormatter();
    }

    // =========================================================================
    // Builder Pattern
    // =========================================================================

    @Nested
    @DisplayName("Builder Pattern")
    class BuilderPatternTests
    {
        @Test
        @DisplayName("Fluent builder chain - preserve line breaks")
        void testFluentBuilderChain()
        {
            String input = """
                    class Test
                    {
                        void method()
                        {
                            Foo result = Foo.builder()
                                    .name("test")
                                    .value(42)
                                    .enabled(true)
                                    .build();
                        }
                    }
                    """;

            String output = formatter.format(input);

            // Each builder method should be on its own line
            assertTrue(output.contains(".name(\"test\")"),
                    "Builder chain should preserve line breaks");
            assertTrue(output.contains(".value(42)"),
                    "Builder chain should preserve line breaks");
            assertTrue(output.contains(".build();"),
                    "Builder chain should preserve line breaks");
        }

        @Test
        @DisplayName("Builder with long arguments")
        void testBuilderWithLongArguments()
        {
            String input = """
                    class Test
                    {
                        void method()
                        {
                            Config config = Config.builder()
                                    .connectionString("jdbc:postgresql://localhost:5432/database")
                                    .timeout(Duration.ofSeconds(30))
                                    .retryPolicy(RetryPolicy.exponentialBackoff())
                                    .build();
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains(".connectionString("),
                    "Long builder arguments should be preserved");
        }

        @Test
        @DisplayName("Inline builder - preserve compact form")
        void testInlineBuilder()
        {
            String input = """
                    class Test
                    {
                        void method()
                        {
                            Foo result = Foo.builder().name("x").build();
                        }
                    }
                    """;

            String output = formatter.format(input);

            // Compact form should remain compact
            assertTrue(output.contains("Foo.builder().name(\"x\").build()"),
                    "Inline builder should preserve compact form");
        }
    }

    // =========================================================================
    // Method Chaining with Lambdas
    // =========================================================================

    @Nested
    @DisplayName("Method Chaining with Lambdas")
    class MethodChainingTests
    {
        @Test
        @DisplayName("Stream with lambdas - preserve line breaks")
        void testStreamWithLambdas()
        {
            String input = """
                    class Test
                    {
                        void method(List<String> items)
                        {
                            List<String> result = items.stream()
                                    .filter(s -> !s.isEmpty())
                                    .map(String::toUpperCase)
                                    .sorted()
                                    .collect(toList());
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains(".filter(s -> !s.isEmpty())"),
                    "Stream chain should preserve line breaks");
            assertTrue(output.contains(".map(String::toUpperCase)"),
                    "Method references should be preserved");
        }

        @Test
        @DisplayName("Stream with multi-line lambdas")
        void testStreamWithMultiLineLambdas()
        {
            String input = """
                    class Test
                    {
                        void method(List<String> items)
                        {
                            items.stream()
                                    .filter(s -> {
                                        if (s.isEmpty()) {
                                            return false;
                                        }
                                        return s.startsWith("A");
                                    })
                                    .forEach(System.out::println);
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains(".filter(s -> {"),
                    "Multi-line lambda should be preserved");
            assertTrue(output.contains("if (s.isEmpty())"),
                    "Lambda body should be preserved");
        }

        @Test
        @DisplayName("CompletableFuture chain")
        void testCompletableFutureChain()
        {
            String input = """
                    class Test
                    {
                        CompletableFuture<String> method()
                        {
                            return CompletableFuture.supplyAsync(() -> fetchData())
                                    .thenApply(this::transform)
                                    .thenCompose(this::asyncOperation)
                                    .exceptionally(ex -> {
                                        log.error("Error", ex);
                                        return defaultValue();
                                    });
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains(".thenApply(this::transform)"),
                    "CompletableFuture chain should be preserved");
        }
    }

    // =========================================================================
    // Ternary Expressions
    // =========================================================================

    @Nested
    @DisplayName("Ternary Expressions")
    class TernaryExpressionTests
    {
        @Test
        @DisplayName("Simple ternary on one line")
        void testSimpleTernaryOneLine()
        {
            String input = """
                    class Test
                    {
                        String method(boolean flag)
                        {
                            return flag ? "yes" : "no";
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("flag ? \"yes\" : \"no\""),
                    "Simple ternary should stay on one line");
        }

        @Test
        @DisplayName("Multi-line ternary with method calls")
        void testMultiLineTernary()
        {
            String input = """
                    class Test
                    {
                        String method(boolean flag)
                        {
                            return flag
                                    ? computeYes()
                                    : computeNo();
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("? computeYes()"),
                    "Multi-line ternary should preserve breaks");
            assertTrue(output.contains(": computeNo()"),
                    "Multi-line ternary should preserve breaks");
        }

        @Test
        @DisplayName("Nested ternary expressions")
        void testNestedTernary()
        {
            String input = """
                    class Test
                    {
                        String method(int value)
                        {
                            return value < 0
                                    ? "negative"
                                    : value == 0
                                            ? "zero"
                                            : "positive";
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("value < 0"),
                    "Nested ternary structure should be preserved");
        }
    }

    // =========================================================================
    // Array Initializers
    // =========================================================================

    @Nested
    @DisplayName("Array Initializers")
    class ArrayInitializerTests
    {
        @Test
        @DisplayName("Multi-line array initializer")
        void testMultiLineArrayInitializer()
        {
            String input = """
                    class Test
                    {
                        int[] data = {
                                1, 2, 3,
                                4, 5, 6,
                                7, 8, 9,
                        };
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("1, 2, 3,"),
                    "Array rows should be preserved");
            assertTrue(output.contains("4, 5, 6,"),
                    "Array rows should be preserved");
        }

        @Test
        @DisplayName("Inline array initializer")
        void testInlineArrayInitializer()
        {
            String input = """
                    class Test
                    {
                        int[] data = {1, 2, 3, 4, 5};
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("{1, 2, 3, 4, 5}") || output.contains("{ 1, 2, 3, 4, 5 }"),
                    "Inline array should stay compact");
        }

        @Test
        @DisplayName("Object array with new expressions")
        void testObjectArrayInitializer()
        {
            String input = """
                    class Test
                    {
                        Point[] points = {
                                new Point(0, 0),
                                new Point(1, 0),
                                new Point(0, 1),
                        };
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("new Point(0, 0),"),
                    "Object array elements should be preserved");
        }
    }

    // =========================================================================
    // Annotation Stacking
    // =========================================================================

    @Nested
    @DisplayName("Annotation Stacking")
    class AnnotationStackingTests
    {
        @Test
        @DisplayName("Multiple annotations on method")
        void testMultipleAnnotationsOnMethod()
        {
            String input = """
                    class Test
                    {
                        @Nullable
                        @Override
                        @SuppressWarnings("unchecked")
                        public String method()
                        {
                            return null;
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("@Nullable"),
                    "Annotations should be preserved");
            assertTrue(output.contains("@Override"),
                    "Annotations should be preserved");
            assertTrue(output.contains("@SuppressWarnings"),
                    "Annotations should be preserved");
        }

        @Test
        @DisplayName("Inline annotations on parameters")
        void testInlineAnnotationsOnParameters()
        {
            String input = """
                    class Test
                    {
                        void method(@Nullable String name, @NotNull String value)
                        {
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("@Nullable String name"),
                    "Inline parameter annotation should be preserved");
            assertTrue(output.contains("@NotNull String value"),
                    "Inline parameter annotation should be preserved");
        }

        @Test
        @DisplayName("Annotation with parameters")
        void testAnnotationWithParameters()
        {
            String input = """
                    class Test
                    {
                        @JsonProperty(value = "user_name", required = true)
                        @NotNull
                        private String userName;
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("@JsonProperty(value = \"user_name\", required = true)"),
                    "Annotation parameters should be preserved");
        }
    }

    // =========================================================================
    // Complex Generics
    // =========================================================================

    @Nested
    @DisplayName("Complex Generics")
    class ComplexGenericsTests
    {
        @Test
        @DisplayName("Deeply nested generics")
        void testDeeplyNestedGenerics()
        {
            String input = """
                    class Test
                    {
                        Map<String, List<Function<Integer, Optional<String>>>> map;
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("Map<String, List<Function<Integer, Optional<String>>>>"),
                    "Nested generics should be preserved");
        }

        @Test
        @DisplayName("Wildcards with bounds")
        void testWildcardsWithBounds()
        {
            String input = """
                    class Test
                    {
                        void method(List<? extends Number> numbers, List<? super Integer> integers)
                        {
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("List<? extends Number>"),
                    "Wildcard with upper bound should be preserved");
            assertTrue(output.contains("List<? super Integer>"),
                    "Wildcard with lower bound should be preserved");
        }

        @Test
        @DisplayName("Generic method with multiple type parameters")
        void testGenericMethodMultipleTypes()
        {
            String input = """
                    class Test
                    {
                        <K, V extends Comparable<V>> Map<K, V> sort(Map<K, V> input)
                        {
                            return input;
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("<K, V extends Comparable<V>>"),
                    "Multiple type parameters should be preserved");
        }
    }

    // =========================================================================
    // Try-With-Resources
    // =========================================================================

    @Nested
    @DisplayName("Try-With-Resources")
    class TryWithResourcesTests
    {
        @Test
        @DisplayName("Single resource")
        void testSingleResource()
        {
            String input = """
                    class Test
                    {
                        void method()
                        {
                            try (InputStream is = new FileInputStream("file.txt")) {
                                process(is);
                            }
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("try (InputStream is ="),
                    "Single resource should be preserved");
        }

        @Test
        @DisplayName("Multiple resources")
        void testMultipleResources()
        {
            String input = """
                    class Test
                    {
                        void method()
                        {
                            try (InputStream is = new FileInputStream("in.txt");
                                    OutputStream os = new FileOutputStream("out.txt")) {
                                transfer(is, os);
                            }
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("InputStream is ="),
                    "First resource should be preserved");
            assertTrue(output.contains("OutputStream os ="),
                    "Second resource should be preserved");
        }

        @Test
        @DisplayName("Effectively final resources")
        void testEffectivelyFinalResources()
        {
            String input = """
                    class Test
                    {
                        void method(InputStream in, OutputStream out)
                        {
                            try (in; out) {
                                transfer(in, out);
                            }
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("try (in; out)"),
                    "Effectively final resources should be preserved");
        }
    }

    // =========================================================================
    // Long Method Signatures
    // =========================================================================

    @Nested
    @DisplayName("Long Method Signatures")
    class LongMethodSignatureTests
    {
        @Test
        @DisplayName("Long parameter list - wrapped")
        void testLongParameterListWrapped()
        {
            String input = """
                    class Test
                    {
                        void method(
                                String firstName,
                                String lastName,
                                String email,
                                String phone,
                                String address)
                        {
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("String firstName,"),
                    "Wrapped parameters should be preserved");
        }

        @Test
        @DisplayName("Method with throws clause")
        void testMethodWithThrowsClause()
        {
            String input = """
                    class Test
                    {
                        void method(String input)
                                throws IOException, SQLException, IllegalStateException
                        {
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("throws IOException"),
                    "Throws clause should be preserved");
        }
    }

    // =========================================================================
    // Comment Preservation
    // =========================================================================

    @Nested
    @DisplayName("Comment Preservation")
    class CommentPreservationTests
    {
        @Test
        @DisplayName("Trailing comments on same line")
        void testTrailingComments()
        {
            String input = """
                    class Test
                    {
                        int x = 1; // first value
                        int y = 2; // second value
                        int z = 3; // third value
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("// first value"),
                    "Trailing comments should be preserved");
        }

        @Test
        @DisplayName("Block comment before method")
        void testBlockCommentBeforeMethod()
        {
            String input = """
                    class Test
                    {
                        /* This is a legacy method
                         * that should not be used
                         * in new code */
                        @Deprecated
                        void oldMethod()
                        {
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("/* This is a legacy method"),
                    "Block comment should be preserved");
        }

        @Test
        @DisplayName("Inline comments in code")
        void testInlineComments()
        {
            String input = """
                    class Test
                    {
                        void method()
                        {
                            int result = compute() // get initial value
                                    + adjustment   // apply adjustment
                                    - offset;      // remove offset
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("// get initial value"),
                    "Inline comments should be preserved");
        }
    }

    // =========================================================================
    // Lambda Expressions
    // =========================================================================

    @Nested
    @DisplayName("Lambda Expressions")
    class LambdaExpressionTests
    {
        @Test
        @DisplayName("Simple expression lambda")
        void testSimpleExpressionLambda()
        {
            String input = """
                    class Test
                    {
                        void method(List<String> list)
                        {
                            list.forEach(s -> System.out.println(s));
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("s -> System.out.println(s)"),
                    "Expression lambda should be preserved");
        }

        @Test
        @DisplayName("Block lambda with multiple statements")
        void testBlockLambda()
        {
            String input = """
                    class Test
                    {
                        void method(List<String> list)
                        {
                            list.forEach(s -> {
                                log.info("Processing: {}", s);
                                process(s);
                            });
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("s -> {"),
                    "Block lambda should be preserved");
            assertTrue(output.contains("log.info("),
                    "Lambda body should be preserved");
        }

        @Test
        @DisplayName("Lambda with explicit types")
        void testLambdaWithExplicitTypes()
        {
            String input = """
                    class Test
                    {
                        BiFunction<String, Integer, String> fn = (String s, Integer i) -> s.repeat(i);
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("(String s, Integer i) ->"),
                    "Explicit lambda types should be preserved");
        }
    }
}
