/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter;

import io.airlift.formatter.internal.jdt.CodeFormatter;
import io.airlift.formatter.internal.jdt.DefaultCodeFormatterConstants;
import io.airlift.formatter.internal.jdt.DefaultCodeFormatter;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Main entry point for the forked Airlift JDT formatter.
 *
 * <p>This formatter uses a forked copy of Eclipse JDT's formatter with
 * direct source code modifications to achieve Airlift-style formatting.
 * Unlike the bytecode-modified approach in jdt-mod, this allows
 * complete control over formatting decisions.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * AirliftFormatter formatter = new AirliftFormatter();
 * String formatted = formatter.format(sourceCode);
 * }</pre>
 *
 * <h2>CLI Usage</h2>
 * <pre>
 * java -jar airlift-jdt-core.jar file1.java file2.java ...
 * </pre>
 */
public class AirliftFormatter
{
    private final FormatterConfiguration configuration;
    private final DefaultCodeFormatter codeFormatter;
    private final AirliftImportOrganizer importOrganizer;
    // AirliftBraceNormalizer eliminated - brace positioning now handled directly in JDT's
    // LineBreaksPreparator.handleBracedCode() with IntelliJ-style enforcement
    // AirliftBlankLineNormalizer eliminated - blank lines now handled directly in JDT's
    // LineBreaksPreparator.handleBracedCode() using ~blankLinesBeforeClosingBrace

    /**
     * Creates a new AirliftFormatter with default Airlift options.
     */
    public AirliftFormatter()
    {
        this(FormatterConfiguration.airlift());
    }

    /**
     * Creates a new AirliftFormatter with a custom configuration.
     *
     * <p>Use this constructor to customize formatter behavior:
     * <pre>{@code
     * FormatterConfiguration config = FormatterConfiguration.builder()
     *     .classBraceStyle(BraceStyle.END_OF_LINE)
     *     .lineWidth(120)
     *     .build();
     * AirliftFormatter formatter = new AirliftFormatter(config);
     * }</pre>
     *
     * @param configuration the formatter configuration
     */
    public AirliftFormatter(FormatterConfiguration configuration)
    {
        this.configuration = Objects.requireNonNull(configuration, "configuration is null");
        this.codeFormatter = new DefaultCodeFormatter(configuration.toJdtOptions());
        // TODO: Pass configuration.getImportOrder() to AirliftImportOrganizer in Phase 4
        this.importOrganizer = new AirliftImportOrganizer();
    }

    /**
     * Creates a new AirliftFormatter with raw JDT options.
     *
     * <p>This constructor is provided for backward compatibility.
     * Consider using {@link #AirliftFormatter(FormatterConfiguration)} instead.
     *
     * @param options the JDT formatter options
     * @deprecated Use {@link #AirliftFormatter(FormatterConfiguration)} instead
     */
    @Deprecated
    public AirliftFormatter(Map<String, String> options)
    {
        this.configuration = null; // Raw options mode
        this.codeFormatter = new DefaultCodeFormatter(options);
        this.importOrganizer = new AirliftImportOrganizer();
    }

    /**
     * Returns the configuration used by this formatter.
     *
     * @return the formatter configuration, or null if using raw options
     */
    public FormatterConfiguration getConfiguration()
    {
        return configuration;
    }

    /**
     * Formats Java source code.
     *
     * @param source the source code to format
     * @return the formatted source code
     */
    public String format(String source)
    {
        return format(source, CodeFormatter.K_COMPILATION_UNIT);
    }

