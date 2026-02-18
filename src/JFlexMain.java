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
        while (true) {

            System.out.print("\nEnter file number (1-" + kwFiles.length + ") or 0 to exit: ");

            int choice = -1;

            try {
                choice = Integer.parseInt(userInput.nextLine().trim());

                if (choice == 0) {
                    System.out.println("Exiting program...");
                    break;
                }

                if (choice < 1 || choice > kwFiles.length) {
                    System.out.println("Invalid choice.");
                    continue;
                }

            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
                continue;
            }

            File selectedFile = kwFiles[choice - 1];
            System.out.println("\nReading: " + selectedFile.getName());
            System.out.println("===========================================");

            try {
                FileReader fr    = new FileReader(selectedFile);
                Yylex      lexer = new Yylex(fr);
                Token      token;
                int        count = 0;

                System.out.println("\n=== TOKENS ===");
                while ((token = lexer.yylex()) != null) {
                    if (token.getType() != TokenType.WHITESPACE) {
                        System.out.println(token);
                        count++;
                    }
                }

                System.out.println("\n=== PRE-PROCESSING ===");
                System.out.println("File               : " + selectedFile.getName());
                System.out.println("Total tokens       : " + count);
                System.out.println("Whitespace removed : " + lexer.getWhitespaceRemoved());
                System.out.println("Lines processed    : " + (lexer.getLinesProcessed() + 1));

                lexer.getSymbolTable().display();
                lexer.getErrorHandler().displayErrors();
                lexer.getErrorHandler().displaySummary();

            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
        userInput.close();
    }
}