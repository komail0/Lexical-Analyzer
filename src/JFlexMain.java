import java.io.*;
import java.util.*;


public class JFlexMain {

    public static void main(String[] args) {
        Scanner userInput = new Scanner(System.in);

        File testsDir = new File("." + File.separator + "tests");

        if (!testsDir.exists() || !testsDir.isDirectory()) {
            System.out.println("ERROR: tests folder not found at: "
                    + testsDir.getAbsolutePath());
            userInput.close();
            return;
        }

        File[] kwFiles = testsDir.listFiles(f -> f.getName().endsWith(".kw"));

        if (kwFiles == null || kwFiles.length == 0) {
            System.out.println("ERROR: No .kw files found in: "
                    + testsDir.getAbsolutePath());
            userInput.close();
            return;
        }

        Arrays.sort(kwFiles, (a, b) -> a.getName().compareTo(b.getName()));

        System.out.println("=======================");
        System.out.println("|     JFLEX SCANNER    |");
        System.out.println("========================");
        System.out.println("Available test files:");
        System.out.println("-------------------------------------------");
        for (int i = 0; i < kwFiles.length; i++) {
            System.out.printf("  [%d] %s%n", i + 1, kwFiles[i].getName());
        }
        System.out.println("-------------------------------------------");
        System.out.print("Enter file number (1-" + kwFiles.length + "): ");

        int choice = -1;
        while (choice < 1 || choice > kwFiles.length) {
            try {
                choice = Integer.parseInt(userInput.nextLine().trim());
                if (choice < 1 || choice > kwFiles.length)
                    System.out.print("Invalid. Enter (1-" + kwFiles.length + "): ");
            } catch (NumberFormatException e) {
                System.out.print("Invalid. Enter (1-" + kwFiles.length + "): ");
            }
        }

        File selectedFile = kwFiles[choice - 1];
        System.out.println("\nReading: " + selectedFile.getName());
        System.out.println("===========================================");

        try {
            FileReader fr    = new FileReader(selectedFile);
            Yylex      lexer = new Yylex(fr);
            Token      token;
            int        count = 0;

            // Part A: Print tokens
            System.out.println("\n=== TOKENS ===");
            while ((token = lexer.yylex()) != null) {
                if (token.getType() != TokenType.WHITESPACE) {
                    System.out.println(token);
                    count++;
                }
            }

            // Part B: Pre-processing summary
            System.out.println("\n=== PRE-PROCESSING ===");
            System.out.println("File               : " + selectedFile.getName());
            System.out.println("Total tokens       : " + count);
            System.out.println("Whitespace removed : " + lexer.getWhitespaceRemoved());

            // Part E: Symbol table
            lexer.getSymbolTable().display();

            // Part 3: Error handling
            lexer.getErrorHandler().displayErrors();
            lexer.getErrorHandler().displaySummary();

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }

        userInput.close();
    }
}