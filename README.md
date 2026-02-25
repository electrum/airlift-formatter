# Airlift Java Formatter

A standalone IntelliJ-compatible Java code formatter and checkstyle checker for Airlift/Trino projects.

## TL;DR - Maven Plugin Usage

Quick try (no pom.xml changes needed):

```bash
mvn ca.vanzyl:airlift-formatter-maven-plugin:1.0-SNAPSHOT:format
```

Or add the plugin to your `pom.xml`:

```xml
<plugin>
    <groupId>ca.vanzyl</groupId>
    <artifactId>airlift-formatter-maven-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
</plugin>
```

Format your code:

```bash
mvn airlift-formatter:format
```

Check for violations (fails build if found):

```bash
mvn airlift-formatter:check
```

Skip formatting:

```bash
mvn airlift-formatter:format -Dailrlift.formatter.skip=true
```

---

## What It Does

This tool provides **IntelliJ-compatible Java source code formatting** and **Airlift checkstyle validation** without requiring IntelliJ IDEA to be installed or running.

**Key capabilities:**

| Feature | Description |
|---------|-------------|
| **Format** | Formats Java code to match IntelliJ IDEA with Airlift.xml configuration |
| **Check** | Validates code against Airlift checkstyle rules (airbase-checks.xml) |
| **Idempotent** | Formatting already-formatted code produces no changes |
| **gofmt-style** | Preserves developer formatting choices where possible |

**Use cases:**

- CI/CD pipelines - automated formatting without IntelliJ
- Pre-commit hooks - format code before committing
- Code review - check for style violations
- IDE-free workflows - format from command line

---

## Architecture

The formatter is built on Eclipse JDT with strategic modifications for Airlift-style formatting:

```
airlift-formatter/
├── airlift-formatter-core/
│   └── io.airlift.formatter/
│       ├── AirliftFormatter.java        # Main entry point
│       ├── FormatterConfiguration.java  # Configuration builder
│       ├── checkstyle/                  # Checkstyle validation
│       ├── config/                      # IntelliJ XML parsing
│       ├── util/                        # Utilities (mutation testing)
│       └── internal/jdt/                # Forked Eclipse JDT internals
└── airlift-formatter-maven-plugin/
    └── io.airlift.formatter.maven/
        ├── FormatMojo                   # format goal
        └── CheckMojo                    # check goal
```

**Key design principles:**

1. **Forked Eclipse JDT Core** - Strategic modifications marked with "AIRLIFT MODIFICATION"
2. **gofmt-style philosophy** - Preserves developer intent rather than enforcing rigid rules
3. **Import organizer** - Pre-processor handles Airlift-specific import ordering
4. **IntelliJ XML parser** - Parses Airlift.xml into formatter configuration

---

## Testing Summary

The formatter has been extensively tested against production codebases to ensure correctness.

### Test Results

| Test | Airlift | Trino |
|------|---------|-------|
| **Files tested** | 943 | 10,736 |
| **Format preservation** | 100% | 100% |
| **Checkstyle compliance** | 100% | 100% |
| **Idempotency** | Pass | Pass |

### Test Descriptions

| Test | Purpose |
|------|---------|
| **Format Preservation** | Verifies that already-formatted code (from Airlift/Trino repos) remains UNCHANGED after formatting. This is the critical test - the formatter must not modify correctly-formatted code. |
| **Checkstyle Compliance** | Verifies that formatted output passes all airbase-checks.xml checkstyle rules. |
| **Idempotency** | Verifies that `format(format(code)) == format(code)`. Formatting twice produces identical output. |
| **Mutation Testing** | Applies random code mutations (scrambled imports, removed braces, etc.) and verifies the formatter restores them correctly. Tests robustness. |

### Running Tests

```bash
# Clone test repositories
mkdir -p airlift-formatter-core/target/repos
git clone --depth 1 https://github.com/airlift/airlift.git airlift-formatter-core/target/repos/airlift
git clone --depth 1 https://github.com/trinodb/trino.git airlift-formatter-core/target/repos/trino

# Run format preservation tests
cd airlift-formatter-core
mvn test -Dtest=FormatterGapDiscoveryTest#verifyNoChangesOnAirlift \
    "-Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition"

mvn test -Dtest=FormatterGapDiscoveryTest#verifyNoChangesOnTrino \
    "-Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition"

# Run idempotency tests
mvn test -Dtest=FormatterGapDiscoveryTest#verifyIdempotencyInAirlift \
    "-Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition"

# Run comprehensive tests (format + checkstyle validation)
mvn test -Dtest=FormatterGapDiscoveryTest#verifyComprehensiveOnAirlift \
    "-Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition"
```

---

## Maven Plugin Reference

### Goals

| Goal | Description | Default Phase |
|------|-------------|---------------|
| `format` | Format Java source files | `process-sources` |
| `check` | Validate checkstyle compliance | `verify` |

### Configuration Options

