import java.io.*;
import java.util.*;


public class JFlexMain {

    public static void main(String[] args) {
        Scanner userInput = new Scanner(System.in);

        // Go one directory up into tests/
        File testsDir = new File("." + File.separator + "tests");

        if (!testsDir.exists() || !testsDir.isDirectory()) {
            System.out.println("ERROR: tests folder not found at: "
                    + testsDir.getAbsolutePath());
            userInput.close();
            return;
        }

        // Find all .kw files
        File[] kwFiles = testsDir.listFiles(f -> f.getName().endsWith(".kw"));

        if (kwFiles == null || kwFiles.length == 0) {
            System.out.println("ERROR: No .kw files found in: "
                    + testsDir.getAbsolutePath());
            userInput.close();
            return;
        }

        // Sort so they appear as test1.kw, test2.kw ...
        Arrays.sort(kwFiles, (a, b) -> a.getName().compareTo(b.getName()));

        // Show menu
        System.out.println("==========================");
        System.out.println("|      JFLEX SCANNER      |");
        System.out.println("===========================");
        System.out.println("Available test files:");
        System.out.println("-------------------------------------------");
        for (int i = 0; i < kwFiles.length; i++) {
            System.out.printf("  [%d] %s%n", i + 1, kwFiles[i].getName());
        }
        System.out.println("-------------------------------------------");
        System.out.print("Enter file number (1-" + kwFiles.length + "): ");

        // Validate choice
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
            // Pass file to JFlex-generated Yylex
            FileReader fr    = new FileReader(selectedFile);
            Yylex      lexer = new Yylex(fr);
            Token      token;
            int        count = 0;

            // Print tokens (skip whitespace)
            System.out.println("\n=== TOKENS ===");
            while ((token = lexer.yylex()) != null) {
                if (token.getType() != TokenType.WHITESPACE) {
                    System.out.println(token);
                    count++;
                }
            }

            // Pre-processing summary (Part B)
            System.out.println("\n=== PRE-PROCESSING (Part B) ===");
            System.out.println("File               : " + selectedFile.getName());
            System.out.println("Total tokens       : " + count);
            System.out.println("Whitespace removed : " + lexer.getWhitespaceRemoved());

            // Symbol table (Part E)
            lexer.getSymbolTable().display();

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }

        userInput.close();
    }
}
