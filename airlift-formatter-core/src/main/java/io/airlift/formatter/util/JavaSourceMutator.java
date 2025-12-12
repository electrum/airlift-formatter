/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Randomly mutates Java source code in syntactically valid ways that only affect formatting.
 * This is useful for testing that a formatter produces consistent output regardless of input formatting.
 *
 * <p>The mutations include:
 * <ul>
 *   <li>Removing/adding spaces around keywords and operators</li>
 *   <li>Changing brace placement (same line vs new line)</li>
 *   <li>Adding/removing blank lines</li>
 *   <li>Messing up indentation</li>
 *   <li>Shuffling import order</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * JavaSourceMutator mutator = new JavaSourceMutator();
 * Random random = new Random(12345);  // Use seed for reproducibility
 * String mutated = mutator.mutate(sourceCode, random, 0.3);
 * }</pre>
 */
public class JavaSourceMutator
{
    private static final double DEFAULT_PROBABILITY = 0.3;

    private final List<Mutator> mutators;

    /**
     * Creates a mutator with all default mutation strategies.
     */
    public JavaSourceMutator()
    {
        this(createDefaultMutators());
    }

    /**
     * Creates a mutator with custom mutation strategies.
     *
     * @param mutators the list of mutators to use
     */
    public JavaSourceMutator(List<Mutator> mutators)
    {
        this.mutators = new ArrayList<>(mutators);
    }

    /**
     * Creates and returns the default list of mutators.
     */
    public static List<Mutator> createDefaultMutators()
    {
        List<Mutator> mutators = new ArrayList<>();

        // Whitespace mutations
        mutators.add(new RemoveKeywordSpaceMutator());
        mutators.add(new RemoveBraceSpaceMutator());
        mutators.add(new AddSpaceInsideParensMutator());
        mutators.add(new RemoveOperatorSpaceMutator());
        mutators.add(new AddOperatorSpaceMutator());

        // Blank line mutations
        mutators.add(new AddBlankLinesMutator());
        mutators.add(new RemoveBlankLinesMutator());

        // Indentation mutations
        mutators.add(new MessUpIndentationMutator());

        // Brace placement mutations
        mutators.add(new MoveBraceToSameLineMutator());
        mutators.add(new MoveBraceToNewLineMutator());

        // Import mutations
        mutators.add(new ShuffleImportsMutator());

        // Line wrapping mutations
        mutators.add(new JoinLinesMutator());
        mutators.add(new SplitLineMutator());

        return mutators;
    }

    /**
     * Mutates the source code using the default probability.
     *
     * @param source the source code to mutate
     * @param random the random number generator
     * @return the mutated source code
     */
    public String mutate(String source, Random random)
    {
        return mutate(source, random, DEFAULT_PROBABILITY);
    }

    /**
     * Mutates the source code with the specified probability for each mutation.
     *
     * @param source the source code to mutate
     * @param random the random number generator
     * @param probability the probability of applying each mutation (0.0 to 1.0)
     * @return the mutated source code
     */
    public String mutate(String source, Random random, double probability)
    {
        String result = source;
        for (Mutator mutator : mutators) {
            if (random.nextDouble() < probability) {
                result = mutator.mutate(result, random);
            }
        }
        return result;
    }

    /**
     * Returns the number of mutations applied to transform the source.
     *
     * @param source the source code to mutate
     * @param random the random number generator
     * @param probability the probability of applying each mutation
     * @return a result containing the mutated source and count of mutations applied
     */
    public MutationResult mutateWithCount(String source, Random random, double probability)
    {
        String result = source;
        int count = 0;
        for (Mutator mutator : mutators) {
            if (random.nextDouble() < probability) {
                String mutated = mutator.mutate(result, random);
                if (!mutated.equals(result)) {
                    result = mutated;
                    count++;
                }
            }
        }
        return new MutationResult(result, count);
    }

    /**
     * Result of a mutation operation.
     */
    public static class MutationResult
    {
        private final String source;
        private final int mutationCount;