    /**
     * Formats Java source code with a specific kind.
     *
     * @param source the source code to format
     * @param kind the format kind (see {@link CodeFormatter})
     * @return the formatted source code
     */
    public String format(String source, int kind)
    {
        if (source == null || source.isEmpty()) {
            return source;
        }

        // Step 1: Organize imports first (for K_COMPILATION_UNIT only)
        String organized = source;
        if (kind == CodeFormatter.K_COMPILATION_UNIT) {
            organized = importOrganizer.organizeImports(source);
        }

        // Step 2: Brace placement now handled directly in JDT's
        // LineBreaksPreparator.handleBracedCode() with IntelliJ-style enforcement
        // No pre-processing needed for brace normalization

        // Step 3: Apply JDT formatting
        TextEdit edit = codeFormatter.format(kind, organized, 0, organized.length(), 0, "\n");

        if (edit == null) {
            // JDT couldn't parse the source, return as-is
            return organized;
        }

        try {
            IDocument document = new Document(organized);
            edit.apply(document);
            String formatted = document.get();

            // Step 4: Blank line normalization now handled directly in JDT's
            // LineBreaksPreparator.handleBracedCode() using ~blankLinesBeforeClosingBrace
            // No post-processing needed for blank lines

            return formatted;
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to apply formatting edits", e);
        }
    }

    /**
     * Formats a Java source file in place.
     *
     * @param path the path to the Java file
     * @throws IOException if reading/writing fails
     */
    public void formatFile(Path path)
            throws IOException
    {
        String source = Files.readString(path, StandardCharsets.UTF_8);
        String formatted = format(source);

        if (!source.equals(formatted)) {
            Files.writeString(path, formatted, StandardCharsets.UTF_8);
        }
    }

    /**
     * Formats a Java source file and returns the result without modifying the original.
     *
     * @param path the path to the Java file
     * @return the formatted source code
     * @throws IOException if reading fails
     */
    public String formatFileToString(Path path)
            throws IOException
    {
        String source = Files.readString(path, StandardCharsets.UTF_8);
        return format(source);
    }

    /**
     * Creates default Airlift formatting options.
     *
     * @return the options map
     */
    public static Map<String, String> createAirliftOptions()
    {
        Map<String, String> options = new HashMap<>(DefaultCodeFormatterConstants.getEclipseDefaultSettings());

        // Brace positions - Airlift uses next_line for class/method
        options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION, DefaultCodeFormatterConstants.NEXT_LINE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_METHOD_DECLARATION, DefaultCodeFormatterConstants.NEXT_LINE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_CONSTRUCTOR_DECLARATION, DefaultCodeFormatterConstants.NEXT_LINE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ENUM_DECLARATION, DefaultCodeFormatterConstants.NEXT_LINE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ANNOTATION_TYPE_DECLARATION, DefaultCodeFormatterConstants.NEXT_LINE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_RECORD_DECLARATION, DefaultCodeFormatterConstants.NEXT_LINE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_RECORD_CONSTRUCTOR, DefaultCodeFormatterConstants.NEXT_LINE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ANONYMOUS_TYPE_DECLARATION, DefaultCodeFormatterConstants.NEXT_LINE);

