git package compiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Entry point. Runs every .bng file under tests/ through the full pipeline:
 *   Lexer -> Parser -> SemanticAnalyzer -> CodeGenerator -> (run the output)
 *
 * Each test's generated Python file is written to build/runs/<test-name>/,
 * matching the project's target folder layout. This also serves as a
 * simple regression check: the "error" tests are expected to fail at a
 * specific phase, and this run confirms they still do.
 */
public class Main {

    private static final Path TESTS_DIR = Paths.get("tests");
    private static final Path RUNS_DIR = Paths.get("build", "runs");

    public static void main(String[] args) throws IOException {
        File[] testFiles = TESTS_DIR.toFile().listFiles((dir, name) -> name.endsWith(".bng"));

        if (testFiles == null || testFiles.length == 0) {
            System.out.println("No .bng test files found under " + TESTS_DIR.toAbsolutePath());
            System.out.println("Run this from the project root (where the tests/ folder lives).");
            return;
        }

        Arrays.sort(testFiles); // stable, alphabetical order

        for (File file : testFiles) {
            String name = file.getName().replace(".bng", "");
            String source = Files.readString(file.toPath());
            runTest(name, source);
        }
    }

    private static void runTest(String name, String source) throws IOException {
        System.out.println("========================================");
        System.out.println(name);
        System.out.println("========================================");

        Diagnostic diagnostics = new Diagnostic();

        // ---------- PHASE 1: LEXER ----------
        System.out.println("\n===== PHASE 1: LEXICAL ANALYSIS (TOKENS) =====\n");
        Lexer lexer = new Lexer(source, diagnostics);
        List<Token> tokens = lexer.scanTokens();
        for (Token token : tokens) {
            System.out.println(token);
        }

        // ---------- PHASE 2: PARSER ----------
        System.out.println("\n===== PHASE 2: SYNTAX ANALYSIS (PARSER) =====\n");
        Parser parser = new Parser(tokens, diagnostics);
        Ast.Program program = parser.parseProgram();

        if (diagnostics.hasErrors()) {
            System.out.println("\nParsing Failed - Syntax is Invalid");
            System.out.println("(skipping semantic analysis and code generation)\n");
            printSummary(diagnostics);
            return;
        }
        System.out.println("Parsing Successful - Syntax is Valid");

        // ---------- PHASE 3: SEMANTIC ANALYSIS ----------
        System.out.println("\n===== PHASE 3: SEMANTIC ANALYSIS =====\n");
        SemanticAnalyzer analyzer = new SemanticAnalyzer(diagnostics);
        boolean semanticOk = analyzer.analyze(program);

        if (!semanticOk) {
            System.out.println("\nSemantic analysis failed.");
            System.out.println("(skipping code generation)\n");
            printSummary(diagnostics);
            return;
        }
        System.out.println("No semantic errors - program is well-typed.");

        // ---------- PHASE 4: CODE GENERATION (PYTHON) ----------
        System.out.println("\n===== PHASE 4: CODE GENERATION (PYTHON TARGET) =====\n");
        CodeGenerator generator = new CodeGenerator();
        String pythonCode = generator.generate(program);
        System.out.println(pythonCode);

        // ---------- PHASE 5: EXECUTE GENERATED PYTHON ----------
        Path runDir = RUNS_DIR.resolve(name);
        Files.createDirectories(runDir);
        Path outputFile = runDir.resolve("generated_program.py");
        Files.writeString(outputFile, pythonCode);
        System.out.println("(generated code written to " + outputFile + ")\n");
        runPython(outputFile);

        printSummary(diagnostics);
        System.out.println();
    }

    /** Prints a one-line "N error(s), M warning(s)" count for this test, if there were any. */
    private static void printSummary(Diagnostic diagnostics) {
        long errorCount = diagnostics.getEntries().stream()
                .filter(e -> e.severity == Diagnostic.Severity.ERROR).count();
        long warningCount = diagnostics.getEntries().stream()
                .filter(e -> e.severity == Diagnostic.Severity.WARNING).count();

        if (errorCount == 0 && warningCount == 0) return;

        System.out.println("----------------------------------------");
        System.out.println("Summary: " + errorCount + " error(s), " + warningCount + " warning(s)");
    }

    /** Try to run the generated Python file with python3, falling back to python. */
    private static void runPython(Path filePath) {
        for (String cmd : new String[]{"python3", "python"}) {
            try {
                Process p = new ProcessBuilder(cmd, filePath.toString())
                        .redirectErrorStream(true)
                        .start();
                String output = new String(p.getInputStream().readAllBytes());
                int exitCode = p.waitFor();
                if (exitCode == 0) {
                    System.out.println("===== EXECUTION OUTPUT (" + cmd + ") =====\n");
                    System.out.println(output);
                    return;
                }
            } catch (Exception ignored) {
                // try next command
            }
        }
        System.out.println("(Python not found on PATH - skipping execution)");
    }
}
