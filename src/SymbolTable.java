import java.util.*;

public class SymbolTable {

    private Map<String, SymbolEntry> table;

    public SymbolTable() {
        this.table = new LinkedHashMap<>();
    }

    // Insert new identifier or increment frequency if already exists
    public void insert(String name, int line, int column) {
        if (table.containsKey(name)) {
            table.get(name).incrementFrequency();
        } else {
            table.put(name, new SymbolEntry(name, line, column));
        }
    }

    // Print the symbol table
    public void display() {
        System.out.println("\n=== SYMBOL TABLE ===");
        System.out.printf("%-25s %-15s %-12s %-12s %-10s%n",
                "Identifier", "Type", "First Line", "First Col", "Frequency");
        System.out.println("-".repeat(75));
        for (SymbolEntry entry : table.values()) {
            System.out.println(entry);
        }
        System.out.println("-".repeat(75));
        System.out.println("Total unique identifiers: " + table.size());
    }

    // Inner class: one row in the symbol table
    public class SymbolEntry {
        private String name;
        private String type;
        private int    firstLine;
        private int    firstColumn;
        private int    frequency;

        public SymbolEntry(String name, int line, int column) {
            this.name        = name;
            this.type        = "IDENTIFIER";
            this.firstLine   = line;
            this.firstColumn = column;
            this.frequency   = 1;
        }

        public void incrementFrequency() { this.frequency++; }

        public String getName()     { return name;        }
        public String getType()     { return type;        }
        public int getFirstLine()   { return firstLine;   }
        public int getFirstColumn() { return firstColumn; }
        public int getFrequency()   { return frequency;   }

        @Override
        public String toString() {
            return String.format("%-25s %-15s %-12d %-12d %-10d",
                    name, type, firstLine, firstColumn, frequency);
        }
    }
}