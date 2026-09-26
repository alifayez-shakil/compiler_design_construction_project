package compiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;


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

        Arrays.sort(testFiles); 
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

        System.out.println("\n===== PHASE 1: LEXICAL ANALYSIS (TOKENS) =====\n");
        Lexer lexer = new Lexer(source, diagnostics);
        List<Token> tokens = lexer.scanTokens();
        for (Token token : tokens) {
            System.out.println(token);
        }


        if (diagnostics.hasErrors()) {
            System.out.println("\nLexing Failed - invalid characters in source");
            System.out.println("(skipping parser, semantic analysis, and code generation)\n");
            printSummary(diagnostics);
            return;
        }

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

        System.out.println("\n===== PHASE 4: CODE GENERATION (PYTHON TARGET) =====\n");
        CodeGenerator generator = new CodeGenerator();
        String pythonCode = generator.generate(program);
        System.out.println(pythonCode);

        Path runDir = RUNS_DIR.resolve(name);
        Files.createDirectories(runDir);
        Path outputFile = runDir.resolve("generated_program.py");
        Files.writeString(outputFile, pythonCode);
        System.out.println("(generated code written to " + outputFile + ")\n");
        runPython(outputFile);

        printSummary(diagnostics);
        System.out.println();
    }

    private static void printSummary(Diagnostic diagnostics) {
        long errorCount = diagnostics.getEntries().stream()
                .filter(e -> e.severity == Diagnostic.Severity.ERROR).count();
        long warningCount = diagnostics.getEntries().stream()
                .filter(e -> e.severity == Diagnostic.Severity.WARNING).count();

        if (errorCount == 0 && warningCount == 0) return;

        System.out.println("----------------------------------------");
        System.out.println("Summary: " + errorCount + " error(s), " + warningCount + " warning(s)");
    }

 
    private static void runPython(Path filePath) {
    String localAppData = System.getenv("LOCALAPPDATA");
    java.util.List<String> commands = new java.util.ArrayList<>();

    // Full paths to known real Python installations
    if (localAppData != null) {
        commands.add(localAppData + "\\Programs\\Python\\Python314\\python.exe");
        commands.add(localAppData + "\\Programs\\Python\\Python313\\python.exe");
    }

    // The py launcher (usually reliable)
    commands.add("py");

    // Bare commands (may hit the fake Store alias — we detect and skip)
    commands.add("python");
    commands.add("python3");

    for (String cmd : commands) {
        Process p;
        try {
            p = new ProcessBuilder(cmd, filePath.toString())
                    .redirectErrorStream(true)
                    .start();
        } catch (Exception e) {
            continue;
        }
        try {
            String output = new String(p.getInputStream().readAllBytes());
            int exitCode = p.waitFor();

            // Skip the Microsoft Store hijack
            if (exitCode == 9009 || output.contains("Python was not found")) {
                continue;
            }

            System.out.println("===== EXECUTION OUTPUT (" + cmd + ", exit " + exitCode + ") =====\n");
            System.out.println(output);
            return;
        } catch (Exception e) {
            continue;
        }
    }
    System.out.println("(Python not found - skipping execution)");
}
}