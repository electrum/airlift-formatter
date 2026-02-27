/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter;

import io.airlift.formatter.internal.jdt.DefaultCodeFormatterConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for the Airlift Java code formatter.
 *
 * <p>Encapsulates all formatter options and provides conversion to JDT formatter
 * option maps. Use the {@link #builder()} method to create custom configurations,
 * or use predefined presets like {@link #airlift()}.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Use Airlift preset
 * FormatterConfiguration config = FormatterConfiguration.airlift();
 *
 * // Custom configuration
 * FormatterConfiguration config = FormatterConfiguration.builder()
 *     .classBraceStyle(BraceStyle.NEXT_LINE)
 *     .elseOnNewLine(true)
 *     .lineWidth(180)
 *     .build();
 * }</pre>
 */
public final class FormatterConfiguration
{
    // Brace positions
    private final BraceStyle classBraceStyle;
    private final BraceStyle methodBraceStyle;
    private final BraceStyle constructorBraceStyle;
    private final BraceStyle enumBraceStyle;
    private final BraceStyle annotationBraceStyle;
    private final BraceStyle recordBraceStyle;
    private final BraceStyle anonymousClassBraceStyle;
    private final BraceStyle controlBraceStyle;
    private final BraceStyle switchBraceStyle;
    private final BraceStyle lambdaBraceStyle;

    // Control flow
    private final boolean elseOnNewLine;
    private final boolean catchOnNewLine;
    private final boolean finallyOnNewLine;
    private final boolean whileOnSameLine;

    // Indentation
    private final int tabSize;
    private final boolean useSpaces;
    private final int indentationSize;
    private final int continuationIndent;

    // Wrapping
    private final int lineWidth;
    private final boolean joinWrappedLines;
    private final WrapMode methodParametersWrap;
    private final WrapMode methodArgumentsWrap;
    private final WrapMode methodChainWrap;
    private final WrapMode ternaryWrap;
    private final WrapMode extendsListWrap;
    private final WrapMode throwsListWrap;
    private final WrapMode arrayInitializerWrap;

    // Blank lines
    private final int blankLinesInDeclarations;
    private final int blankLinesInCode;
    private final int blankLinesBeforeClosingBrace;

    // Keep on one line
    private final boolean keepSimpleMethodsOnOneLine;
    private final boolean keepSimpleLambdasOnOneLine;
    private final boolean keepSimpleClassesOnOneLine;

    // Imports
    private final ImportOrder importOrder;

    // Comments
    private final boolean formatBlockComments;
    private final boolean preserveWhitespaceBeforeLineComments;

    // Private constructor - use builder
    private FormatterConfiguration(Builder builder)
    {
        this.classBraceStyle = builder.classBraceStyle;
        this.methodBraceStyle = builder.methodBraceStyle;
        this.constructorBraceStyle = builder.constructorBraceStyle;
        this.enumBraceStyle = builder.enumBraceStyle;
        this.annotationBraceStyle = builder.annotationBraceStyle;
        this.recordBraceStyle = builder.recordBraceStyle;
        this.anonymousClassBraceStyle = builder.anonymousClassBraceStyle;
        this.controlBraceStyle = builder.controlBraceStyle;
        this.switchBraceStyle = builder.switchBraceStyle;
        this.lambdaBraceStyle = builder.lambdaBraceStyle;

        this.elseOnNewLine = builder.elseOnNewLine;
        this.catchOnNewLine = builder.catchOnNewLine;
        this.finallyOnNewLine = builder.finallyOnNewLine;
        this.whileOnSameLine = builder.whileOnSameLine;

        this.tabSize = builder.tabSize;
        this.useSpaces = builder.useSpaces;
        this.indentationSize = builder.indentationSize;
        this.continuationIndent = builder.continuationIndent;

        this.lineWidth = builder.lineWidth;
        this.joinWrappedLines = builder.joinWrappedLines;
        this.methodParametersWrap = builder.methodParametersWrap;
        this.methodArgumentsWrap = builder.methodArgumentsWrap;
        this.methodChainWrap = builder.methodChainWrap;
        this.ternaryWrap = builder.ternaryWrap;
        this.extendsListWrap = builder.extendsListWrap;
        this.throwsListWrap = builder.throwsListWrap;
        this.arrayInitializerWrap = builder.arrayInitializerWrap;

        this.blankLinesInDeclarations = builder.blankLinesInDeclarations;
        this.blankLinesInCode = builder.blankLinesInCode;
        this.blankLinesBeforeClosingBrace = builder.blankLinesBeforeClosingBrace;

        this.keepSimpleMethodsOnOneLine = builder.keepSimpleMethodsOnOneLine;
        this.keepSimpleLambdasOnOneLine = builder.keepSimpleLambdasOnOneLine;
        this.keepSimpleClassesOnOneLine = builder.keepSimpleClassesOnOneLine;

        this.importOrder = builder.importOrder;

        this.formatBlockComments = builder.formatBlockComments;
        this.preserveWhitespaceBeforeLineComments = builder.preserveWhitespaceBeforeLineComments;
    }

    /**
     * Creates the default Airlift formatter configuration.
     */
    public static FormatterConfiguration airlift()
    {
        return builder()
                // Brace positions - Airlift uses next_line for class/method
                .classBraceStyle(BraceStyle.NEXT_LINE)
                .methodBraceStyle(BraceStyle.NEXT_LINE)
                .constructorBraceStyle(BraceStyle.NEXT_LINE)
                .enumBraceStyle(BraceStyle.NEXT_LINE)
                .annotationBraceStyle(BraceStyle.NEXT_LINE)
                .recordBraceStyle(BraceStyle.NEXT_LINE)
                .anonymousClassBraceStyle(BraceStyle.NEXT_LINE)
                // Control flow - end of line
                .controlBraceStyle(BraceStyle.END_OF_LINE)
                .switchBraceStyle(BraceStyle.END_OF_LINE)
                .lambdaBraceStyle(BraceStyle.END_OF_LINE)
                // else/catch/finally on new line
                .elseOnNewLine(true)
                .catchOnNewLine(true)
                .finallyOnNewLine(true)
                .whileOnSameLine(true) // do-while on same line
                // Indentation
                .tabSize(4)
                .useSpaces(true)
                .indentationSize(4)
                .continuationIndent(2) // 2 units = 8 spaces
                // gofmt-style: don't force wrap
                .lineWidth(9999)
                .joinWrappedLines(false)
                .methodParametersWrap(WrapMode.WRAP_IF_LONG_COMPACT)
                .methodArgumentsWrap(WrapMode.WRAP_IF_LONG_COMPACT)
                .methodChainWrap(WrapMode.WRAP_IF_LONG_COMPACT)
                // Airlift import order
                .importOrder(ImportOrder.AIRLIFT)
                // Don't format block comments (gofmt-style)
                .formatBlockComments(false)
                .preserveWhitespaceBeforeLineComments(true)
                .build();
    }

    /**
     * Creates a Google Java Style formatter configuration.
     */
    public static FormatterConfiguration google()
    {
        return builder()
                // Google uses end_of_line for everything
                .classBraceStyle(BraceStyle.END_OF_LINE)
                .methodBraceStyle(BraceStyle.END_OF_LINE)
                .constructorBraceStyle(BraceStyle.END_OF_LINE)
                .enumBraceStyle(BraceStyle.END_OF_LINE)
                .annotationBraceStyle(BraceStyle.END_OF_LINE)
                .recordBraceStyle(BraceStyle.END_OF_LINE)
                .anonymousClassBraceStyle(BraceStyle.END_OF_LINE)
                .controlBraceStyle(BraceStyle.END_OF_LINE)
                .switchBraceStyle(BraceStyle.END_OF_LINE)
                .lambdaBraceStyle(BraceStyle.END_OF_LINE)
                // else/catch/finally on same line
                .elseOnNewLine(false)
                .catchOnNewLine(false)
                .finallyOnNewLine(false)
                .whileOnSameLine(true)
                // Indentation
                .tabSize(2)
                .useSpaces(true)
                .indentationSize(2)
                .continuationIndent(2) // +4 spaces
                // 100 char line width
                .lineWidth(100)
                .joinWrappedLines(true)
                .methodParametersWrap(WrapMode.WRAP_IF_LONG_COMPACT)
                .methodArgumentsWrap(WrapMode.WRAP_IF_LONG_COMPACT)
                .methodChainWrap(WrapMode.WRAP_IF_LONG_COMPACT)
                // Google import order
                .importOrder(ImportOrder.GOOGLE)
                .formatBlockComments(true)
                .preserveWhitespaceBeforeLineComments(false)
                .build();
    }

    /**
     * Creates a new builder for custom configuration.
     */
    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * Converts this configuration to a JDT formatter options map.
     *
     * @return map of JDT formatter option keys to values
     */
    public Map<String, String> toJdtOptions()
    {
        Map<String, String> options = new HashMap<>(DefaultCodeFormatterConstants.getEclipseDefaultSettings());

        // Brace positions
        options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION, classBraceStyle.getJdtValue());
        options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_METHOD_DECLARATION, methodBraceStyle.getJdtValue());
        options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_CONSTRUCTOR_DECLARATION, constructorBraceStyle.getJdtValue());
        options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ENUM_DECLARATION, enumBraceStyle.getJdtValue());
        options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ANNOTATION_TYPE_DECLARATION, annotationBraceStyle.getJdtValue());
        options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_RECORD_DECLARATION, recordBraceStyle.getJdtValue());
        options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_RECORD_CONSTRUCTOR, recordBraceStyle.getJdtValue());
        options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_ANONYMOUS_TYPE_DECLARATION, anonymousClassBraceStyle.getJdtValue());
        options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_BLOCK, controlBraceStyle.getJdtValue());
        options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_SWITCH, switchBraceStyle.getJdtValue());
        options.put(DefaultCodeFormatterConstants.FORMATTER_BRACE_POSITION_FOR_LAMBDA_BODY, lambdaBraceStyle.getJdtValue());

        // Control flow keywords
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_ELSE_IN_IF_STATEMENT, elseOnNewLine ? "insert" : "do not insert");
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_CATCH_IN_TRY_STATEMENT, catchOnNewLine ? "insert" : "do not insert");
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_FINALLY_IN_TRY_STATEMENT, finallyOnNewLine ? "insert" : "do not insert");
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_BEFORE_WHILE_IN_DO_STATEMENT, whileOnSameLine ? "do not insert" : "insert");

        // Indentation
        options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, useSpaces ? "space" : "tab");
        options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, String.valueOf(tabSize));
        options.put(DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE, String.valueOf(indentationSize));
        options.put(DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION, String.valueOf(continuationIndent));

        // Wrapping
        options.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, String.valueOf(lineWidth));
        options.put(DefaultCodeFormatterConstants.FORMATTER_JOIN_WRAPPED_LINES, joinWrappedLines ? "true" : "false");

        String wrapCompact = methodParametersWrap.toJdtAlignmentValue(false);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_METHOD_DECLARATION, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_CONSTRUCTOR_DECLARATION, wrapCompact);
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_METHOD_INVOCATION, methodArgumentsWrap.toJdtAlignmentValue(false));
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_ALLOCATION_EXPRESSION, methodArgumentsWrap.toJdtAlignmentValue(false));
        options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SELECTOR_IN_METHOD_INVOCATION, methodChainWrap.toJdtAlignmentValue(false));

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

        // Comments
        options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_BLOCK_COMMENT, formatBlockComments ? "true" : "false");
        options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_MARKDOWN_COMMENT, "false");
        options.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_PRESERVE_WHITE_SPACE_BETWEEN_CODE_AND_LINE_COMMENT, preserveWhitespaceBeforeLineComments ? "true" : "false");

        // gofmt-style options (always set for Airlift compatibility)
        options.put(DefaultCodeFormatterConstants.FORMATTER_WRAP_OUTER_EXPRESSIONS_WHEN_NESTED, "false");
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_METHOD_BODY_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_TYPE_DECLARATION_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_LAMBDA_BODY_BLOCK_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_ANONYMOUS_TYPE_DECLARATION_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_RECORD_DECLARATION_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_RECORD_CONSTRUCTOR_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_ENUM_DECLARATION_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_ANNOTATION_DECLARATION_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);

        // Blank lines (gofmt-style: preserve original)
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_PACKAGE, "0");
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_PACKAGE, "1");
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_IMPORTS, "1");
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_IMPORTS, "1");
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BETWEEN_IMPORT_GROUPS, "1");
        // AIRLIFT MODIFICATION: Match IntelliJ's KEEP_BLANK_LINES_IN_CODE=1 setting
        options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "1");
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_NEW_CHUNK, "0");
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIELD, "0");
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_METHOD, "0");
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_ABSTRACT_METHOD, "0");
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_MEMBER_TYPE, "0");
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIRST_CLASS_BODY_DECLARATION, "0");
        options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_AFTER_LAST_CLASS_BODY_DECLARATION, "0");

        // Annotations - preserve developer's choice
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_FIELD, "do not insert");
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_METHOD, "do not insert");
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_PARAMETER, "do not insert");
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_AFTER_ANNOTATION_ON_LOCAL_VARIABLE, "do not insert");

        // Comments at column 0 - preserve
        options.put(DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_BLOCK_COMMENTS_ON_FIRST_COLUMN, "true");
        options.put(DefaultCodeFormatterConstants.FORMATTER_NEVER_INDENT_LINE_COMMENTS_ON_FIRST_COLUMN, "true");

        // Empty lines - no indentation
        options.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_EMPTY_LINES, "false");

        // Switch statement indentation
        options.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_SWITCH, "true");
        options.put(DefaultCodeFormatterConstants.FORMATTER_INDENT_SWITCHSTATEMENTS_COMPARE_TO_CASES, "true");

        // Text blocks - preserve original indentation
        options.put(DefaultCodeFormatterConstants.FORMATTER_TEXT_BLOCK_INDENTATION, Integer.toString(DefaultCodeFormatterConstants.INDENT_PRESERVE));

        // Array initializers - no spaces inside braces
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER, "do not insert");
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER, "do not insert");
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_BRACES_IN_ARRAY_INITIALIZER, "do not insert");

        // For loop
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_SEMICOLON_IN_FOR, "do not insert");

        // Keep simple constructs on one line
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_SIMPLE_IF_ON_ONE_LINE, "false");
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_THEN_STATEMENT_ON_SAME_LINE, "false");
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_ELSE_STATEMENT_ON_SAME_LINE, "false");

        // Don't insert newline in empty anonymous type
        options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_NEW_LINE_IN_EMPTY_ANONYMOUS_TYPE_DECLARATION, "do not insert");

        // Switch cases
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_SWITCH_CASE_WITH_ARROW_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_SWITCH_BODY_BLOCK_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_CODE_BLOCK_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_NEVER);
        options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_ENUM_CONSTANT_DECLARATION_ON_ONE_LINE, DefaultCodeFormatterConstants.ONE_LINE_PRESERVE);

        return options;
    }

    // Getters
    public BraceStyle getClassBraceStyle()
    {
        return classBraceStyle;
    }

    public BraceStyle getMethodBraceStyle()
    {
        return methodBraceStyle;
    }

    public BraceStyle getControlBraceStyle()
    {
        return controlBraceStyle;
    }

    public BraceStyle getLambdaBraceStyle()
    {
        return lambdaBraceStyle;
    }

    public boolean isElseOnNewLine()
    {
        return elseOnNewLine;
    }

    public boolean isCatchOnNewLine()
    {
        return catchOnNewLine;
    }

    public boolean isFinallyOnNewLine()
    {
        return finallyOnNewLine;
    }

    public int getTabSize()
    {
        return tabSize;
    }

    public boolean isUseSpaces()
    {
        return useSpaces;
    }

    public int getLineWidth()
    {
        return lineWidth;
    }

    public boolean isJoinWrappedLines()
    {
        return joinWrappedLines;
    }

    public ImportOrder getImportOrder()
    {
        return importOrder;
    }

    /**
     * Builder for FormatterConfiguration.
     */
    public static final class Builder
    {
        private BraceStyle classBraceStyle = BraceStyle.END_OF_LINE;
        private BraceStyle methodBraceStyle = BraceStyle.END_OF_LINE;
        private BraceStyle constructorBraceStyle = BraceStyle.END_OF_LINE;
        private BraceStyle enumBraceStyle = BraceStyle.END_OF_LINE;
        private BraceStyle annotationBraceStyle = BraceStyle.END_OF_LINE;
        private BraceStyle recordBraceStyle = BraceStyle.END_OF_LINE;
        private BraceStyle anonymousClassBraceStyle = BraceStyle.END_OF_LINE;
        private BraceStyle controlBraceStyle = BraceStyle.END_OF_LINE;
        private BraceStyle switchBraceStyle = BraceStyle.END_OF_LINE;
        private BraceStyle lambdaBraceStyle = BraceStyle.END_OF_LINE;

        private boolean elseOnNewLine = false;
        private boolean catchOnNewLine = false;
        private boolean finallyOnNewLine = false;
        private boolean whileOnSameLine = true;

        private int tabSize = 4;
        private boolean useSpaces = true;
        private int indentationSize = 4;
        private int continuationIndent = 2;

        private int lineWidth = 120;
        private boolean joinWrappedLines = true;
        private WrapMode methodParametersWrap = WrapMode.WRAP_IF_LONG_COMPACT;
        private WrapMode methodArgumentsWrap = WrapMode.WRAP_IF_LONG_COMPACT;
        private WrapMode methodChainWrap = WrapMode.WRAP_IF_LONG_COMPACT;
        private WrapMode ternaryWrap = WrapMode.WRAP_IF_LONG_COMPACT;
        private WrapMode extendsListWrap = WrapMode.WRAP_IF_LONG_COMPACT;
        private WrapMode throwsListWrap = WrapMode.WRAP_IF_LONG_COMPACT;
        private WrapMode arrayInitializerWrap = WrapMode.WRAP_IF_LONG_COMPACT;

        private int blankLinesInDeclarations = 1;
        private int blankLinesInCode = 1;
        private int blankLinesBeforeClosingBrace = 0;

        private boolean keepSimpleMethodsOnOneLine = true;
        private boolean keepSimpleLambdasOnOneLine = true;
        private boolean keepSimpleClassesOnOneLine = true;

        private ImportOrder importOrder = ImportOrder.AIRLIFT;

        private boolean formatBlockComments = true;
        private boolean preserveWhitespaceBeforeLineComments = false;

        private Builder() {}

        public Builder classBraceStyle(BraceStyle style)
        {
            this.classBraceStyle = Objects.requireNonNull(style);
            return this;
        }

        public Builder methodBraceStyle(BraceStyle style)
        {
            this.methodBraceStyle = Objects.requireNonNull(style);
            return this;
        }

        public Builder constructorBraceStyle(BraceStyle style)
        {
            this.constructorBraceStyle = Objects.requireNonNull(style);
            return this;
        }

        public Builder enumBraceStyle(BraceStyle style)
        {
            this.enumBraceStyle = Objects.requireNonNull(style);
            return this;
        }

        public Builder annotationBraceStyle(BraceStyle style)
        {
            this.annotationBraceStyle = Objects.requireNonNull(style);
            return this;
        }

        public Builder recordBraceStyle(BraceStyle style)
        {
            this.recordBraceStyle = Objects.requireNonNull(style);
            return this;
        }

        public Builder anonymousClassBraceStyle(BraceStyle style)
        {
            this.anonymousClassBraceStyle = Objects.requireNonNull(style);
            return this;
        }

        public Builder controlBraceStyle(BraceStyle style)
        {
            this.controlBraceStyle = Objects.requireNonNull(style);
            return this;
        }

        public Builder switchBraceStyle(BraceStyle style)
        {
            this.switchBraceStyle = Objects.requireNonNull(style);
            return this;
        }

        public Builder lambdaBraceStyle(BraceStyle style)
        {
            this.lambdaBraceStyle = Objects.requireNonNull(style);
            return this;
        }

        public Builder elseOnNewLine(boolean value)
        {
            this.elseOnNewLine = value;
            return this;
        }

        public Builder catchOnNewLine(boolean value)
        {
            this.catchOnNewLine = value;
            return this;
        }

        public Builder finallyOnNewLine(boolean value)
        {
            this.finallyOnNewLine = value;
            return this;
        }

        public Builder whileOnSameLine(boolean value)
        {
            this.whileOnSameLine = value;
            return this;
        }

        public Builder tabSize(int value)
        {
            this.tabSize = value;
            return this;
        }

        public Builder useSpaces(boolean value)
        {
            this.useSpaces = value;
            return this;
        }

        public Builder indentationSize(int value)
        {
            this.indentationSize = value;
            return this;
        }

        public Builder continuationIndent(int value)
        {
            this.continuationIndent = value;
            return this;
        }

        public Builder lineWidth(int value)
        {
            this.lineWidth = value;
            return this;
        }

        public Builder joinWrappedLines(boolean value)
        {
            this.joinWrappedLines = value;
            return this;
        }

        public Builder methodParametersWrap(WrapMode mode)
        {
            this.methodParametersWrap = Objects.requireNonNull(mode);
            return this;
        }

        public Builder methodArgumentsWrap(WrapMode mode)
        {
            this.methodArgumentsWrap = Objects.requireNonNull(mode);
            return this;
        }

        public Builder methodChainWrap(WrapMode mode)
        {
            this.methodChainWrap = Objects.requireNonNull(mode);
            return this;
        }

        public Builder ternaryWrap(WrapMode mode)
        {
            this.ternaryWrap = Objects.requireNonNull(mode);
            return this;
        }

        public Builder extendsListWrap(WrapMode mode)
        {
            this.extendsListWrap = Objects.requireNonNull(mode);
            return this;
        }

        public Builder throwsListWrap(WrapMode mode)
        {
            this.throwsListWrap = Objects.requireNonNull(mode);
            return this;
        }

        public Builder arrayInitializerWrap(WrapMode mode)
        {
            this.arrayInitializerWrap = Objects.requireNonNull(mode);
            return this;
        }

        public Builder blankLinesInDeclarations(int value)
        {
            this.blankLinesInDeclarations = value;
            return this;
        }

        public Builder blankLinesInCode(int value)
        {
            this.blankLinesInCode = value;
            return this;
        }

        public Builder blankLinesBeforeClosingBrace(int value)
        {
            this.blankLinesBeforeClosingBrace = value;
            return this;
        }

        public Builder keepSimpleMethodsOnOneLine(boolean value)
        {
            this.keepSimpleMethodsOnOneLine = value;
            return this;
        }

        public Builder keepSimpleLambdasOnOneLine(boolean value)
        {
            this.keepSimpleLambdasOnOneLine = value;
            return this;
        }

        public Builder keepSimpleClassesOnOneLine(boolean value)
        {
            this.keepSimpleClassesOnOneLine = value;
            return this;
        }

        public Builder importOrder(ImportOrder order)
        {
            this.importOrder = Objects.requireNonNull(order);
            return this;
        }

        public Builder formatBlockComments(boolean value)
        {
            this.formatBlockComments = value;
            return this;
        }

        public Builder preserveWhitespaceBeforeLineComments(boolean value)
        {
            this.preserveWhitespaceBeforeLineComments = value;
            return this;
        }

        public FormatterConfiguration build()
        {
            return new FormatterConfiguration(this);
        }
    }
}
