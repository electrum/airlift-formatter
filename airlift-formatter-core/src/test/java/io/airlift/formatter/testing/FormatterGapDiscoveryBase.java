package io.airlift.formatter.testing;

import io.airlift.formatter.checkstyle.CheckstyleValidator;
import io.airlift.formatter.checkstyle.CheckstyleViolation;
import io.airlift.formatter.util.JavaSourceMutator;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base class for formatter gap discovery tests.
 *
 * <p>Provides common infrastructure for testing formatter implementations
 * against real codebases (Airlift, Trino) to discover formatting gaps.
 *
 * <h2>Usage</h2>
 * <pre>
 * public class MyFormatterGapTest extends FormatterGapDiscoveryBase {
 *     private MyFormatter formatter;
 *
 *     {@literal @}BeforeAll
 *     void setup() {
 *         formatter = new MyFormatter();
 *     }
 *
 *     {@literal @}Test
 *     void testAirlift() throws IOException {
 *         verifyNoChanges("airlift/airlift", getAirliftDir(), formatter::format);
 *     }
 * }
 * </pre>
 */
public abstract class FormatterGapDiscoveryBase
{
    /**
     * Gets the directory containing cloned test repositories.
     * Override to customize the location.
     *
     * @return path to the repos directory
     */
    protected Path getReposDir()
    {
        return Path.of(System.getProperty("repos.dir", "target/repos")).toAbsolutePath();
    }

    /**
     * Gets the Trino repository directory.
     *
     * @return path to cloned Trino repository
     */
    protected Path getTrinoDir()
    {
        return getReposDir().resolve("trino");
    }

    /**
     * Gets the Airlift repository directory.
     *
     * @return path to cloned Airlift repository
     */
    protected Path getAirliftDir()
    {
        return getReposDir().resolve("airlift");
    }

    /**
     * Verifies that the formatter produces no changes on already-formatted code.
     *
     * @param repoName display name for the repository
     * @param repoDir path to the repository
     * @param formatter the formatter to test
     * @return verification result
     * @throws IOException if reading files fails
     */
    public VerificationResult verifyNoChanges(String repoName, Path repoDir, JavaFormatter formatter)
            throws IOException
    {
        List<Path> allFiles = findAllJavaFiles(repoDir);
        return verifyNoChanges(repoName, allFiles, formatter);
    }

