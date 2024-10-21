package mars.assembler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
Copyright (c) 2003-2006,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
*/

/**
 * A table of Symbol objects.
 *
 * @author Jason Bumgarner, Jason Shrewsbury
 * @version June 2003
 */
public class SymbolTable {
    private final String filename;
    private final Map<String, Symbol> symbols;
    private final List<Symbol> alignableSymbols;

    /**
     * Create a new empty symbol table corresponding to the given filename.
     *
     * @param filename The name of the file this symbol table is associated with.  Will be
     *                 used only for output/display so it can be any descriptive string.
     */
    public SymbolTable(String filename) {
        this.filename = filename;
        this.symbols = new HashMap<>();
        this.alignableSymbols = new ArrayList<>();
    }

    public String getFilename() {
        return this.filename;
    }

    /**
     * Adds a Symbol object into the array of Symbols.
     *
     * @param identifier The identifier representing the Symbol.
     * @param address    The address of the Symbol.
     * @param isData     The type of Symbol, true for data, false for text.
     */
    public Symbol defineSymbol(String identifier, int address, boolean isData) {
        return this.defineSymbol(new Symbol(identifier, address, isData));
    }

    /**
     * Adds a Symbol object into the array of Symbols.
     *
     * @param symbol The symbol to add.
     */
    public Symbol defineSymbol(Symbol symbol) {
        // If address has changed since the last alignable symbol, this symbol marks the new alignable address
        if (!this.alignableSymbols.isEmpty() && symbol.getAddress() != this.alignableSymbols.get(0).getAddress()) {
            this.alignableSymbols.clear();
        }
        this.alignableSymbols.add(symbol);

        return this.symbols.put(symbol.getIdentifier(), symbol);
    }

    /**
     * Removes a symbol from the Symbol table.  If not found, it does nothing.
     * This will rarely happen (only when variable is declared .globl after already
     * being defined in the local symbol table).
     *
     * @param identifier The identifier for the symbol to remove.
     */
    public void removeSymbol(String identifier) {
        this.symbols.remove(identifier);
    }

    /**
     * Get the address associated with the given identifier.
     *
     * @param identifier The identifier to search for.
     * @return The memory address of the identifier given, or <code>null</code> if not found in symbol table.
     */
    public Integer getAddress(String identifier) {
        Symbol symbol = this.getSymbol(identifier);
        return (symbol == null) ? null : symbol.getAddress();
    }

    /**
     * Produce Symbol object from symbol table that corresponds to given String.
     *
     * @param identifier The identifier to search for.
     * @return Symbol object for requested target, null if not found in symbol table.
     */
    public Symbol getSymbol(String identifier) {
        return this.symbols.get(identifier);
    }

    /**
     * Get all symbols in the table which represent data.
     *
     * @return Array of data symbols.
     */
    public Symbol[] getDataSymbols() {
        return this.symbols.values()
            .stream()
            .filter(Symbol::isData)
            .toArray(Symbol[]::new); // Java arrays are weird
    }

    /**
     * Get all symbols in the table which represent text.
     *
     * @return Array of text symbols.
     */
    public Symbol[] getTextSymbols() {
        return this.symbols.values()
            .stream()
            .filter(Symbol::isText)
            .toArray(Symbol[]::new); // Java arrays are weird
    }

    /**
     * Get all symbols in the table.
     *
     * @return Array of symbols.
     */
    public Symbol[] getAllSymbols() {
        return this.symbols.values()
            .toArray(Symbol[]::new); // Java arrays are weird
    }

    /**
     * Get the count of entries currently in the table.
     *
     * @return Number of symbol table entries.
     */
    public int getSize() {
        return this.symbols.size();
    }

    /**
     * Clear all symbols from the table.
     */
    public void clear() {
        this.symbols.clear();
    }

    public void realignSymbols(int currentAddress, int alignedAddress) {
        if (!this.alignableSymbols.isEmpty() && currentAddress == this.alignableSymbols.get(0).getAddress()) {
            for (Symbol symbol : this.alignableSymbols) {
                symbol.setAddress(alignedAddress);
            }
        }
        else {
            this.alignableSymbols.clear();
        }
    }

    /**
     * Fetches the text segment label which, if declared global, indicates
     * the starting address for execution.
     *
     * @return String containing global label whose text segment address is starting address for program execution.
     */
    public static String getStartLabel() {
        // TODO: add setting
        return "main";
    }
}