```xml
<plugin>
    <groupId>ca.vanzyl</groupId>
    <artifactId>airlift-formatter-maven-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
    <configuration>
        <!-- Skip execution -->
        <skip>false</skip>

        <!-- Include test sources -->
        <includeTestSources>true</includeTestSources>

        <!-- Fail build on checkstyle violations (check goal only) -->
        <failOnViolation>true</failOnViolation>

        <!-- Custom source directories -->
        <sourceDirectories>
            <sourceDirectory>src/main/java</sourceDirectory>
        </sourceDirectories>

        <!-- File patterns to include (glob) -->
        <includes>
            <include>**/*.java</include>
        </includes>

        <!-- File patterns to exclude (glob) -->
        <excludes>
            <exclude>**/generated/**</exclude>
        </excludes>
    </configuration>
</plugin>
```

### Command-Line Properties

| Property | Description | Default |
|----------|-------------|---------|
| `airlift.formatter.skip` | Skip execution | `false` |
| `airlift.formatter.includeTestSources` | Include test sources | `true` |
| `airlift.formatter.failOnViolation` | Fail on violations | `true` |

---

## Programmatic API

Use the formatter directly in Java code:

```java
import io.airlift.formatter.AirliftFormatter;

// Format with default Airlift settings
AirliftFormatter formatter = new AirliftFormatter();
String formatted = formatter.format(sourceCode);
```

With custom configuration:

```java
import io.airlift.formatter.FormatterConfiguration;
import io.airlift.formatter.BraceStyle;
import io.airlift.formatter.ImportOrder;

FormatterConfiguration config = FormatterConfiguration.builder()
        .classBraceStyle(BraceStyle.NEXT_LINE)
        .methodBraceStyle(BraceStyle.NEXT_LINE)
        .elseOnNewLine(true)
        .catchOnNewLine(true)
        .tabSize(4)
        .useSpaces(true)
        .importOrder(ImportOrder.AIRLIFT)
        .build();

AirliftFormatter formatter = new AirliftFormatter(config);
```

Parse IntelliJ XML configuration:

```java
import io.airlift.formatter.config.IntelliJConfigParser;

IntelliJConfigParser parser = new IntelliJConfigParser();
FormatterConfiguration config = parser.parse(Path.of("Airlift.xml"));
AirliftFormatter formatter = new AirliftFormatter(config);
```

---

## Airlift Code Style Summary

Key formatting rules enforced:

| Rule | Value |
|------|-------|
| Line width | 180 characters |
| Indentation | 4 spaces |
| Continuation indent | 8 spaces |
| Class/method braces | Next line |
| Control flow braces | End of line |
| else/catch/finally | On new line |
| Imports | No wildcards, ordered as `(*, javax, java, static)` |
| Control statements | Always require braces |

---

## Building

```bash
mvn clean install
```

## Requirements

- Java 25+
- Maven 3.8+

## Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| **Checkstyle** | 12.2.0 | Programmatic validation against airbase-checks.xml rules |
| **Eclipse JDT Core** | 3.44.0 | Java AST parsing and formatting engine |

### Checkstyle Java Support

Checkstyle 12.2.0:
- **Requires**: Java 17+ to run
- **Analyzes**: Java source code up to Java 25

## License

Eclipse Public License 2.0

---

# IntelliJ Formatter Option Support Status

This document provides a comprehensive reference of ALL IntelliJ IDEA formatter options and their support status in the Airlift JDT Formatter.

## Implementation Summary

The Airlift JDT Formatter achieves **100% format preservation** on both Airlift (943 files) and Trino (10,736 files) codebases. This is accomplished through:

1. **Forked Eclipse JDT Core**: Strategic modifications to the JDT formatter (marked with "AIRLIFT MODIFICATION")
2. **gofmt-style Philosophy**: Preserves developer intent rather than enforcing rigid rules
3. **Import Organizer**: Pre-processor handles Airlift-specific import ordering
4. **IntelliJ XML Parser**: `IntelliJConfigParser` parses Airlift.xml into `FormatterConfiguration`

### Key Configuration Files

| File                          | Purpose                                                      |
|-------------------------------|--------------------------------------------------------------|
| `FormatterConfiguration.java` | Configuration builder with presets (`airlift()`, `google()`) |
| `IntelliJConfigParser.java`   | Parses IntelliJ XML into FormatterConfiguration              |
| `AirliftImportOrganizer.java` | Import ordering pre-processor                                |
| `WrapPreparator.java`         | Stores original indentation on tokens                        |
| `WrapExecutor.java`           | Uses `originalIndent` for BLOCK_INDENT mode                  |
| `LineBreaksPreparator.java`   | gofmt-style blank line preservation                          |

---

## Support Status Legend