    /**
     * Verifies that the formatter produces no changes on a list of files.
     *
     * @param repoName display name for the repository
     * @param files list of Java files to check
     * @param formatter the formatter to test
     * @return verification result
     */
    public VerificationResult verifyNoChanges(String repoName, List<Path> files, JavaFormatter formatter)
    {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("NO-CHANGE VERIFICATION: " + repoName);
        System.out.println("=".repeat(80));
        System.out.println("Total files to check: " + files.size());

        List<ChangedFile> changedFiles = new ArrayList<>();
        int processed = 0;
        int skipped = 0;

        long startTime = System.currentTimeMillis();
        for (Path file : files) {
            String fileName = file.getFileName().toString();
            if (fileName.equals("module-info.java") || fileName.equals("package-info.java")) {
                skipped++;
                processed++;
                continue;
            }

            try {
                long fileStart = System.currentTimeMillis();
                String original = Files.readString(file);
                String formatted = formatter.format(original);
                long fileTime = System.currentTimeMillis() - fileStart;

                if (!original.equals(formatted)) {
                    changedFiles.add(new ChangedFile(file, original, formatted));

                    if (changedFiles.size() <= 5) {
                        System.out.println("\nCHANGED: " + file);
                        showDiff(original, formatted, 5);
                    }
                }

                // Progress output to stderr (unbuffered) every file
                processed++;
                if (processed % 100 == 0) {
                    long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                    double filesPerSec = processed / (elapsed > 0 ? elapsed : 1.0);
                    int remaining = files.size() - processed;
                    int etaSec = (int) (remaining / filesPerSec);
                    System.err.printf("[%d/%d] %d changed, %.1f files/sec, ETA: %dm %ds%n",
                            processed, files.size(), changedFiles.size(), filesPerSec, etaSec / 60, etaSec % 60);
                }
            }
            catch (Exception e) {
                System.err.println("Error processing " + file + ": " + e.getMessage());
                processed++;
            }
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("RESULTS: " + repoName);
        System.out.println("=".repeat(80));
        System.out.println("Total files checked: " + processed);
        System.out.println("Files skipped: " + skipped);
        System.out.println("Files CHANGED: " + changedFiles.size());

        double unchangedPct = processed > 0 ? (processed - changedFiles.size()) * 100.0 / processed : 100.0;
        System.out.printf("Unchanged: %.1f%%\n", unchangedPct);

        if (changedFiles.isEmpty()) {
            System.out.println("\n SUCCESS: Formatter leaves all files unchanged!");
        }
        else {
            System.out.println("\n RESULT: Formatter modified " + changedFiles.size() + " files");
            categorizeChanges(changedFiles);
            writeChangeReport(repoName, changedFiles);
        }

        return new VerificationResult(processed, skipped, changedFiles);
    }

    /**
     * Verifies that the formatter is idempotent (formatting twice produces same result).
     *
     * @param repoName display name for the repository
     * @param repoDir path to the repository
     * @param formatter the formatter to test
     * @return idempotency result
     * @throws IOException if reading files fails
     */
    public IdempotencyResult verifyIdempotency(String repoName, Path repoDir, JavaFormatter formatter)
            throws IOException
    {
        List<Path> allFiles = findAllJavaFiles(repoDir);
        return verifyIdempotency(repoName, allFiles, formatter);
    }

    /**
     * Verifies that the formatter is idempotent on a list of files.
     *
     * @param repoName display name for the repository
     * @param files list of Java files to check
     * @param formatter the formatter to test
     * @return idempotency result
     */
    public IdempotencyResult verifyIdempotency(String repoName, List<Path> files, JavaFormatter formatter)
    {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("IDEMPOTENCY CHECK: " + repoName);
        System.out.println("=".repeat(80));

        List<String> nonIdempotentFiles = new ArrayList<>();
        int processed = 0;
        int skipped = 0;

        for (Path file : files) {
            String fileName = file.getFileName().toString();
            if (fileName.equals("module-info.java") || fileName.equals("package-info.java")) {
                skipped++;
                continue;
            }

            try {
                String source = Files.readString(file);

                String formatted1 = formatter.format(source);
                String formatted2 = formatter.format(formatted1);

                if (!formatted1.equals(formatted2)) {
                    nonIdempotentFiles.add(file.toString());
                    System.out.println("NON-IDEMPOTENT: " + file);
                    showFirstDifference(file.toString(), formatted1, formatted2);
                }
            }
            catch (Exception e) {
                System.err.println("Error processing " + file + ": " + e.getMessage());
            }

            processed++;
            if (processed % 500 == 0) {
                System.out.println("Checked " + processed + "/" + files.size() + " files...");
            }
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("IDEMPOTENCY RESULTS: " + repoName);
        System.out.println("=".repeat(80));
        System.out.println("Total files checked: " + processed);
        System.out.println("Files skipped: " + skipped);
        System.out.println("Non-idempotent files: " + nonIdempotentFiles.size());

        if (nonIdempotentFiles.isEmpty()) {
            System.out.println("\n SUCCESS: Formatter is idempotent!");
        }
        else {
            System.out.println("\nFAILED: The following files are not idempotent:");
            for (String f : nonIdempotentFiles) {
                System.out.println("  " + f);
            }
        }

        return new IdempotencyResult(processed, skipped, nonIdempotentFiles);
    }

    /**
     * Finds all Java files in a repository, excluding target and generated directories.
     *
     * @param repoDir path to the repository root
     * @return list of Java file paths
     * @throws IOException if walking the file tree fails
     */
    public List<Path> findAllJavaFiles(Path repoDir)
            throws IOException
    {
        List<Path> files = new ArrayList<>();
        String repoDirStr = repoDir.toAbsolutePath().toString();

        try (Stream<Path> walk = Files.walk(repoDir)) {
            walk.filter(p -> p.toString().endsWith(".java"))
                    .filter(Files::isRegularFile)
                    // Only exclude /target/ subdirectories within the repo, not in the path to the repo
                    .filter(p -> {
                        String relativePath = p.toString().substring(repoDirStr.length());
                        return !relativePath.contains("/target/") && !relativePath.contains("/generated/");
                    })
                    .forEach(files::add);
        }

        return files;
    }

    /**
     * Shows a diff between original and formatted content.
     *
     * @param original original content
     * @param formatted formatted content
     * @param maxLines maximum number of diff lines to show
     */
    public void showDiff(String original, String formatted, int maxLines)
    {
        String[] origLines = original.split("\n", -1);
        String[] formLines = formatted.split("\n", -1);

        int shown = 0;
        for (int i = 0; i < Math.min(origLines.length, formLines.length) && shown < maxLines; i++) {
            if (!origLines[i].equals(formLines[i])) {
                System.out.println("  Line " + (i + 1) + ":");
                System.out.println("    - " + escapeWhitespace(origLines[i]));
                System.out.println("    + " + escapeWhitespace(formLines[i]));
                shown++;
            }
        }

        if (origLines.length != formLines.length) {
            System.out.println("  Line count: " + origLines.length + " -> " + formLines.length);
        }
    }

    /**
     * Shows the first difference between two strings.
     *
     * @param fileName file name for display
     * @param s1 first string
     * @param s2 second string
     */
    public void showFirstDifference(String fileName, String s1, String s2)
    {
        String[] lines1 = s1.split("\n", -1);
        String[] lines2 = s2.split("\n", -1);

        for (int i = 0; i < Math.min(lines1.length, lines2.length); i++) {
            if (!lines1[i].equals(lines2[i])) {
                System.out.println("  First difference at line " + (i + 1) + ":");
                System.out.println("    After 1st format: \"" + escapeWhitespace(lines1[i]) + "\"");
                System.out.println("    After 2nd format: \"" + escapeWhitespace(lines2[i]) + "\"");
                return;
            }
        }

        if (lines1.length != lines2.length) {
            System.out.println("  Line count differs: " + lines1.length + " vs " + lines2.length);
        }
    }

    /**
     * Escapes whitespace characters for display.
     *
     * @param s string to escape
     * @return string with visible whitespace markers
     */
    public String escapeWhitespace(String s)
    {
        return s.replace("\t", "\\t").replace(" ", "·");
    }

    /**
     * Categorizes changes by type.
     *
     * @param changes list of changed files
     */
    protected void categorizeChanges(List<ChangedFile> changes)
    {
        Map<String, Integer> categories = new HashMap<>();

        for (ChangedFile change : changes) {
            String category = analyzeChange(change.original(), change.formatted());
            categories.merge(category, 1, Integer::sum);
        }

        System.out.println("\nChange categories:");
        categories.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(e -> System.out.println("  " + e.getKey() + ": " + e.getValue() + " files"));
    }

    /**
     * Analyzes a change to categorize it.
     *
     * @param original original content
     * @param formatted formatted content
     * @return category name
     */
    protected String analyzeChange(String original, String formatted)
    {
        if (original.lines().count() != formatted.lines().count()) {
            return "LINE_COUNT_CHANGED";
        }

        String[] origLines = original.split("\n");
        String[] formLines = formatted.split("\n");

        for (int i = 0; i < origLines.length; i++) {
            if (!origLines[i].equals(formLines[i])) {
                String orig = origLines[i];
                String form = formLines[i];

                if (orig.trim().equals(form.trim())) {
                    if (orig.startsWith(" ") || form.startsWith(" ")) {
                        return "INDENTATION_CHANGED";
                    }
                    return "TRAILING_WHITESPACE";
                }

                if (orig.startsWith("import ") || form.startsWith("import ")) {
                    return "IMPORT_ORDER_CHANGED";
                }

                if (orig.contains("{") || form.contains("{") ||
                        orig.contains("}") || form.contains("}")) {
                    return "BRACE_PLACEMENT_CHANGED";
                }

                if (orig.isBlank() || form.isBlank()) {
                    return "BLANK_LINE_CHANGED";
                }

                return "OTHER_CHANGE";
            }
        }

        return "UNKNOWN";
    }

    /**
     * Writes a detailed change report to a file.
     *
     * @param repoName repository name for the filename
     * @param changedFiles list of changed files
     */
    protected void writeChangeReport(String repoName, List<ChangedFile> changedFiles)
    {
        try {
            Path reportFile = Path.of("target/no-change-report-" +
                    repoName.replace('/', '-') + ".txt");
            Files.createDirectories(reportFile.getParent());

            StringWriter sw = new StringWriter();
            PrintWriter report = new PrintWriter(sw);

            report.println("No-Change Verification Report: " + repoName);
            report.println("=".repeat(80));
            report.println("Total changed files: " + changedFiles.size());
            report.println();

            Map<String, List<ChangedFile>> byCategory = changedFiles.stream()
                    .collect(Collectors.groupingBy(cf -> analyzeChange(cf.original(), cf.formatted())));

            report.println("Summary by category:");
            byCategory.entrySet().stream()
                    .sorted(Comparator.comparing(e -> -e.getValue().size()))
                    .forEach(e -> report.println("  " + e.getKey() + ": " + e.getValue().size() + " files"));

            report.println();
            report.println("Changed files:");
            for (ChangedFile cf : changedFiles) {
                report.println("  " + cf.path());
            }

            report.println();
            report.println("Detailed diffs (first 20 files):");
            report.println("=".repeat(80));

            int count = 0;
            for (ChangedFile cf : changedFiles) {
                if (count++ >= 20) {
                    break;
                }
                report.println();
                report.println("FILE: " + cf.path());
                report.println("-".repeat(80));

                String[] origLines = cf.original().split("\n", -1);
                String[] formLines = cf.formatted().split("\n", -1);

                int diffCount = 0;
                for (int i = 0; i < Math.min(origLines.length, formLines.length) && diffCount < 10; i++) {
                    if (!origLines[i].equals(formLines[i])) {
                        report.println("Line " + (i + 1) + ":");
                        report.println("  - " + escapeWhitespace(origLines[i]));
                        report.println("  + " + escapeWhitespace(formLines[i]));
                        diffCount++;
                    }
                }

                if (origLines.length != formLines.length) {
                    report.println("Line count: " + origLines.length + " -> " + formLines.length);
                }
            }

            Files.writeString(reportFile, sw.toString());
            System.out.println("Detailed report written to: " + reportFile);
        }
        catch (IOException e) {
            System.err.println("Failed to write report: " + e.getMessage());
        }
    }

    /**
     * Record representing a file that was changed by formatting.
     */
    public record ChangedFile(Path path, String original, String formatted) {}

    /**
     * Result of no-change verification.
     */
    public record VerificationResult(
            int processed,
            int skipped,
            List<ChangedFile> changedFiles)
    {
        public boolean passed()
        {
            return changedFiles.isEmpty();
        }

        public int changedCount()
        {
            return changedFiles.size();
        }

        public double unchangedPercentage()
        {
            if (processed == 0) {
                return 100.0;
            }
            return (processed - changedFiles.size()) * 100.0 / processed;
        }
    }

    /**
     * Result of idempotency verification.
     */
    public record IdempotencyResult(
            int processed,
            int skipped,
            List<String> nonIdempotentFiles)
    {
        public boolean passed()
        {
            return nonIdempotentFiles.isEmpty();
        }
    }

    /**
     * Result for a single file's comprehensive test.
     * All 4 checks run independently to get complete picture.
     */
    public record FileTestResult(
            Path file,
            boolean formatPreservesClean,
            boolean checkstylePassesOnClean,
            boolean mutationRestores,
            boolean checkstylePassesAfterRestore,
            String formatDiff,
            String restoreDiff,
            List<CheckstyleViolation> cleanViolations,
            List<CheckstyleViolation> restoreViolations)
    {
        public boolean passed()
        {
            return formatPreservesClean && checkstylePassesOnClean &&
                    mutationRestores && checkstylePassesAfterRestore;
        }

        public List<String> failedStages()
        {
            List<String> stages = new ArrayList<>();
            if (!formatPreservesClean) {
                stages.add("FORMAT_PRESERVATION");
            }
            if (!checkstylePassesOnClean) {
                stages.add("CHECKSTYLE_ON_CLEAN");
            }
            if (!mutationRestores) {
                stages.add("MUTATION_RESTORE");
            }
            if (!checkstylePassesAfterRestore) {
                stages.add("CHECKSTYLE_AFTER_RESTORE");
            }
            return stages;
        }
    }

    /**
     * Aggregate result for comprehensive test run.
     */
    public record ComprehensiveTestResult(
            String repoName,
            int totalFiles,
            int fullyPassed,
            int formatPreservationFailed,
            int checkstyleOnCleanFailed,
            int mutationRestoreFailed,
            int checkstyleAfterRestoreFailed,
            List<FileTestResult> failures)
    {
        public boolean passed()
        {
            return failures.isEmpty();
        }

        public double passRate()
        {
            return totalFiles > 0 ? (double) fullyPassed / totalFiles * 100 : 0;
        }
    }

    /**
     * Runs comprehensive verification on a repository: format preservation, checkstyle,
     * mutation restore, and checkstyle after restore.
     *
     * @param repoName display name for the repository
     * @param repoDir path to the repository
     * @param formatter the formatter to test
     * @param checkstyleValidator the checkstyle validator
     * @param seed random seed for mutation reproducibility
     * @param mutationProbability probability of applying each mutation
     * @return comprehensive test result
     * @throws IOException if reading files fails
     */
    public ComprehensiveTestResult verifyComprehensive(
            String repoName,
            Path repoDir,
            JavaFormatter formatter,
            CheckstyleValidator checkstyleValidator,
            long seed,
            double mutationProbability)
            throws IOException
    {
        List<Path> files = findAllJavaFiles(repoDir);

        System.out.println("\n" + "=".repeat(80));
        System.out.println("COMPREHENSIVE VERIFICATION: " + repoName);
        System.out.println("=".repeat(80));
        System.out.println("Total files to check: " + files.size());
        System.out.println("Mutation seed: " + seed + ", probability: " + mutationProbability);

        List<FileTestResult> failures = new ArrayList<>();
        JavaSourceMutator mutator = new JavaSourceMutator();

        int formatPreservationFailed = 0;
        int checkstyleOnCleanFailed = 0;
        int mutationRestoreFailed = 0;
        int checkstyleAfterRestoreFailed = 0;

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < files.size(); i++) {
            Path file = files.get(i);
            String fileName = file.getFileName().toString();

            // Skip module-info and package-info
            if (fileName.equals("module-info.java") || fileName.equals("package-info.java")) {
                continue;
            }

            FileTestResult result = testFile(file, formatter, checkstyleValidator,
                    mutator, seed + i, mutationProbability);

            if (!result.passed()) {
                failures.add(result);

                for (String stage : result.failedStages()) {
                    switch (stage) {
                        case "FORMAT_PRESERVATION" -> formatPreservationFailed++;
                        case "CHECKSTYLE_ON_CLEAN" -> checkstyleOnCleanFailed++;
                        case "MUTATION_RESTORE" -> mutationRestoreFailed++;
                        case "CHECKSTYLE_AFTER_RESTORE" -> checkstyleAfterRestoreFailed++;
                    }
                }
            }

            // Progress output every 100 files
            if ((i + 1) % 100 == 0) {
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                double filesPerSec = (i + 1) / (elapsed > 0 ? elapsed : 1.0);
                System.err.printf("[%d/%d] %d failures, %.1f files/sec%n",
                        i + 1, files.size(), failures.size(), filesPerSec);
            }
        }

        int fullyPassed = files.size() - failures.size();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("COMPREHENSIVE RESULTS: " + repoName);
        System.out.println("=".repeat(80));
        System.out.printf("Total files: %d%n", files.size());
        System.out.printf("Fully passed: %d (%.1f%%)%n", fullyPassed,
                files.size() > 0 ? fullyPassed * 100.0 / files.size() : 0);
        System.out.printf("Format preservation failed: %d%n", formatPreservationFailed);
        System.out.printf("Checkstyle on clean failed: %d%n", checkstyleOnCleanFailed);
        System.out.printf("Mutation restore failed: %d%n", mutationRestoreFailed);
        System.out.printf("Checkstyle after restore failed: %d%n", checkstyleAfterRestoreFailed);

        if (!failures.isEmpty()) {
            System.out.println("\nFirst 10 failures:");
            failures.stream().limit(10).forEach(f ->
                    System.out.printf("  %s: %s%n", f.failedStages(), f.file()));

            writeComprehensiveReport(repoName, failures);
        }

        return new ComprehensiveTestResult(repoName, files.size(), fullyPassed,
                formatPreservationFailed, checkstyleOnCleanFailed,
                mutationRestoreFailed, checkstyleAfterRestoreFailed, failures);
    }

    /**
     * Tests a single file through all 4 verification stages.
     */
    private FileTestResult testFile(
            Path file,
            JavaFormatter formatter,
            CheckstyleValidator checkstyleValidator,
            JavaSourceMutator mutator,
            long seed,
            double mutationProbability)
    {
        try {
            String original = Files.readString(file);

            // Stage 1-2: Format clean source and check
            String formatted = formatter.format(original);
            boolean formatPreserves = formatted.equals(original);
            String formatDiff = formatPreserves ? null : generateDiff(original, formatted);

            // Stage 3: Checkstyle on formatted (even if format changed it)
            List<CheckstyleViolation> cleanViolations = validateWithCheckstyle(
                    checkstyleValidator, formatted, file.getFileName().toString());
            boolean checkstyleOnClean = cleanViolations.isEmpty();

            // Stage 4-5: Mutate and format
            Random random = new Random(seed);
            String mutated = mutator.mutate(original, random, mutationProbability);
            String restored = formatter.format(mutated);
            boolean mutationRestores = restored.equals(original);
            String restoreDiff = mutationRestores ? null : generateDiff(original, restored);

            // Stage 6: Checkstyle after restore
            List<CheckstyleViolation> restoreViolations = validateWithCheckstyle(
                    checkstyleValidator, restored, file.getFileName().toString());
            boolean checkstyleAfterRestore = restoreViolations.isEmpty();

            return new FileTestResult(file, formatPreserves, checkstyleOnClean,
                    mutationRestores, checkstyleAfterRestore,
                    formatDiff, restoreDiff, cleanViolations, restoreViolations);
        }
        catch (Throwable e) {
            // Return a failure result for exceptions and errors (including AssertionError)
            return new FileTestResult(file, false, false, false, false,
                    e.getClass().getSimpleName() + ": " + e.getMessage(), null, List.of(), List.of());
        }
    }

    /**
     * Validates content with checkstyle using a temporary file.
     */
    private List<CheckstyleViolation> validateWithCheckstyle(
            CheckstyleValidator validator,
            String content,
            String fileName)
    {
        try {
            Path tempFile = Files.createTempFile("checkstyle-", "-" + fileName);
            try {
                Files.writeString(tempFile, content);
                return validator.validate(tempFile);
            }
            finally {
                Files.deleteIfExists(tempFile);
            }
        }
        catch (Exception e) {
            // Return a synthetic violation for errors
            return List.of(new CheckstyleViolation(
                    fileName, 0, 0, "Validation error: " + e.getMessage(), "CheckstyleException"));
        }
    }

    /**
     * Generates a unified diff between original and modified content.
     */
    protected String generateDiff(String original, String modified)
    {
        String[] origLines = original.split("\n", -1);
        String[] modLines = modified.split("\n", -1);

        StringBuilder diff = new StringBuilder();
        int diffCount = 0;
        int maxDiffs = 10;

        for (int i = 0; i < Math.min(origLines.length, modLines.length) && diffCount < maxDiffs; i++) {
            if (!origLines[i].equals(modLines[i])) {
                diff.append("Line ").append(i + 1).append(":\n");
                diff.append("  - ").append(escapeWhitespace(origLines[i])).append("\n");
                diff.append("  + ").append(escapeWhitespace(modLines[i])).append("\n");
                diffCount++;
            }
        }

        if (origLines.length != modLines.length) {
            diff.append("Line count: ").append(origLines.length).append(" -> ").append(modLines.length).append("\n");
        }

        if (diffCount >= maxDiffs) {
            diff.append("... (more differences truncated)\n");
        }

        return diff.toString();
    }

    /**
     * Writes a detailed comprehensive test report.
     */
    protected void writeComprehensiveReport(String repoName, List<FileTestResult> failures)
    {
        try {
            Path reportFile = Path.of("target/comprehensive-report-" +
                    repoName.replace('/', '-') + ".txt");
            Files.createDirectories(reportFile.getParent());

            StringWriter sw = new StringWriter();
            PrintWriter report = new PrintWriter(sw);

            report.println("Comprehensive Test Report: " + repoName);
            report.println("=".repeat(80));
            report.println("Total failures: " + failures.size());
            report.println();

            // Group by failure type
            Map<String, List<FileTestResult>> byStage = new HashMap<>();
            for (FileTestResult f : failures) {
                for (String stage : f.failedStages()) {
                    byStage.computeIfAbsent(stage, k -> new ArrayList<>()).add(f);
                }
            }

            report.println("Failures by stage:");
            byStage.forEach((stage, files) ->
                    report.println("  " + stage + ": " + files.size() + " files"));

            report.println();
            report.println("Detailed failures:");
            report.println("=".repeat(80));

            int count = 0;
            for (FileTestResult f : failures) {
                if (count++ >= 50) {
                    report.println("\n... (more failures truncated)");
                    break;
                }

                report.println();
                report.println("FILE: " + f.file());
                report.println("Failed stages: " + f.failedStages());

                if (f.formatDiff() != null) {
                    report.println("\nFormat diff:");
                    report.println(f.formatDiff());
                }

                if (!f.cleanViolations().isEmpty()) {
                    report.println("\nCheckstyle violations on clean:");
                    f.cleanViolations().forEach(v ->
                            report.println("  Line " + v.line() + ": " + v.message()));
                }

                if (f.restoreDiff() != null) {
                    report.println("\nRestore diff:");
                    report.println(f.restoreDiff());
                }

                if (!f.restoreViolations().isEmpty()) {
                    report.println("\nCheckstyle violations after restore:");
                    f.restoreViolations().forEach(v ->
                            report.println("  Line " + v.line() + ": " + v.message()));
                }

                report.println("-".repeat(80));
            }

            Files.writeString(reportFile, sw.toString());
            System.out.println("Detailed report written to: " + reportFile);
        }
        catch (IOException e) {
            System.err.println("Failed to write report: " + e.getMessage());
        }
    }
}
