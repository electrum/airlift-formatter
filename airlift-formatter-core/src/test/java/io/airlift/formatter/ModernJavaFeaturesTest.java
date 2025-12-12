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
 * Tests for modern Java features (Java 21-25+).
 *
 * <p>These tests verify that the formatter correctly handles syntax from
 * recent Java versions, including:
 * <ul>
 *   <li>Records and record patterns (Java 21)</li>
 *   <li>Sealed classes and interfaces (Java 17+)</li>
 *   <li>Pattern matching for switch (Java 21)</li>
 *   <li>Text blocks (Java 15+)</li>
 *   <li>Switch expressions (Java 14+)</li>
 *   <li>Unnamed variables and patterns (Java 22+)</li>
 *   <li>String templates (Java 21+ preview, finalized in 25)</li>
 *   <li>Primitive types in patterns (Java 23+)</li>
 *   <li>Flexible constructor bodies (Java 22+)</li>
 *   <li>Module import declarations (Java 23+)</li>
 * </ul>
 */
@TestInstance(PER_CLASS)
@DisplayName("Modern Java Features (21-25+)")
public class ModernJavaFeaturesTest
{
    private AirliftFormatter formatter;

    @BeforeAll
    void setup()
    {
        formatter = new AirliftFormatter();
    }

    // =========================================================================
    // Records (Java 16+, finalized Java 21)
    // =========================================================================

