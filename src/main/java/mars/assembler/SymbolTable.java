package mars.assembler;

import mars.ErrorList;
import mars.ErrorMessage;
import mars.Application;

import java.util.HashMap;

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
    /*
     * This class was originally implemented with an ArrayList and linear searching,
     * which can be highly inefficient, so I took it upon myself to reimplement it with
     * a HashMap instead. That should make this class more friendly to larger programs.
     * Sean Clarke 03/2024
     */

    // Note -1 is legal 32 bit address (0xFFFFFFFF) but it is the high address in
    // kernel address space so highly unlikely that any symbol will have this as
    // its associated address!
    public static final int NOT_FOUND = -1;
    private static final String START_LABEL = "main";

    private final String filename;
    private final HashMap<String, Symbol> table;

    /**
     * Create a new empty symbol table for given file.
     *
     * @param filename name of file this symbol table is associated with.  Will be
     *                 used only for output/display so it can be any descriptive string.
     */
    public SymbolTable(String filename) {
        this.filename = filename;
        this.table = new HashMap<>();
    }

    /**
     * Adds a Symbol object into the array of Symbols.
     *
     * @param token   The token representing the Symbol.
     * @param address The address of the Symbol.
     * @param isData  The type of Symbol, true for data, false for text.
     * @param errors  List to which to add any processing errors that occur.
     */
    public void addSymbol(Token token, int address, boolean isData, ErrorList errors) {
        String label = token.getValue();
        Symbol previousSymbol = table.put(label, new Symbol(label, address, isData));
        if (previousSymbol != null) {
            errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "label \"" + label + "\" already defined"));
        }
        if (Application.debug) {
            System.out.println("The symbol " + label + " with address " + address + " has been added to the " + this.filename + " symbol table.");
        }
    }

    /**
     * Removes a symbol from the Symbol table.  If not found, it does nothing.
     * This will rarely happen (only when variable is declared .globl after already
     * being defined in the local symbol table).
     *
     * @param label The label for the symbol to remove.
     */
    public void removeSymbol(String label) {
        Symbol removedSymbol = table.remove(label);
        if (Application.debug && removedSymbol != null) {
            System.out.println("The symbol " + label + " has been removed from the " + this.filename + " symbol table.");
        }
    }

    /**
     * Method to return the address associated with the given label.
     *
     * @param label The label to search for.
     * @return The memory address of the label given, or NOT_FOUND if not found in symbol table.
     */
    public int getAddress(String label) {
        Symbol symbol = getSymbol(label);
        return (symbol == null) ? NOT_FOUND : symbol.getAddress();
    }

    /**
     * Method to return the address associated with the given label.  Look first
     * in this (local) symbol table then in symbol table of labels declared
     * global (.globl directive).
     *
     * @param s The label.
     * @return The memory address of the label given, or NOT_FOUND if not found in symbol table.
     */
    public int getAddressLocalOrGlobal(String s) {
        int address = this.getAddress(s);
        return (address == NOT_FOUND) ? Application.globalSymbolTable.getAddress(s) : address;
    }

    /**
     * Produce Symbol object from symbol table that corresponds to given String.
     *
     * @param label The label to search for.
     * @return Symbol object for requested target, null if not found in symbol table.
     */
    public Symbol getSymbol(String label) {
        return table.get(label);
    }

    /**
     * Produce Symbol object from symbol table that has the given address.
     *
     * @param address Address of symbol to find.
     * @return Symbol object having requested address, null if address not found in symbol table.
     */
    // TODO: I'm not really a fan of this... but without a lot of restructuring it's hard to avoid. -Sean Clarke
    public Symbol getSymbolGivenAddress(int address) {
        return table.values().stream()
            .filter(symbol -> symbol.getAddress() == address)
            .findFirst()
            .orElse(null);
    }

    /**
     * Produce Symbol object from either local or global symbol table that has the
     * given address.
     *
     * @param address Address of symbol to find.
     * @return Symbol object having requested address, null if address not found in symbol table.
     */
    public Symbol getSymbolGivenAddressLocalOrGlobal(int address) {
        Symbol symbol = this.getSymbolGivenAddress(address);
        return (symbol == null) ? Application.globalSymbolTable.getSymbolGivenAddress(address) : symbol;
    }

    /**
     * Get all symbols in the table which represent data.
     *
     * @return Array of data symbols.
     */
    public Symbol[] getDataSymbols() {
        return table.values().stream()
            .filter(Symbol::isData)
            .toArray(Symbol[]::new); // Java arrays are weird
    }

    /**
     * Get all symbols in the table which represent text.
     *
     * @return Array of text symbols.
     */
    public Symbol[] getTextSymbols() {
        return table.values().stream()
            .filter(Symbol::isText)
            .toArray(Symbol[]::new); // Java arrays are weird
    }

    /**
     * Get all symbols in the table.
     *
     * @return Array of symbols.
     */
    public Symbol[] getAllSymbols() {
        return table.values()
            .toArray(Symbol[]::new); // Java arrays are weird
    }

    /**
     * Get the count of entries currently in the table.
     *
     * @return Number of symbol table entries.
     */
    public int getSize() {
        return table.size();
    }

    /**
     * Clear all symbols from the table.
     */
    public void clear() {
        table.clear();
    }

    /**
     * Fix address in symbol table entry.  Any and all entries that match the original
     * address will be modified to contain the replacement address. There is no effect
     * if none of the addresses match.
     *
     * @param originalAddress    Address associated with 0 or more symbol table entries.
     * @param replacementAddress Any entry that has originalAddress will have its
     *                           address updated to this value.  Does nothing if none do.
     */
    public void fixSymbolTableAddress(int originalAddress, int replacementAddress) {
        Symbol label = getSymbolGivenAddress(originalAddress);
        while (label != null) {
            label.setAddress(replacementAddress);
            label = getSymbolGivenAddress(originalAddress);
        }
    }

    /**
     * Fetches the text segment label which, if declared global, indicates
     * the starting address for execution.
     *
     * @return String containing global label whose text segment address is starting address for program execution.
     */
    public static String getStartLabel() {
        return START_LABEL;
    }
}