        public MutationResult(String source, int mutationCount)
        {
            this.source = source;
            this.mutationCount = mutationCount;
        }

        public String getSource()
        {
            return source;
        }

        public int getMutationCount()
        {
            return mutationCount;
        }
    }

    // ========== Mutator Interface ==========

    /**
     * Interface for individual mutation strategies.
     */
    public interface Mutator
    {
        /**
         * Applies a mutation to the source code.
         *
         * @param source the source code to mutate
         * @param random the random number generator
         * @return the mutated source code
         */
        String mutate(String source, Random random);
    }

    // ========== Mutator Implementations ==========

    /**
     * Removes space after control flow keywords: "if (" → "if("
     */
    public static class RemoveKeywordSpaceMutator
            implements Mutator
    {
        private static final Pattern PATTERN = Pattern.compile(
                "\\b(if|for|while|switch|catch|synchronized)\\s+\\(");

        @Override
        public String mutate(String source, Random random)
        {
            Matcher matcher = PATTERN.matcher(source);
            StringBuilder result = new StringBuilder();
            while (matcher.find()) {
                if (random.nextBoolean()) {
                    matcher.appendReplacement(result, matcher.group(1) + "(");
                }
            }
            matcher.appendTail(result);
            return result.toString();
        }
    }

    /**
     * Removes space before opening brace: ") {" → "){"
     */
    public static class RemoveBraceSpaceMutator
            implements Mutator
    {
        private static final Pattern PATTERN = Pattern.compile("\\)\\s+\\{");

        @Override
        public String mutate(String source, Random random)
        {
            Matcher matcher = PATTERN.matcher(source);
            StringBuilder result = new StringBuilder();
            while (matcher.find()) {
                if (random.nextBoolean()) {
                    matcher.appendReplacement(result, "){");
                }
            }
            matcher.appendTail(result);
            return result.toString();
        }
    }

    /**
     * Adds spaces inside parentheses: "(x)" → "( x )"
     */
    public static class AddSpaceInsideParensMutator
            implements Mutator
    {
        private static final Pattern PATTERN = Pattern.compile("\\(([^()\\s][^()]*)\\)");

        @Override
        public String mutate(String source, Random random)
        {
            Matcher matcher = PATTERN.matcher(source);
            StringBuilder result = new StringBuilder();
            while (matcher.find()) {
                if (random.nextDouble() < 0.2) { // Lower probability - too aggressive otherwise
                    String inner = matcher.group(1);
                    if (!inner.startsWith(" ") && !inner.endsWith(" ")) {
                        matcher.appendReplacement(result, "( " + Matcher.quoteReplacement(inner) + " )");
                    }
                }
            }
            matcher.appendTail(result);
            return result.toString();
        }
    }

    /**
     * Removes spaces around binary operators: "a + b" → "a+b"
     */
    public static class RemoveOperatorSpaceMutator
            implements Mutator
    {
        private static final Pattern PATTERN = Pattern.compile(
                "(\\w)\\s+([+\\-*/%&|^])\\s+(\\w)");

        @Override
        public String mutate(String source, Random random)
        {
            Matcher matcher = PATTERN.matcher(source);
            StringBuilder result = new StringBuilder();
            while (matcher.find()) {
                if (random.nextBoolean()) {
                    matcher.appendReplacement(result,
                            matcher.group(1) + matcher.group(2) + matcher.group(3));
                }
            }
            matcher.appendTail(result);
            return result.toString();
        }
    }

    /**
     * Adds extra spaces around operators: "a+b" → "a  +  b"
     */
    public static class AddOperatorSpaceMutator
            implements Mutator
    {
        private static final Pattern PATTERN = Pattern.compile(
                "(\\w)\\s*([+\\-*/%])\\s*(\\w)");

        @Override
        public String mutate(String source, Random random)
        {
            Matcher matcher = PATTERN.matcher(source);
            StringBuilder result = new StringBuilder();
            while (matcher.find()) {
                if (random.nextDouble() < 0.15) {
                    int spaces = random.nextInt(3) + 2; // 2-4 spaces
                    String padding = " ".repeat(spaces);
                    matcher.appendReplacement(result,
                            matcher.group(1) + padding + matcher.group(2) + padding + matcher.group(3));
                }
            }
            matcher.appendTail(result);
            return result.toString();
        }
    }