    @Nested
    @DisplayName("Records")
    class RecordTests
    {
        @Test
        @DisplayName("Simple record declaration")
        void testSimpleRecord()
        {
            String input = """
                    public record Point(int x, int y)
                    {
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("record Point(int x, int y)"),
                    "Record declaration should be preserved");
            assertTrue(output.contains("{"),
                    "Record should have brace");
        }

        @Test
        @DisplayName("Record with compact constructor")
        void testRecordWithCompactConstructor()
        {
            String input = """
                    public record Person(String name, int age)
                    {
                        public Person
                        {
                            if (age < 0) {
                                throw new IllegalArgumentException("Age cannot be negative");
                            }
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("public Person"),
                    "Compact constructor should be preserved");
        }

        @Test
        @DisplayName("Record with static members")
        void testRecordWithStaticMembers()
        {
            String input = """
                    public record Config(String host, int port)
                    {
                        public static final Config DEFAULT = new Config("localhost", 8080);

                        public static Config fromEnv()
                        {
                            return DEFAULT;
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("public static final Config DEFAULT"),
                    "Static field should be preserved");
            assertTrue(output.contains("public static Config fromEnv()"),
                    "Static method should be preserved");
        }

        @Test
        @DisplayName("Nested records")
        void testNestedRecords()
        {
            String input = """
                    public record Outer(Inner inner)
                    {
                        public record Inner(String value)
                        {
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("record Outer(Inner inner)"),
                    "Outer record should be preserved");
            assertTrue(output.contains("record Inner(String value)"),
                    "Inner record should be preserved");
        }

        @Test
        @DisplayName("Record with generics")
        void testRecordWithGenerics()
        {
            String input = """
                    public record Pair<T, U>(T first, U second)
                    {
                        public static <T, U> Pair<T, U> of(T first, U second)
                        {
                            return new Pair<>(first, second);
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("record Pair<T, U>(T first, U second)"),
                    "Generic record should be preserved");
        }
    }

    // =========================================================================
    // Record Patterns (Java 21)
    // =========================================================================

    @Nested
    @DisplayName("Record Patterns")
    class RecordPatternTests
    {
        @Test
        @DisplayName("Simple record pattern in switch")
        void testSimpleRecordPattern()
        {
            String input = """
                    class Test
                    {
                        String describe(Object obj)
                        {
                            return switch (obj) {
                                case Point(int x, int y) -> "Point at " + x + ", " + y;
                                case null, default -> "Unknown";
                            };
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("case Point(int x, int y)"),
                    "Record pattern should be preserved");
        }

        @Test
        @DisplayName("Nested record patterns")
        void testNestedRecordPatterns()
        {
            String input = """
                    class Test
                    {
                        String describe(Object obj)
                        {
                            return switch (obj) {
                                case Line(Point(int x1, int y1), Point(int x2, int y2)) ->
                                        "Line from (" + x1 + "," + y1 + ") to (" + x2 + "," + y2 + ")";
                                default -> "Unknown";
                            };
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("case Line(Point(int x1, int y1), Point(int x2, int y2))"),
                    "Nested record pattern should be preserved");
        }

        @Test
        @DisplayName("Record pattern with when guard")
        void testRecordPatternWithGuard()
        {
            String input = """
                    class Test
                    {
                        String quadrant(Point p)
                        {
                            return switch (p) {
                                case Point(int x, int y) when x > 0 && y > 0 -> "Q1";
                                case Point(int x, int y) when x < 0 && y > 0 -> "Q2";
                                case Point(int x, int y) when x < 0 && y < 0 -> "Q3";
                                case Point(int x, int y) when x > 0 && y < 0 -> "Q4";
                                default -> "Origin or axis";
                            };
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("when x > 0 && y > 0"),
                    "Guard clause should be preserved");
        }

        @Test
        @DisplayName("Record pattern in instanceof")
        void testRecordPatternInInstanceof()
        {
            String input = """
                    class Test
                    {
                        void process(Object obj)
                        {
                            if (obj instanceof Point(int x, int y)) {
                                System.out.println("Point: " + x + ", " + y);
                            }
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("instanceof Point(int x, int y)"),
                    "Record pattern in instanceof should be preserved");
        }
    }

    // =========================================================================
    // Sealed Classes (Java 17+)
    // =========================================================================

    @Nested
    @DisplayName("Sealed Classes")
    class SealedClassTests
    {
        @Test
        @DisplayName("Sealed class with permits")
        void testSealedClassWithPermits()
        {
            String input = """
                    public sealed class Shape permits Circle, Rectangle, Square
                    {
                        public abstract double area();
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("sealed class Shape permits Circle, Rectangle, Square"),
                    "Sealed class with permits should be preserved");
        }

        @Test
        @DisplayName("Final subclass of sealed")
        void testFinalSubclass()
        {
            String input = """
                    public final class Circle extends Shape
                    {
                        private final double radius;

                        public Circle(double radius)
                        {
                            this.radius = radius;
                        }

                        @Override
                        public double area()
                        {
                            return Math.PI * radius * radius;
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("public final class Circle extends Shape"),
                    "Final subclass should be preserved");
        }

        @Test
        @DisplayName("Non-sealed subclass")
        void testNonSealedSubclass()
        {
            String input = """
                    public non-sealed class Rectangle extends Shape
                    {
                        protected final double width;
                        protected final double height;

                        public Rectangle(double width, double height)
                        {
                            this.width = width;
                            this.height = height;
                        }

                        @Override
                        public double area()
                        {
                            return width * height;
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("non-sealed class Rectangle"),
                    "Non-sealed class should be preserved");
        }

        @Test
        @DisplayName("Sealed interface")
        void testSealedInterface()
        {
            String input = """
                    public sealed interface Expr permits Const, Add, Mul
                    {
                        int eval();
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("sealed interface Expr permits"),
                    "Sealed interface should be preserved");
        }
    }

    // =========================================================================
    // Pattern Matching for Switch (Java 21)
    // =========================================================================

    @Nested
    @DisplayName("Pattern Matching for Switch")
    class PatternMatchingSwitchTests
    {
        @Test
        @DisplayName("Type pattern in switch")
        void testTypePatternInSwitch()
        {
            String input = """
                    class Test
                    {
                        String format(Object obj)
                        {
                            return switch (obj) {
                                case Integer i -> String.format("int %d", i);
                                case Long l -> String.format("long %d", l);
                                case Double d -> String.format("double %f", d);
                                case String s -> String.format("String %s", s);
                                default -> obj.toString();
                            };
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("case Integer i ->"),
                    "Type pattern should be preserved");
            assertTrue(output.contains("case String s ->"),
                    "Type pattern should be preserved");
        }

        @Test
        @DisplayName("Null case in switch")
        void testNullCaseInSwitch()
        {
            String input = """
                    class Test
                    {
                        String describe(Object obj)
                        {
                            return switch (obj) {
                                case null -> "null";
                                case String s -> "string: " + s;
                                default -> "other";
                            };
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("case null ->"),
                    "Null case should be preserved");
        }

        @Test
        @DisplayName("Combined null and default case")
        void testNullDefaultCase()
        {
            String input = """
                    class Test
                    {
                        String describe(Object obj)
                        {
                            return switch (obj) {
                                case String s -> "string: " + s;
                                case null, default -> "other or null";
                            };
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("case null, default ->"),
                    "Combined null/default case should be preserved");
        }

        @Test
        @DisplayName("Guarded patterns with when")
        void testGuardedPatterns()
        {
            String input = """
                    class Test
                    {
                        String describe(Integer i)
                        {
                            return switch (i) {
                                case Integer n when n < 0 -> "negative";
                                case Integer n when n == 0 -> "zero";
                                case Integer n when n > 0 -> "positive";
                                default -> "unreachable";
                            };
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("when n < 0"),
                    "Guard clause should be preserved");
        }
    }

    // =========================================================================
    // Switch Expressions (Java 14+)
    // =========================================================================

    @Nested
    @DisplayName("Switch Expressions")
    class SwitchExpressionTests
    {
        @Test
        @DisplayName("Arrow switch expression")
        void testArrowSwitchExpression()
        {
            String input = """
                    class Test
                    {
                        int numLetters(String day)
                        {
                            return switch (day) {
                                case "MONDAY", "FRIDAY", "SUNDAY" -> 6;
                                case "TUESDAY" -> 7;
                                case "THURSDAY", "SATURDAY" -> 8;
                                case "WEDNESDAY" -> 9;
                                default -> throw new IllegalArgumentException();
                            };
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("case \"MONDAY\", \"FRIDAY\", \"SUNDAY\" -> 6"),
                    "Multiple case labels should be preserved");
        }

        @Test
        @DisplayName("Yield in switch expression")
        void testYieldInSwitchExpression()
        {
            String input = """
                    class Test
                    {
                        String describe(int value)
                        {
                            return switch (value) {
                                case 1 -> "one";
                                case 2 -> "two";
                                default -> {
                                    String result = "number: " + value;
                                    yield result;
                                }
                            };
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("yield result;"),
                    "Yield statement should be preserved");
        }
    }

    // =========================================================================
    // Text Blocks (Java 15+)
    // =========================================================================

    @Nested
    @DisplayName("Text Blocks")
    class TextBlockTests
    {
        @Test
        @DisplayName("Simple text block")
        void testSimpleTextBlock()
        {
            String input = """
                    class Test
                    {
                        String html = \"""
                                <html>
                                    <body>
                                        <p>Hello, World</p>
                                    </body>
                                </html>
                                \""";
                    }
                    """;

            String output = formatter.format(input);

            // Text block content should be preserved
            assertTrue(output.contains("<html>"),
                    "Text block content should be preserved");
        }

        @Test
        @DisplayName("Text block with JSON")
        void testTextBlockWithJson()
        {
            String input = """
                    class Test
                    {
                        String json = \"""
                                {
                                    "name": "John",
                                    "age": 30,
                                    "city": "New York"
                                }
                                \""";
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("\"name\": \"John\""),
                    "JSON content should be preserved");
        }

        @Test
        @DisplayName("Text block with SQL")
        void testTextBlockWithSql()
        {
            String input = """
                    class Test
                    {
                        String sql = \"""
                                SELECT id, name, email
                                FROM users
                                WHERE status = 'active'
                                ORDER BY created_at DESC
                                \""";
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("SELECT id, name, email"),
                    "SQL content should be preserved");
        }
    }

    // =========================================================================
    // Unnamed Variables and Patterns (Java 22+)
    // =========================================================================

    @Nested
    @DisplayName("Unnamed Variables (Java 22+)")
    class UnnamedVariableTests
    {
        @Test
        @DisplayName("Unnamed variable in try-with-resources")
        void testUnnamedVariableInTry()
        {
            String input = """
                    class Test
                    {
                        void process()
                        {
                            try (var _ = ScopedContext.acquire()) {
                                doWork();
                            }
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("var _ ="),
                    "Unnamed variable should be preserved");
        }

        @Test
        @DisplayName("Unnamed variable in lambda")
        void testUnnamedVariableInLambda()
        {
            String input = """
                    class Test
                    {
                        void process(List<String> list)
                        {
                            list.forEach(_ -> count++);
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("_ ->"),
                    "Unnamed lambda parameter should be preserved");
        }

        @Test
        @DisplayName("Unnamed variable in catch")
        void testUnnamedVariableInCatch()
        {
            String input = """
                    class Test
                    {
                        void process()
                        {
                            try {
                                riskyOperation();
                            }
                            catch (Exception _) {
                                handleError();
                            }
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("catch (Exception _)"),
                    "Unnamed exception variable should be preserved");
        }

        @Test
        @DisplayName("Unnamed pattern in switch")
        void testUnnamedPatternInSwitch()
        {
            String input = """
                    class Test
                    {
                        void process(Box box)
                        {
                            switch (box) {
                                case Box(RedBall _) -> processRed();
                                case Box(BlueBall _) -> processBlue();
                                case Box(_) -> processOther();
                            }
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("Box(RedBall _)"),
                    "Unnamed pattern should be preserved");
            assertTrue(output.contains("Box(_)"),
                    "Unnamed wildcard pattern should be preserved");
        }
    }

    // =========================================================================
    // String Templates (Java 21 preview, Java 25+ finalized)
    // =========================================================================

    @Nested
    @DisplayName("String Templates (Java 25+)")
    class StringTemplateTests
    {
        @Test
        @DisplayName("Simple string template with STR")
        void testSimpleStringTemplate()
        {
            String input = """
                    class Test
                    {
                        String greet(String name)
                        {
                            return STR."Hello, \\{name}!";
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("STR.\"Hello"),
                    "STR template processor should be preserved");
        }

        @Test
        @DisplayName("String template with expressions")
        void testStringTemplateWithExpressions()
        {
            String input = """
                    class Test
                    {
                        String describe(int x, int y)
                        {
                            return STR."The point (\\{x}, \\{y}) has sum \\{x + y}";
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("STR.\"The point"),
                    "String template with expressions should be preserved");
        }

        @Test
        @DisplayName("Multi-line string template")
        void testMultiLineStringTemplate()
        {
            String input = """
                    class Test
                    {
                        String html(String title, String content)
                        {
                            return STR.\"""
                                <html>
                                    <head><title>\\{title}</title></head>
                                    <body>\\{content}</body>
                                </html>
                                \""";
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("<head><title>"),
                    "Multi-line string template should be preserved");
        }

        @Test
        @DisplayName("FMT template processor")
        void testFmtTemplateProcessor()
        {
            String input = """
                    class Test
                    {
                        String format(double value)
                        {
                            return FMT."Value: %.2f\\{value}";
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("FMT.\"Value"),
                    "FMT template processor should be preserved");
        }

        @Test
        @DisplayName("RAW template processor")
        void testRawTemplateProcessor()
        {
            String input = """
                    class Test
                    {
                        StringTemplate raw(String name)
                        {
                            return RAW."Hello, \\{name}!";
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("RAW.\"Hello"),
                    "RAW template processor should be preserved");
        }
    }

    // =========================================================================
    // Primitive Types in Patterns (Java 23+)
    // =========================================================================

    @Nested
    @DisplayName("Primitive Types in Patterns (Java 23+)")
    class PrimitivePatternTests
    {
        @Test
        @DisplayName("Primitive type pattern in switch")
        void testPrimitiveTypePatternInSwitch()
        {
            String input = """
                    class Test
                    {
                        String classify(int value)
                        {
                            return switch (value) {
                                case int i when i < 0 -> "negative";
                                case int i when i == 0 -> "zero";
                                case int i -> "positive: " + i;
                            };
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("case int i when"),
                    "Primitive type pattern should be preserved");
        }

        @Test
        @DisplayName("Primitive pattern in instanceof")
        void testPrimitivePatternInInstanceof()
        {
            String input = """
                    class Test
                    {
                        void process(Number n)
                        {
                            if (n instanceof int i) {
                                System.out.println("int: " + i);
                            }
                            else if (n instanceof long l) {
                                System.out.println("long: " + l);
                            }
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("instanceof int i"),
                    "Primitive instanceof pattern should be preserved");
        }
    }

    // =========================================================================
    // Flexible Constructor Bodies (Java 22+)
    // =========================================================================

    @Nested
    @DisplayName("Flexible Constructor Bodies (Java 22+)")
    class FlexibleConstructorTests
    {
        @Test
        @DisplayName("Statements before super()")
        void testStatementsBeforeSuper()
        {
            String input = """
                    class PositiveBigInteger extends BigInteger
                    {
                        public PositiveBigInteger(long value)
                        {
                            if (value <= 0) {
                                throw new IllegalArgumentException("Value must be positive");
                            }
                            super(value);
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("if (value <= 0)"),
                    "Validation before super should be preserved");
            assertTrue(output.contains("super(value);"),
                    "Super call should be preserved");
        }

        @Test
        @DisplayName("Validation before this()")
        void testStatementsBeforeThis()
        {
            String input = """
                    class Rectangle
                    {
                        private final int width;
                        private final int height;

                        public Rectangle(int width, int height)
                        {
                            this.width = width;
                            this.height = height;
                        }

                        public Rectangle(int side)
                        {
                            if (side <= 0) {
                                throw new IllegalArgumentException("Side must be positive");
                            }
                            this(side, side);
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("if (side <= 0)"),
                    "Validation before this() should be preserved");
            assertTrue(output.contains("this(side, side);"),
                    "this() call should be preserved");
        }
    }

    // =========================================================================
    // Module Import Declarations (Java 23+)
    // =========================================================================

    @Nested
    @DisplayName("Module Import Declarations (Java 23+)")
    class ModuleImportTests
    {
        @Test
        @DisplayName("Import module declaration")
        void testImportModuleDeclaration()
        {
            String input = """
                    import module java.base;

                    class Test
                    {
                        void process()
                        {
                            List<String> list = new ArrayList<>();
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("import module java.base;"),
                    "Module import should be preserved");
        }
    }

    // =========================================================================
    // Implicitly Declared Classes (Java 23+)
    // =========================================================================

    @Nested
    @DisplayName("Implicitly Declared Classes (Java 23+)")
    class ImplicitlyDeclaredClassTests
    {
        @Test
        @DisplayName("Simple implicitly declared class")
        void testImplicitlyDeclaredClass()
        {
            // Note: Implicitly declared classes don't have an explicit class declaration
            // The file contains just methods at the top level
            String input = """
                    void main()
                    {
                        System.out.println("Hello, World!");
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("void main()"),
                    "Implicit main method should be preserved");
        }

        @Test
        @DisplayName("Implicitly declared class with helper methods")
        void testImplicitClassWithHelpers()
        {
            String input = """
                    void main()
                    {
                        greet("World");
                    }

                    void greet(String name)
                    {
                        System.out.println("Hello, " + name + "!");
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("void main()"),
                    "Main method should be preserved");
            assertTrue(output.contains("void greet(String name)"),
                    "Helper method should be preserved");
        }
    }

    // =========================================================================
    // Complex Combinations
    // =========================================================================

    @Nested
    @DisplayName("Complex Feature Combinations")
    class ComplexCombinationTests
    {
        @Test
        @DisplayName("Sealed hierarchy with record patterns")
        void testSealedWithRecordPatterns()
        {
            String input = """
                    sealed interface Expr permits Const, Add, Mul
                    {
                    }

                    record Const(int value) implements Expr
                    {
                    }

                    record Add(Expr left, Expr right) implements Expr
                    {
                    }

                    record Mul(Expr left, Expr right) implements Expr
                    {
                    }

                    class Calculator
                    {
                        int eval(Expr expr)
                        {
                            return switch (expr) {
                                case Const(int value) -> value;
                                case Add(Expr left, Expr right) -> eval(left) + eval(right);
                                case Mul(Expr left, Expr right) -> eval(left) * eval(right);
                            };
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("sealed interface Expr"),
                    "Sealed interface should be preserved");
            assertTrue(output.contains("case Const(int value)"),
                    "Record pattern should be preserved");
        }

        @Test
        @DisplayName("Records with text blocks and patterns")
        void testRecordsWithTextBlocksAndPatterns()
        {
            String input = """
                    record HtmlTemplate(String title, String body)
                    {
                        String render()
                        {
                            return \"""
                                    <html>
                                        <head><title>%s</title></head>
                                        <body>%s</body>
                                    </html>
                                    \""".formatted(title, body);
                        }
                    }

                    class Processor
                    {
                        void process(Object template)
                        {
                            switch (template) {
                                case HtmlTemplate(String title, String body) when !title.isEmpty() ->
                                        System.out.println("Rendering: " + title);
                                case HtmlTemplate(_, _) ->
                                        System.out.println("Empty template");
                                default ->
                                        System.out.println("Unknown");
                            }
                        }
                    }
                    """;

            String output = formatter.format(input);

            assertTrue(output.contains("record HtmlTemplate"),
                    "Record should be preserved");
            assertTrue(output.contains("<html>"),
                    "Text block should be preserved");
            assertTrue(output.contains("case HtmlTemplate(String title, String body)"),
                    "Record pattern should be preserved");
        }
    }
}
