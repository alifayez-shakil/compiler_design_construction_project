import java.util.List;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {

        runTest("1. VALID PROGRAM (declarations, if/else, while, print)",
                "gona x := 10;\n" +
                "doshomik y := 2.5;\n" +
                "gona sum := x + 5 * 2;\n" +
                "\n" +
                "jodi (sum > 15) {\n" +
                "    koiyo(sum);\n" +
                "} nohoile {\n" +
                "    koiyo(x);\n" +
                "}\n" +
                "\n" +
                "jotokhon (x > 0) {\n" +
                "    x := x - 1;\n" +
                "}\n"
        );

        runTest("2. SYNTAX ERROR (missing semicolon, missing expression)",
                "gona x := 10\n" +            // missing ';'
                "gona result := x + ;\n" +    // missing expression after '+'
                "koiyo(result)\n"             // missing ';'
        );

        runTest("3. TYPE ERROR (assigning doshomik value into gona variable)",
                "gona x := 3.14;\n" +
                "koiyo(x);\n"
        );

        runTest("4. USE-BEFORE-DECLARE ERROR",
                "gona x := y + 1;\n"
        );

        runTest("5. DIVISION BY ZERO (caught at semantic check)",
                "gona x := 10 / 0;\n"
        );

        runTest("6. CONDITION TYPE ERROR (plain arithmetic instead of a comparison)",
                "gona x := 5;\n" +
                "jodi (x + 1) {\n" +
                "    koiyo(x);\n" +
                "}\n"
        );
    }

    private static void runTest(String title, String source) {
        System.out.println("========================================");
        System.out.println(title);
        System.out.println("========================================");

        // ---------- PHASE 1: LEXER ----------
        System.out.println("\n===== PHASE 1: LEXICAL ANALYSIS (TOKENS) =====\n");
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        for (Token token : tokens) {
            System.out.println(token);
        }

        // ---------- PHASE 2: PARSER ----------
        System.out.println("\n===== PHASE 2: SYNTAX ANALYSIS (PARSER) =====\n");
        Parser parser = new Parser(tokens);
        Ast.Program program = parser.parseProgram();
        boolean parseOk = !parser.hasErrors();

        if (!parseOk) {
            System.out.println("\nParsing Failed - Syntax is Invalid");
            System.out.println("(skipping semantic analysis and code generation)\n");
            return;
        }
        System.out.println("Parsing Successful - Syntax is Valid");

        // ---------- PHASE 3: SEMANTIC ANALYSIS ----------
        System.out.println("\n===== PHASE 3: SEMANTIC ANALYSIS =====\n");
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        boolean semanticOk = analyzer.analyze(program);

        if (!semanticOk) {
            System.out.println("\nSemantic analysis failed.");
            System.out.println("(skipping code generation)\n");
            return;
        }
        System.out.println("No semantic errors - program is well-typed.");

        // ---------- PHASE 4: CODE GENERATION (PYTHON) ----------
        System.out.println("\n===== PHASE 4: CODE GENERATION (PYTHON TARGET) =====\n");
        PythonCodeGenerator generator = new PythonCodeGenerator();
        String pythonCode = generator.generate(program);
        System.out.println(pythonCode);

        // ---------- PHASE 5: EXECUTE GENERATED PYTHON ----------
        try {
            Files.writeString(Paths.get("generated_program.py"), pythonCode);
            System.out.println("(generated code written to generated_program.py)\n");
            runPython("generated_program.py");
        } catch (Exception e) {
            System.out.println("Could not write generated file: " + e.getMessage());
        }

        System.out.println();
    }

    /** Try to run the generated Python file with python3, falling back to python. */
    private static void runPython(String filename) {
        for (String cmd : new String[]{"python3", "python"}) {
            try {
                Process p = new ProcessBuilder(cmd, filename)
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