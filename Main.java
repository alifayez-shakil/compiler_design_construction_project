import java.util.List;

public class Main {

    public static void main(String[] args) {
        // ---- Test 1: a valid program ----
        String validProgram =
                "num x := 10;\n" +
                "num result := x + 5 * 2;\n" +
                "print(result);\n";

        runTest("VALID PROGRAM TEST", validProgram);

        // ---- Test 2: an invalid program (missing semicolon) ----
        String invalidProgram =
                "num x := 10\n" +          // missing ';'
                "num result := x + ;\n" +  // missing expression after '+'
                "print(result)\n";         // missing ';'

        runTest("INVALID PROGRAM TEST", invalidProgram);
    }

    private static void runTest(String title, String source) {
        System.out.println("========================================");
        System.out.println(title);
        System.out.println("========================================");

        System.out.println("\n===== SOURCE CODE =====\n");
        System.out.println(source);

        System.out.println("===== TOKENS =====\n");
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        for (Token token : tokens) {
            System.out.println(token);
        }

        System.out.println("\n===== PARSER =====\n");
        Parser parser = new Parser(tokens);
        boolean success = parser.parseProgram();

        if (success) {
            System.out.println("\nParsing Successful");
            System.out.println("Syntax is Valid");
        } else {
            System.out.println("\nParsing Failed");
            System.out.println("Syntax is Invalid");
        }
        System.out.println();
    }
}