    /**
     * Adds random blank lines between statements.
     */
    public static class AddBlankLinesMutator
            implements Mutator
    {
        @Override
        public String mutate(String source, Random random)
        {
            String[] lines = source.split("\n", -1);
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < lines.length; i++) {
                result.append(lines[i]);
                if (i < lines.length - 1) {
                    result.append("\n");
                    // Randomly add 1-3 extra blank lines
                    if (random.nextDouble() < 0.1 && lines[i].trim().endsWith(";")) {
                        int extraLines = random.nextInt(3) + 1;
                        result.append("\n".repeat(extraLines));
                    }
                }
            }

            return result.toString();
        }
    }

    /**
     * Removes blank lines between code.
     */
    public static class RemoveBlankLinesMutator
            implements Mutator
    {
        private static final Pattern PATTERN = Pattern.compile("\n\\s*\n\\s*\n");

        @Override
        public String mutate(String source, Random random)
        {
            Matcher matcher = PATTERN.matcher(source);
            StringBuilder result = new StringBuilder();
            while (matcher.find()) {
                if (random.nextBoolean()) {
                    matcher.appendReplacement(result, "\n");
                }
            }
            matcher.appendTail(result);
            return result.toString();
        }
    }

    /**
     * Messes up indentation by adding or removing spaces.
     */
    public static class MessUpIndentationMutator
            implements Mutator
    {
        @Override
        public String mutate(String source, Random random)
        {
            String[] lines = source.split("\n", -1);
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (random.nextDouble() < 0.2 && line.startsWith(" ")) {
                    // Randomly modify indentation
                    int currentIndent = 0;
                    while (currentIndent < line.length() && line.charAt(currentIndent) == ' ') {
                        currentIndent++;
                    }

                    if (currentIndent > 0) {
                        int change = random.nextInt(9) - 4; // -4 to +4
                        int newIndent = Math.max(0, currentIndent + change);
                        line = " ".repeat(newIndent) + line.substring(currentIndent);
                    }
                }
                result.append(line);
                if (i < lines.length - 1) {
                    result.append("\n");
                }
            }

            return result.toString();
        }
    }

    /**
     * Moves opening brace from new line to same line.
     */
    public static class MoveBraceToSameLineMutator
            implements Mutator
    {
        private static final Pattern PATTERN = Pattern.compile("(\\)|\\w)\\s*\n\\s*\\{");

        @Override
        public String mutate(String source, Random random)
        {
            Matcher matcher = PATTERN.matcher(source);
            StringBuilder result = new StringBuilder();
            while (matcher.find()) {
                if (random.nextBoolean()) {
                    matcher.appendReplacement(result, matcher.group(1) + " {");
                }
            }
            matcher.appendTail(result);
            return result.toString();
        }
    }

    /**
     * Moves opening brace from same line to new line.
     */
    public static class MoveBraceToNewLineMutator
            implements Mutator
    {
        private static final Pattern PATTERN = Pattern.compile("(\\)|\\w)\\s*\\{\\s*$", Pattern.MULTILINE);

        @Override
        public String mutate(String source, Random random)
        {
            Matcher matcher = PATTERN.matcher(source);
            StringBuilder result = new StringBuilder();
            while (matcher.find()) {
                if (random.nextDouble() < 0.3) {
                    matcher.appendReplacement(result, matcher.group(1) + "\n{");
                }
            }
            matcher.appendTail(result);
            return result.toString();
        }
    }

    /**
     * Shuffles import statements.
     */
    public static class ShuffleImportsMutator
            implements Mutator
    {
        @Override
        public String mutate(String source, Random random)
        {
            // Find the import block
            String[] lines = source.split("\n", -1);
            int importStart = -1;
            int importEnd = -1;
            List<String> imports = new ArrayList<>();
            List<String> staticImports = new ArrayList<>();

            for (int i = 0; i < lines.length; i++) {
                String trimmed = lines[i].trim();
                if (trimmed.startsWith("import ")) {
                    if (importStart == -1) {
                        importStart = i;
                    }
                    importEnd = i;
                    if (trimmed.startsWith("import static ")) {
                        staticImports.add(lines[i]);
                    }
                    else {
                        imports.add(lines[i]);
                    }
                }
                else if (importStart != -1 && !trimmed.isEmpty() && !trimmed.startsWith("import")) {
                    break;
                }
            }

            if (imports.size() <= 1) {
                return source;
            }

            // Shuffle imports
            Collections.shuffle(imports, random);
            Collections.shuffle(staticImports, random);

            // Rebuild source
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < importStart; i++) {
                result.append(lines[i]).append("\n");
            }

            // Write shuffled imports (no blank lines between them)
            for (String imp : imports) {
                result.append(imp).append("\n");
            }
            for (String imp : staticImports) {
                result.append(imp).append("\n");
            }

            // Skip old import lines and continue
            for (int i = importEnd + 1; i < lines.length; i++) {
                result.append(lines[i]);
                if (i < lines.length - 1) {
                    result.append("\n");
                }
            }

            return result.toString();
        }
    }

    /**
     * Joins consecutive lines that could be on one line.
     */
    public static class JoinLinesMutator
            implements Mutator
    {
        @Override
        public String mutate(String source, Random random)
        {
            String[] lines = source.split("\n", -1);
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                String trimmed = line.trim();

                // Try to join with next line if this line ends with an operator or comma
                if (i < lines.length - 1 && random.nextDouble() < 0.15) {
                    String nextTrimmed = lines[i + 1].trim();
                    if ((trimmed.endsWith(",") || trimmed.endsWith("(") ||
                            trimmed.endsWith("+") || trimmed.endsWith("&&") || trimmed.endsWith("||")) &&
                            !nextTrimmed.isEmpty() && !nextTrimmed.startsWith("//") && !nextTrimmed.startsWith("*")) {
                        result.append(line).append(" ").append(nextTrimmed);
                        i++; // Skip next line
                        if (i < lines.length - 1) {
                            result.append("\n");
                        }
                        continue;
                    }
                }

                result.append(line);
                if (i < lines.length - 1) {
                    result.append("\n");
                }
            }

            return result.toString();
        }
    }

    /**
     * Splits long lines at operators or commas.
     */
    public static class SplitLineMutator
            implements Mutator
    {
        @Override
        public String mutate(String source, Random random)
        {
            String[] lines = source.split("\n", -1);
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];

                // Only split long lines
                if (line.length() > 60 && random.nextDouble() < 0.2) {
                    // Find a good split point (after comma or operator)
                    int splitPoint = -1;
                    for (int j = 40; j < Math.min(line.length() - 10, 100); j++) {
                        char c = line.charAt(j);
                        if (c == ',' || c == '+' || c == '&' || c == '|') {
                            splitPoint = j + 1;
                            break;
                        }
                    }

                    if (splitPoint > 0 && splitPoint < line.length() - 5) {
                        int indentLen = 0;
                        while (indentLen < line.length() && line.charAt(indentLen) == ' ') {
                            indentLen++;
                        }
                        String indent = " ".repeat(indentLen + 8); // Extra indent for continuation

                        result.append(line.substring(0, splitPoint).stripTrailing()).append("\n");
                        result.append(indent).append(line.substring(splitPoint).stripLeading());
                    }
                    else {
                        result.append(line);
                    }
                }
                else {
                    result.append(line);
                }

                if (i < lines.length - 1) {
                    result.append("\n");
                }
            }

            return result.toString();
        }
    }
}