        // Control flow - Airlift uses end_of_line
        options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_BLOCK, DefaultCodeFormatterConstants.END_OF_LINE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_SWITCH, DefaultCodeFormatterConstants.END_OF_LINE);
        // Lambda body brace on same line as arrow
        options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_LAMBDA_BODY, DefaultCodeFormatterConstants.END_OF_LINE);

        // else/catch/finally on new line (gofmt-style: preserve do-while on same line as closing brace)
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_ELSE_IN_IF_STATEMENT, "insert");
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_CATCH_IN_TRY_STATEMENT, "insert");
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_FINALLY_IN_TRY_STATEMENT, "insert");
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_WHILE_IN_DO_STATEMENT, "do not insert");

        // Line width - set extremely high to avoid any unwanted line wrapping
        options.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, "9999");

        // Indentation
        options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, "space");
        options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
        options.put(DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE, "4");
        options.put(DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION, "2");

        // Blank lines - gofmt-style: preserve original, don't force any
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_PACKAGE, "0");
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_PACKAGE, "1");
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_IMPORTS, "1");
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_IMPORTS, "1");
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BETWEEN_IMPORT_GROUPS, "1");
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AT_BEGINNING_OF_CODE_BLOCK, "0");
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AT_END_OF_CODE_BLOCK, "0");
        // AIRLIFT MODIFICATION: Match IntelliJ's KEEP_BLANK_LINES_IN_CODE=1 setting
        options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "1");
        // Don't force blank lines between code chunks (gofmt-style: preserve original)
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_NEW_CHUNK, "0");
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIELD, "0");
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_METHOD, "0");
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_ABSTRACT_METHOD, "0");
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_MEMBER_TYPE, "0");
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIRST_CLASS_BODY_DECLARATION, "0");
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_LAST_CLASS_BODY_DECLARATION, "0");

        // Keep simple constructs on one line
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_SIMPLE_IF_ON_ONE_LINE, "false");
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_THEN_STATEMENT_ON_SAME_LINE, "false");
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_ELSE_STATEMENT_ON_SAME_LINE, "false");

        // Keep empty anonymous type declarations on one line
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_ANONYMOUS_TYPE_DECLARATION_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ANONYMOUS_TYPE_DECLARATION, "do not insert");

        // Lambda body blocks on one line - preserve existing style
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_LAMBDA_BODY_BLOCK_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);

        // Keep method/type bodies on one line - preserve original formatting (gofmt-style)
        // This enables OneLineEnforcer to preserve compact bodies like: private Foo() {}
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_METHOD_BODY_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_TYPE_DECLARATION_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_RECORD_DECLARATION_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_RECORD_CONSTRUCTOR_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_ENUM_DECLARATION_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_ANNOTATION_DECLARATION_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_ENUM_CONSTANT_DECLARATION_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);

        // CRITICAL: Do not join already-wrapped lines
        options.put(DefaultCodeFormatterConstants.FORMATTER_JOIN_WRAPPED_LINES, "false");

        // Do not wrap outer expressions when formatting nested structures
        options.put(DefaultCodeFormatterConstants.FORMATTER_WRAP_OUTER_EXPRESSIONS_WHEN_NESTED, "false");

        // Use WRAP_COMPACT to preserve existing line breaks
        String wrapCompact = DefaultCodeFormatterConstants.createAlignmentValue(false, DefaultCodeFormatterConstants.WRAP_COMPACT);

        // Method/constructor parameters
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_METHOD_DECLARATION, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_CONSTRUCTOR_DECLARATION, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_METHOD_INVOCATION, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ALLOCATION_EXPRESSION, wrapCompact);

        // Method chains
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SELECTOR_IN_METHOD_INVOCATION, wrapCompact);

        // String concatenation
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_STRING_CONCATENATION, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_STRING_CONCATENATION, "false");

        // Binary expressions
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ADDITIVE_OPERATOR, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_MULTIPLICATIVE_OPERATOR, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_LOGICAL_OPERATOR, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_BITWISE_OPERATOR, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_RELATIONAL_OPERATOR, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_CONDITIONAL_EXPRESSION, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SHIFT_OPERATOR, wrapCompact);

        // Binary operator wrapping - keep + at end of line
        options.put(DefaultCodeFormatterConstants.FORMATTER_WRAP_BEFORE_BINARY_OPERATOR, "false");

        // Assignment
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ASSIGNMENT, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_COMPACT_IF, wrapCompact);

        // Throws/extends
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_METHOD_DECLARATION, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_THROWS_CLAUSE_IN_CONSTRUCTOR_DECLARATION, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERCLASS_IN_TYPE_DECLARATION, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_TYPE_DECLARATION, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SUPERINTERFACES_IN_ENUM_DECLARATION, wrapCompact);

        // Record components
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_RECORD_COMPONENTS, wrapCompact);

        // Enum/annotations
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ENUM_CONSTANTS, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_TYPE, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_ENUM_CONSTANT, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_FIELD, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_METHOD, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_PACKAGE, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_PARAMETER, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ANNOTATIONS_ON_LOCAL_VARIABLE, wrapCompact);

        // Array initializers - no spaces inside braces
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER, "do not insert");
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER, "do not insert");
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_BRACES_IN_ARRAY_INITIALIZER, "do not insert");

        // For loop
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON_IN_FOR, "do not insert");

        // CRITICAL: Preserve original whitespace before trailing line comments
        // This prevents JDT from changing aligned comment spacing like:
        //   code;   // comment  ->  code; // comment
        options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_PRESERVE_WHITE_SPACE_BETWEEN_CODE_AND_LINE_COMMENT, "true");

        // Switch statement indentation
        options.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_SWITCH, "true");
        options.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_CASES, "true");

        // Switch case with arrow
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_SWITCH_CASE_WITH_ARROW_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_SWITCH_BODY_BLOCK_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);

        // Code blocks
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_CODE_BLOCK_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_NEVER);

        // Text blocks - preserve original indentation (gofmt-style)
        options.put(DefaultCodeFormatterConstants.FORMATTER_TEXT_BLOCK_INDENTATION, Integer.toString(DefaultCodeFormatterConstants.INDENT_PRESERVE));

        // Annotations - preserve developer's choice of same-line vs separate line (gofmt-style)
        // Setting to "do not insert" means annotations won't be forced to separate lines
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_ENUM_CONSTANT, "do not insert");
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_FIELD, "do not insert");
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_METHOD, "do not insert");
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_PARAMETER, "do not insert");
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE, "do not insert");

        // Comments at column 0 - preserve original position (gofmt-style)
        // Developer may intentionally put block/line comments at column 0, don't indent them
        options.put(DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_BLOCK_COMMENTS_ON_FIRST_COLUMN, "true");
        options.put(DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_LINE_COMMENTS_ON_FIRST_COLUMN, "true");

        // Empty lines - do not add indentation to empty lines (avoid trailing whitespace)
        options.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_EMPTY_LINES, "false");

        // Block comments - do not format content inside block comments (gofmt-style)
        // Developer formatting inside /* */ should be preserved exactly as written
        options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT, "false");
        // Javadoc markdown comments (///) are line-sensitive; preserve original wrapping
        options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_MARKDOWN_COMMENT, "false");

        return options;
    }

    /**
     * CLI main entry point.
     */
    public static void main(String[] args)
    {
        if (args.length == 0) {
            System.out.println("Airlift JDT Core Formatter (Forked)");
            System.out.println("Usage: java -jar airlift-jdt-core.jar [options] <files...>");
            System.out.println();
            System.out.println("Options:");
            System.out.println("  --check    Check if files are formatted (exit 1 if not)");
            System.exit(0);
        }

        boolean checkMode = false;
        int filesProcessed = 0;
        int filesChanged = 0;

        AirliftFormatter formatter = new AirliftFormatter();

        for (String arg : args) {
            if ("--check".equals(arg)) {
                checkMode = true;
                continue;
            }

            try {
                Path path = Path.of(arg);
                if (!Files.exists(path)) {
                    System.err.println("File not found: " + arg);
                    continue;
                }

                String original = Files.readString(path, StandardCharsets.UTF_8);
                String formatted = formatter.format(original);

                if (!original.equals(formatted)) {
                    filesChanged++;
                    if (checkMode) {
                        System.out.println("Would reformat: " + arg);
                    }
                    else {
                        Files.writeString(path, formatted, StandardCharsets.UTF_8);
                        System.out.println("Formatted: " + arg);
                    }
                }

                filesProcessed++;
            }
            catch (Exception e) {
                System.err.println("Error processing " + arg + ": " + e.getMessage());
            }
        }

        System.out.println();
        System.out.println("Processed " + filesProcessed + " files, " + filesChanged + " changed");

        if (checkMode && filesChanged > 0) {
            System.exit(1);
        }
    }
}