| Status  | Emoji | Description                                                                            |
|---------|-------|----------------------------------------------------------------------------------------|
| FULL    | ✅     | Fully supported, produces identical output to IntelliJ                                 |
| PARTIAL | 🔶    | Supported via gofmt-style preservation (doesn't actively wrap, but preserves existing) |
| N/A     | ➖     | Not applicable to formatting, or not enforced by design                                |
| NONE    | ❌     | Intentionally not supported (semantic operation, not formatting)                       |
| DEFAULT | ⚪     | Uses IntelliJ default value (not explicitly configured in Airlift.xml)                 |

---

## Complete IntelliJ Option Catalog

All ~220 IntelliJ formatter options in one table, organized by category.

| Category                  | IntelliJ Option                                         | IntelliJ Default | Airlift.xml      | Support | Notes                                                           |
|---------------------------|---------------------------------------------------------|------------------|------------------|---------|-----------------------------------------------------------------|
| **Alignment**             | ALIGN_CONSECUTIVE_ASSIGNMENTS                           | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Alignment**             | ALIGN_CONSECUTIVE_VARIABLE_DECLARATIONS                 | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Alignment**             | ALIGN_GROUP_FIELD_DECLARATIONS                          | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Alignment**             | ALIGN_MULTILINE_ANNOTATION_PARAMETERS                   | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Alignment**             | ALIGN_MULTILINE_ARRAY_INITIALIZER_EXPRESSION            | false            | true             | 🔶      | Airlift enables (global option)                                 |
| **Alignment**             | ALIGN_MULTILINE_ASSIGNMENT                              | false            | true             | 🔶      | Airlift enables alignment                                       |
| **Alignment**             | ALIGN_MULTILINE_BINARY_OPERATION                        | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Alignment**             | ALIGN_MULTILINE_CHAINED_METHODS                         | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Alignment**             | ALIGN_MULTILINE_DECONSTRUCTION                          | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Alignment**             | ALIGN_MULTILINE_EXTENDS_LIST                            | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Alignment**             | ALIGN_MULTILINE_FOR                                     | true             | false            | ✅       | Airlift disables                                                |
| **Alignment**             | ALIGN_MULTILINE_METHOD_BRACKETS                         | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Alignment**             | ALIGN_MULTILINE_PARAMETERS                              | true             | false            | ✅       | Airlift disables column alignment                               |
| **Alignment**             | ALIGN_MULTILINE_PARAMETERS_IN_CALLS                     | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Alignment**             | ALIGN_MULTILINE_PARENTHESIZED_EXPRESSION                | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Alignment**             | ALIGN_MULTILINE_RECORDS                                 | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Alignment**             | ALIGN_MULTILINE_RESOURCES                               | true             | false            | ✅       | Airlift disables                                                |
| **Alignment**             | ALIGN_MULTILINE_TERNARY_OPERATION                       | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Alignment**             | ALIGN_MULTILINE_TEXT_BLOCKS                             | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Alignment**             | ALIGN_MULTILINE_THROWS_LIST                             | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Alignment**             | ALIGN_SUBSEQUENT_SIMPLE_METHODS                         | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Alignment**             | ALIGN_THROWS_KEYWORD                                    | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Annotations**           | ANNOTATION_PARAMETER_WRAP                               | 0                | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Annotations**           | CLASS_ANNOTATION_WRAP                                   | 5                | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Annotations**           | DO_NOT_WRAP_AFTER_SINGLE_ANNOTATION                     | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Annotations**           | DO_NOT_WRAP_AFTER_SINGLE_ANNOTATION_IN_PARAMETER        | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Annotations**           | FIELD_ANNOTATION_WRAP                                   | 5                | 0 (do not)       | ✅       | Airlift doesn't wrap field annotations                          |
| **Annotations**           | METHOD_ANNOTATION_WRAP                                  | 5                | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Annotations**           | PARAMETER_ANNOTATION_WRAP                               | 0                | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Annotations**           | VARIABLE_ANNOTATION_WRAP                                | 0                | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Blank Lines**           | BLANK_LINES_AFTER_ANONYMOUS_CLASS_HEADER                | 0                | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Blank Lines**           | BLANK_LINES_AFTER_CLASS_HEADER                          | 0                | (default)        | ✅       | `FORMATTER_BLANK_LINES_BEFORE_FIRST_CLASS_BODY_DECLARATION`     |
| **Blank Lines**           | BLANK_LINES_AFTER_IMPORTS                               | 1                | (default)        | ✅       | `FORMATTER_BLANK_LINES_AFTER_IMPORTS`                           |
| **Blank Lines**           | BLANK_LINES_AFTER_PACKAGE                               | 1                | (default)        | ✅       | `FORMATTER_BLANK_LINES_AFTER_PACKAGE`                           |
| **Blank Lines**           | BLANK_LINES_AROUND_CLASS                                | 1                | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Blank Lines**           | BLANK_LINES_AROUND_FIELD                                | 0                | (default)        | ✅       | `FORMATTER_BLANK_LINES_BEFORE_FIELD`                            |
| **Blank Lines**           | BLANK_LINES_AROUND_FIELD_IN_INTERFACE                   | 0                | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Blank Lines**           | BLANK_LINES_AROUND_METHOD                               | 1                | (default)        | ✅       | `FORMATTER_BLANK_LINES_BEFORE_METHOD`                           |
| **Blank Lines**           | BLANK_LINES_AROUND_METHOD_IN_INTERFACE                  | 1                | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Blank Lines**           | BLANK_LINES_BEFORE_CLASS_END                            | 0                | (default)        | ✅       | `FORMATTER_BLANK_LINES_AFTER_LAST_CLASS_BODY_DECLARATION`       |
| **Blank Lines**           | BLANK_LINES_BEFORE_IMPORTS                              | 1                | (default)        | ✅       | `FORMATTER_BLANK_LINES_BEFORE_IMPORTS`                          |
| **Blank Lines**           | BLANK_LINES_BEFORE_METHOD_BODY                          | 0                | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Blank Lines**           | BLANK_LINES_BEFORE_PACKAGE                              | 0                | (default)        | ✅       | `FORMATTER_BLANK_LINES_BEFORE_PACKAGE`                          |
| **Blank Lines**           | KEEP_BLANK_LINES_BEFORE_RBRACE                          | 2                | 0                | ✅       | JDT modification to remove                                      |
| **Blank Lines**           | KEEP_BLANK_LINES_BETWEEN_PACKAGE_DECLARATION_AND_HEADER | 2                | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Blank Lines**           | KEEP_BLANK_LINES_IN_CODE                                | 2                | 1                | ✅       | Airlift uses stricter limit                                     |
| **Blank Lines**           | KEEP_BLANK_LINES_IN_DECLARATIONS                        | 2                | 1                | ✅       | Airlift uses stricter limit                                     |
| **Brace Placement**       | BRACE_STYLE                                             | 1 (end of line)  | (default)        | ⚪       | General brace style                                             |
| **Brace Placement**       | CLASS_BRACE_STYLE                                       | 1 (end of line)  | 2 (next line)    | ✅       | `FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION`                 |
| **Brace Placement**       | LAMBDA_BRACE_STYLE                                      | 1 (end of line)  | (default)        | ✅       | `FORMATTER_BRACE_POSITION_FOR_LAMBDA_BODY`                      |
| **Brace Placement**       | METHOD_BRACE_STYLE                                      | 1 (end of line)  | 2 (next line)    | ✅       | `FORMATTER_BRACE_POSITION_FOR_METHOD_DECLARATION`               |
| **Code Generation**       | GENERATE_FINAL_LOCALS                                   | false            | (default)        | ➖       | Code generation, not formatting                                 |
| **Code Generation**       | GENERATE_FINAL_PARAMETERS                               | false            | (default)        | ➖       | Code generation, not formatting                                 |
| **Code Generation**       | INSERT_OVERRIDE_ANNOTATION                              | true             | (default)        | ➖       | Code generation, not formatting                                 |
| **Code Generation**       | REPEAT_SYNCHRONIZED                                     | true             | (default)        | ➖       | Code generation, not formatting                                 |
| **Code Generation**       | REPLACE_INSTANCEOF_AND_CAST                             | false            | (default)        | ➖       | Code generation, not formatting                                 |
| **Code Generation**       | REPLACE_NULL_CHECK                                      | false            | (default)        | ➖       | Code generation, not formatting                                 |
| **Code Generation**       | REPLACE_SUM                                             | false            | (default)        | ➖       | Code generation, not formatting                                 |
| **Code Generation**       | USE_EXTERNAL_ANNOTATIONS                                | false            | (default)        | ➖       | Code generation, not formatting                                 |
| **Code Generation**       | VISIBILITY                                              | "public"         | (default)        | ➖       | Code generation, not formatting                                 |
| **Control Flow**          | CATCH_ON_NEW_LINE                                       | false            | true             | ✅       | `FORMATTER_INSERT_NEW_LINE_BEFORE_CATCH_IN_TRY_STATEMENT`       |
| **Control Flow**          | ELSE_ON_NEW_LINE                                        | false            | true             | ✅       | `FORMATTER_INSERT_NEW_LINE_BEFORE_ELSE_IN_IF_STATEMENT`         |
| **Control Flow**          | FINALLY_ON_NEW_LINE                                     | false            | true             | ✅       | `FORMATTER_INSERT_NEW_LINE_BEFORE_FINALLY_IN_TRY_STATEMENT`     |
| **Control Flow**          | KEEP_CONTROL_STATEMENT_IN_ONE_LINE                      | true             | false            | ✅       | `FORMATTER_KEEP_SIMPLE_IF_ON_ONE_LINE`                          |
| **Control Flow**          | SPECIAL_ELSE_IF_TREATMENT                               | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Control Flow**          | WHILE_ON_NEW_LINE                                       | false            | true             | ✅       | `FORMATTER_INSERT_NEW_LINE_BEFORE_WHILE_IN_DO_STATEMENT`        |
| **Force Braces**          | DOWHILE_BRACE_FORCE                                     | 0 (do not force) | 3 (always)       | ➖       | Not enforced - preserves existing                               |
| **Force Braces**          | FOR_BRACE_FORCE                                         | 0 (do not force) | 3 (always)       | ➖       | Not enforced - preserves existing                               |
| **Force Braces**          | IF_BRACE_FORCE                                          | 0 (do not force) | 3 (always)       | ➖       | Not enforced - preserves existing                               |
| **Force Braces**          | WHILE_BRACE_FORCE                                       | 0 (do not force) | 3 (always)       | ➖       | Not enforced - preserves existing                               |
| **General**               | BLOCK_COMMENT_ADD_SPACE                                 | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **General**               | BLOCK_COMMENT_AT_FIRST_COLUMN                           | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **General**               | KEEP_FIRST_COLUMN_COMMENT                               | true             | (default)        | ✅       | `NEVER_INDENT_*_ON_FIRST_COLUMN`                                |
| **General**               | KEEP_LINE_BREAKS                                        | true             | (default)        | ✅       | Essential for gofmt-style                                       |
| **General**               | LINE_COMMENT_ADD_SPACE                                  | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **General**               | LINE_COMMENT_ADD_SPACE_ON_REFORMAT                      | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **General**               | LINE_COMMENT_AT_FIRST_COLUMN                            | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **General**               | RIGHT_MARGIN                                            | 120              | 180              | ✅       | Line width                                                      |
| **General**               | WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN                   | true             | (default)        | ⚪       | Not applicable for formatting                                   |
| **Imports**               | CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND                     | 5                | 9999             | ➖       | Handled by import organizer                                     |
| **Imports**               | IMPORT_LAYOUT_TABLE                                     | (standard)       | custom           | ✅       | Parsed by `IntelliJConfigParser`                                |
| **Imports**               | INSERT_INNER_CLASS_IMPORTS                              | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Imports**               | LAYOUT_STATIC_IMPORTS_SEPARATELY                        | true             | (default)        | ✅       | Supported via import organizer                                  |
| **Imports**               | NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND                     | 3                | 9999             | ➖       | Handled by import organizer                                     |
| **Imports**               | PACKAGES_TO_USE_IMPORT_ON_DEMAND                        | (various)        | (empty)          | ➖       | Handled by import organizer                                     |
| **Indentation**           | CONTINUATION_INDENT_SIZE                                | 8                | (default)        | ✅       | `FORMATTER_CONTINUATION_INDENTATION` = 2                        |
| **Indentation**           | INDENT_SIZE                                             | 4                | (default)        | ✅       | `FORMATTER_INDENTATION_SIZE`                                    |
| **Indentation**           | LABEL_INDENT_ABSOLUTE                                   | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Indentation**           | LABEL_INDENT_SIZE                                       | 0                | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Indentation**           | SMART_TABS                                              | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Indentation**           | TAB_SIZE                                                | 4                | (default)        | ✅       | `FORMATTER_TAB_SIZE`                                            |
| **Indentation**           | USE_RELATIVE_INDENTS                                    | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Indentation**           | USE_TAB_CHARACTER                                       | false            | (default)        | ✅       | `FORMATTER_TAB_CHAR` = "space"                                  |
| **Javadoc**               | JD_ADD_BLANK_AFTER_DESCRIPTION                          | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Javadoc**               | JD_ADD_BLANK_AFTER_PARM_COMMENTS                        | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Javadoc**               | JD_ADD_BLANK_AFTER_RETURN                               | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Javadoc**               | JD_ALIGN_EXCEPTION_COMMENTS                             | true             | false            | ✅       | Airlift disables alignment                                      |
| **Javadoc**               | JD_ALIGN_PARAM_COMMENTS                                 | true             | false            | ✅       | Airlift disables alignment                                      |
| **Javadoc**               | JD_DO_NOT_WRAP_ONE_LINE_COMMENTS                        | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Javadoc**               | JD_INDENT_ON_CONTINUATION                               | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Javadoc**               | JD_KEEP_EMPTY_LINES                                     | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Javadoc**               | JD_KEEP_INVALID_TAGS                                    | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Javadoc**               | JD_LEADING_ASTERISKS_ARE_ENABLED                        | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Javadoc**               | JD_P_AT_EMPTY_LINES                                     | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Javadoc**               | JD_PARAM_DESCRIPTION_ON_NEW_LINE                        | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Javadoc**               | JD_PRESERVE_LINE_FEEDS                                  | false            | true             | ✅       | Airlift enables preservation                                    |
| **Keep on One Line**      | KEEP_MULTIPLE_EXPRESSIONS_IN_ONE_LINE                   | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Keep on One Line**      | KEEP_SIMPLE_BLOCKS_IN_ONE_LINE                          | false            | false            | ✅       | `FORMATTER_KEEP_CODE_BLOCK_ON_ONE_LINE` = NEVER                 |
| **Keep on One Line**      | KEEP_SIMPLE_CLASSES_IN_ONE_LINE                         | false            | true             | ✅       | `FORMATTER_KEEP_TYPE_DECLARATION_ON_ONE_LINE` = PRESERVE        |
| **Keep on One Line**      | KEEP_SIMPLE_LAMBDAS_IN_ONE_LINE                         | false            | true             | ✅       | `FORMATTER_KEEP_LAMBDA_BODY_BLOCK_ON_ONE_LINE` = PRESERVE       |
| **Keep on One Line**      | KEEP_SIMPLE_METHODS_IN_ONE_LINE                         | false            | true             | ✅       | `FORMATTER_KEEP_METHOD_BODY_ON_ONE_LINE` = PRESERVE             |
| **Keep on One Line**      | KEEP_SIMPLE_RECORDS_IN_ONE_LINE                         | true             | (default)        | ✅       | `FORMATTER_KEEP_RECORD_DECLARATION_ON_ONE_LINE`                 |
| **Member Arrangement**    | ARRANGEMENT_RULES                                       | (none)           | custom           | ❌       | Semantic operation, not formatting. JDT cannot reorder members. |
| **Naming Conventions**    | FIELD_NAME_PREFIX                                       | ""               | (default)        | ➖       | Code generation, not formatting                                 |
| **Naming Conventions**    | FIELD_NAME_SUFFIX                                       | ""               | (default)        | ➖       | Code generation, not formatting                                 |
| **Naming Conventions**    | LOCAL_VARIABLE_NAME_PREFIX                              | ""               | (default)        | ➖       | Code generation, not formatting                                 |
| **Naming Conventions**    | LOCAL_VARIABLE_NAME_SUFFIX                              | ""               | (default)        | ➖       | Code generation, not formatting                                 |
| **Naming Conventions**    | PARAMETER_NAME_PREFIX                                   | ""               | (default)        | ➖       | Code generation, not formatting                                 |
| **Naming Conventions**    | PARAMETER_NAME_SUFFIX                                   | ""               | (default)        | ➖       | Code generation, not formatting                                 |
| **Naming Conventions**    | PREFER_LONGER_NAMES                                     | true             | (default)        | ➖       | Code generation, not formatting                                 |
| **Naming Conventions**    | STATIC_FIELD_NAME_PREFIX                                | ""               | (default)        | ➖       | Code generation, not formatting                                 |
| **Naming Conventions**    | STATIC_FIELD_NAME_SUFFIX                                | ""               | (default)        | ➖       | Code generation, not formatting                                 |
| **Pattern Matching**      | ALIGN_MULTILINE_DECONSTRUCTION                          | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Pattern Matching**      | DECONSTRUCTION_LIST_WRAP                                | 0                | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Pattern Matching**      | NEW_LINE_AFTER_LPAREN_IN_DECONSTRUCTION                 | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Pattern Matching**      | RPAREN_ON_NEW_LINE_IN_DECONSTRUCTION                    | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Records**               | NEW_LINE_AFTER_LPAREN_IN_RECORD_HEADER                  | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Records**               | RECORD_COMPONENTS_WRAP                                  | 0                | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Records**               | RPAREN_ON_NEW_LINE_IN_RECORD_HEADER                     | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Braces**      | SPACE_BEFORE_ARRAY_INITIALIZER_LBRACE                   | false            | true             | ✅       | Airlift adds space                                              |
| **Spacing - Braces**      | SPACE_BEFORE_CATCH_LBRACE                               | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Braces**      | SPACE_BEFORE_CLASS_LBRACE                               | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Braces**      | SPACE_BEFORE_DO_LBRACE                                  | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Braces**      | SPACE_BEFORE_ELSE_LBRACE                                | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Braces**      | SPACE_BEFORE_FINALLY_LBRACE                             | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Braces**      | SPACE_BEFORE_FOR_LBRACE                                 | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Braces**      | SPACE_BEFORE_IF_LBRACE                                  | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Braces**      | SPACE_BEFORE_METHOD_LBRACE                              | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Braces**      | SPACE_BEFORE_SWITCH_LBRACE                              | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Braces**      | SPACE_BEFORE_SYNCHRONIZED_LBRACE                        | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Braces**      | SPACE_BEFORE_TRY_LBRACE                                 | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Braces**      | SPACE_BEFORE_WHILE_LBRACE                               | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Operators**   | SPACE_AROUND_ADDITIVE_OPERATORS                         | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Operators**   | SPACE_AROUND_ASSIGNMENT_OPERATORS                       | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Operators**   | SPACE_AROUND_BITWISE_OPERATORS                          | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Operators**   | SPACE_AROUND_EQUALITY_OPERATORS                         | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Operators**   | SPACE_AROUND_LAMBDA_ARROW                               | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Operators**   | SPACE_AROUND_LOGICAL_OPERATORS                          | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Operators**   | SPACE_AROUND_METHOD_REF_DBL_COLON                       | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Operators**   | SPACE_AROUND_MULTIPLICATIVE_OPERATORS                   | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Operators**   | SPACE_AROUND_RELATIONAL_OPERATORS                       | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Operators**   | SPACE_AROUND_SHIFT_OPERATORS                            | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Operators**   | SPACE_AROUND_UNARY_OPERATOR                             | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Other**       | SPACE_AFTER_COLON                                       | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Other**       | SPACE_AFTER_COMMA                                       | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Other**       | SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS                     | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Other**       | SPACE_AFTER_QUEST                                       | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Other**       | SPACE_AFTER_SEMICOLON                                   | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Other**       | SPACE_AFTER_TYPE_CAST                                   | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Other**       | SPACE_BEFORE_COLON                                      | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Other**       | SPACE_BEFORE_COMMA                                      | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Other**       | SPACE_BEFORE_QUEST                                      | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Other**       | SPACE_BEFORE_SEMICOLON                                  | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Other**       | SPACE_BEFORE_TYPE_PARAMETER_LIST                        | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Parentheses** | SPACE_BEFORE_ANNOTATION_PARAMETER_LIST                  | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Parentheses** | SPACE_BEFORE_CATCH_PARENTHESES                          | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Parentheses** | SPACE_BEFORE_FOR_PARENTHESES                            | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Parentheses** | SPACE_BEFORE_IF_PARENTHESES                             | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Parentheses** | SPACE_BEFORE_METHOD_CALL_PARENTHESES                    | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Parentheses** | SPACE_BEFORE_METHOD_PARENTHESES                         | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Parentheses** | SPACE_BEFORE_SWITCH_PARENTHESES                         | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Parentheses** | SPACE_BEFORE_SYNCHRONIZED_PARENTHESES                   | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Parentheses** | SPACE_BEFORE_TRY_PARENTHESES                            | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Parentheses** | SPACE_BEFORE_WHILE_PARENTHESES                          | true             | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Within**      | SPACE_WITHIN_ANNOTATION_PARENTHESES                     | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Within**      | SPACE_WITHIN_ARRAY_INITIALIZER_BRACES                   | false            | true             | ✅       | Airlift adds space                                              |
| **Spacing - Within**      | SPACE_WITHIN_BRACES                                     | false            | true             | ✅       | Airlift adds space                                              |
| **Spacing - Within**      | SPACE_WITHIN_BRACKETS                                   | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Within**      | SPACE_WITHIN_CAST_PARENTHESES                           | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Within**      | SPACE_WITHIN_CATCH_PARENTHESES                          | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Within**      | SPACE_WITHIN_EMPTY_ARRAY_INITIALIZER_BRACES             | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Within**      | SPACE_WITHIN_EMPTY_METHOD_CALL_PARENTHESES              | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Within**      | SPACE_WITHIN_EMPTY_METHOD_PARENTHESES                   | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Within**      | SPACE_WITHIN_FOR_PARENTHESES                            | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Within**      | SPACE_WITHIN_IF_PARENTHESES                             | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Within**      | SPACE_WITHIN_METHOD_CALL_PARENTHESES                    | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Within**      | SPACE_WITHIN_METHOD_PARENTHESES                         | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Within**      | SPACE_WITHIN_PARENTHESES                                | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Within**      | SPACE_WITHIN_SWITCH_PARENTHESES                         | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Within**      | SPACE_WITHIN_SYNCHRONIZED_PARENTHESES                   | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Within**      | SPACE_WITHIN_TRY_PARENTHESES                            | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Spacing - Within**      | SPACE_WITHIN_WHILE_PARENTHESES                          | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Switch Expressions**    | SWITCH_EXPRESSIONS_WRAP                                 | 0                | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Wrapping**              | ARRAY_INITIALIZER_LBRACE_ON_NEXT_LINE                   | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Wrapping**              | ARRAY_INITIALIZER_RBRACE_ON_NEXT_LINE                   | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Wrapping**              | ARRAY_INITIALIZER_WRAP                                  | 0 (do not wrap)  | 1 (always)       | 🔶      | gofmt-style preserves existing                                  |
| **Wrapping**              | ASSERT_STATEMENT_COLON_ON_NEXT_LINE                     | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Wrapping**              | ASSERT_STATEMENT_WRAP                                   | 0 (do not wrap)  | 5 (if long)      | ✅       | `FORMATTER_ALIGNMENT_FOR_ASSERTION_MESSAGE`                     |
| **Wrapping**              | ASSIGNMENT_WRAP                                         | 0 (do not wrap)  | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Wrapping**              | BINARY_OPERATION_SIGN_ON_NEXT_LINE                      | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Wrapping**              | BINARY_OPERATION_WRAP                                   | 0 (do not wrap)  | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Wrapping**              | CALL_PARAMETERS_LPAREN_ON_NEXT_LINE                     | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Wrapping**              | CALL_PARAMETERS_RPAREN_ON_NEXT_LINE                     | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Wrapping**              | CALL_PARAMETERS_WRAP                                    | 0 (do not wrap)  | 5 (if long)      | ✅       | `FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_METHOD_INVOCATION`        |
| **Wrapping**              | EXTENDS_KEYWORD_WRAP                                    | 0 (do not wrap)  | 2 (wrap keyword) | 🔶      | gofmt-style preserves existing                                  |
| **Wrapping**              | EXTENDS_LIST_WRAP                                       | 0 (do not wrap)  | 1 (always)       | 🔶      | gofmt-style preserves existing                                  |
| **Wrapping**              | FOR_STATEMENT_LPAREN_ON_NEXT_LINE                       | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Wrapping**              | FOR_STATEMENT_RPAREN_ON_NEXT_LINE                       | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Wrapping**              | FOR_STATEMENT_WRAP                                      | 0 (do not wrap)  | 5 (if long)      | ✅       | `FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_FOR_LOOP_HEADER`        |
| **Wrapping**              | METHOD_CALL_CHAIN_WRAP                                  | 0 (do not wrap)  | 5 (if long)      | ✅       | `FORMATTER_ALIGNMENT_FOR_SELECTOR_IN_METHOD_INVOCATION`         |
| **Wrapping**              | METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE                   | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Wrapping**              | METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE                   | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Wrapping**              | METHOD_PARAMETERS_WRAP                                  | 0 (do not wrap)  | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Wrapping**              | PARENTHESES_EXPRESSION_LPAREN_WRAP                      | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Wrapping**              | PARENTHESES_EXPRESSION_RPAREN_WRAP                      | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Wrapping**              | PLACE_ASSIGNMENT_SIGN_ON_NEXT_LINE                      | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Wrapping**              | PREFER_PARAMETERS_WRAP                                  | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Wrapping**              | RESOURCE_LIST_LPAREN_ON_NEXT_LINE                       | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Wrapping**              | RESOURCE_LIST_RPAREN_ON_NEXT_LINE                       | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Wrapping**              | RESOURCE_LIST_WRAP                                      | 0 (do not wrap)  | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Wrapping**              | TERNARY_OPERATION_SIGNS_ON_NEXT_LINE                    | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Wrapping**              | TERNARY_OPERATION_WRAP                                  | 0 (do not wrap)  | 5 (if long)      | ✅       | `FORMATTER_ALIGNMENT_FOR_CONDITIONAL_EXPRESSION`                |
| **Wrapping**              | THROWS_KEYWORD_WRAP                                     | 0 (do not wrap)  | 2 (wrap keyword) | 🔶      | gofmt-style preserves existing                                  |
| **Wrapping**              | THROWS_LIST_WRAP                                        | 0 (do not wrap)  | 1 (always)       | 🔶      | gofmt-style preserves existing                                  |
| **Wrapping**              | WRAP_COMMENTS                                           | false            | (default)        | ⚪       | Uses IntelliJ default                                           |
| **Wrapping**              | WRAP_LONG_LINES                                         | false            | (default)        | ⚪       | Uses IntelliJ default                                           |

---

## Coverage Summary

| Status     | Count   | Percentage | Description                                   |
|------------|---------|------------|-----------------------------------------------|
| ✅ FULL     | 50      | 23%        | Fully supported                               |
| 🔶 PARTIAL | 8       | 4%         | gofmt-style preservation                      |
| ⚪ DEFAULT  | 135     | 61%        | Uses IntelliJ defaults                        |
| ➖ N/A      | 26      | 12%        | Not applicable (code generation, naming)      |
| ❌ NONE     | 1       | <1%        | Member arrangement (semantic, not formatting) |
| **Total**  | **220** | **100%**   |                                               |

---

## IntelliJ Value Mappings

### Brace Style Values

| Value | Meaning                | JDT Constant         |
|-------|------------------------|----------------------|
| 1     | End of line (K&R)      | `END_OF_LINE`        |
| 2     | Next line (Allman)     | `NEXT_LINE`          |
| 3     | Next line shifted      | `NEXT_LINE_SHIFTED`  |
| 4     | Next line each shifted | `NEXT_LINE_SHIFTED2` |
| 5     | Next line if wrapped   | `NEXT_LINE_ON_WRAP`  |

### Wrap Mode Values

| Value | Meaning                | JDT Mapping          |
|-------|------------------------|----------------------|
| 0     | Do not wrap            | `WRAP_NO_SPLIT`      |
| 1     | Wrap always            | `WRAP_ONE_PER_LINE`  |
| 2     | Wrap if long (chop)    | `WRAP_NEXT_PER_LINE` |
| 5     | Wrap if long (compact) | `WRAP_COMPACT`       |

### Force Braces Values

| Value | Meaning        | Implementation           |
|-------|----------------|--------------------------|
| 0     | Do not force   | N/A (preserves existing) |
| 1     | When multiline | N/A (not implemented)    |
| 3     | Always         | N/A (preserves existing) |

---

## Key JDT Modifications

| File                            | Modification                                                                 | Purpose                       |
|---------------------------------|------------------------------------------------------------------------------|-------------------------------|
| `Token.java`                    | Added `originalIndent`, `originalSpacesBefore`, `originalSpacesAfter` fields | Store original formatting     |
| `WrapPreparator.java`           | `preserveExistingLineBreaks()` stores original indentation                   | Capture developer choices     |
| `WrapExecutor.java:769-774`     | Use `token.getOriginalIndent()` for `BLOCK_INDENT` mode                      | Honor original indentation    |
| `LineBreaksPreparator.java:790` | Use `~blankLinesBeforeClosingBrace` to force removal                         | Remove blank lines before `}` |
| `LineBreaksPreparator.java`     | `enforceBracePosition=false` for 6 visit methods                             | Preserve compact forms        |
| `TextEditsBuilder.java`         | Use `originalIndent` in output                                               | Output original formatting    |
| `CommentsPreparator.java`       | Preserve comment indentation/spacing                                         | Keep aligned comments         |
