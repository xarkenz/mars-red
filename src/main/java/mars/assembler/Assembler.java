package mars.assembler;

import mars.*;
import mars.assembler.log.AssemblerLog;
import mars.assembler.log.AssemblyError;
import mars.assembler.log.LogLevel;
import mars.assembler.log.SourceLocation;
import mars.assembler.syntax.StatementSyntax;
import mars.assembler.syntax.Syntax;
import mars.assembler.syntax.SyntaxParser;
import mars.assembler.token.*;
import mars.mips.hardware.*;
import mars.simulator.Simulator;
import mars.util.Binary;

import java.util.*;

/*
Copyright (c) 2003-2012,  Pete Sanderson and Kenneth Vollmar

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
 * An Assembler is capable of assembling a MIPS program. It has only one public
 * method, <code>assemble()</code>, which implements a two-pass assembler. It
 * translates MIPS source code into binary machine code.
 *
 * @author Pete Sanderson, August 2003
 */
public class Assembler {
    private final AssemblerLog log;
    private final List<String> sourceFilenames;
    private final List<SourceFile> tokenizedFiles;
    private final SortedMap<Integer, StatementSyntax> parsedStatements;
    private final SortedMap<Integer, Statement> resolvedStatements;
    private final SortedMap<Integer, BasicStatement> assembledStatements;

    private final SymbolTable globalSymbolTable;
    private final Map<String, SymbolTable> localSymbolTables;
    private SymbolTable localSymbolTable;
    private final Map<String, Token> localSymbolsToGlobalize;

    private final List<ForwardReferencePatch> currentFilePatches;
    private final List<ForwardReferencePatch> remainingPatches;

    public final Segment textSegment;
    public final Segment dataSegment;
    public final Segment kernelTextSegment;
    public final Segment kernelDataSegment;
    public final Segment externSegment;
    private Segment segment;

    // Default is to align data from directives on appropriate boundary (word, half, byte)
    // This can be turned off for remainder of current data segment with ".align 0"
    private boolean isAutoAlignmentEnabled;

    public Assembler() {
        this.log = new AssemblerLog();
        this.sourceFilenames = new ArrayList<>();
        this.tokenizedFiles = new ArrayList<>();
        this.parsedStatements = new TreeMap<>(Integer::compareUnsigned);
        this.resolvedStatements = new TreeMap<>(Integer::compareUnsigned);
        this.assembledStatements = new TreeMap<>(Integer::compareUnsigned);

        this.globalSymbolTable = new SymbolTable("(global)");
        this.localSymbolTables = new HashMap<>();
        this.localSymbolTable = null;
        this.localSymbolsToGlobalize = new HashMap<>();

        this.currentFilePatches = new ArrayList<>();
        this.remainingPatches = new ArrayList<>();

        this.dataSegment = new Segment(true);
        this.textSegment = new Segment(false);
        this.kernelDataSegment = new Segment(true);
        this.kernelTextSegment = new Segment(false);
        this.externSegment = new Segment(true);
        this.segment = this.textSegment;

        this.isAutoAlignmentEnabled = true;
    }

    public AssemblerLog getLog() {
        return this.log;
    }

    public void logInfo(SourceLocation location, String content) {
        this.log.logInfo(location, content);
    }

    public void logWarning(SourceLocation location, String content) {
        this.log.logWarning(location, content);
    }

    public void logCompatibilityWarning(SourceLocation location, String content) {
        if (Application.getSettings().compatibilityWarningsEnabled.get()) {
            this.logWarning(location, content);
        }
    }

    public void logError(SourceLocation location, String content) {
        this.log.logError(location, content);
    }

    public Symbol getSymbol(String identifier) {
        Symbol symbol = null;
        if (this.localSymbolTable != null) {
            symbol = this.localSymbolTable.getSymbol(identifier);
        }
        if (symbol == null) {
            symbol = this.globalSymbolTable.getSymbol(identifier);
        }
        return symbol;
    }

    public SymbolTable getLocalSymbolTable() {
        return this.localSymbolTable;
    }

    public SymbolTable getLocalSymbolTable(String filename) {
        return this.localSymbolTables.get(filename);
    }

    public SymbolTable getGlobalSymbolTable() {
        return this.globalSymbolTable;
    }

    public SymbolTable getSymbolTable(String filename) {
        return (filename == null) ? this.getGlobalSymbolTable() : this.getLocalSymbolTable(filename);
    }

    public Segment getSegment() {
        return this.segment;
    }

    public void setSegment(Segment segment) {
        this.segment = segment;
        this.setAutoAlignmentEnabled(true);
    }

    public boolean isAutoAlignmentEnabled() {
        return this.isAutoAlignmentEnabled;
    }

    public void setAutoAlignmentEnabled(boolean enabled) {
        this.isAutoAlignmentEnabled = enabled;
    }

    public List<String> getSourceFilenames() {
        return this.sourceFilenames;
    }

    public List<SourceFile> getTokenizedFiles() {
        return this.tokenizedFiles;
    }

    public SortedMap<Integer, StatementSyntax> getParsedStatements() {
        return this.parsedStatements;
    }

    public SortedMap<Integer, Statement> getResolvedStatements() {
        return this.resolvedStatements;
    }

    public SortedMap<Integer, BasicStatement> getAssembledStatements() {
        return this.assembledStatements;
    }

    public void resetExternalState() {
        Memory.getInstance().reset();
        Processor.reset();
        Coprocessor0.reset();
        Coprocessor1.reset();
    }

    public void reset() {
        this.log.clear();
        this.sourceFilenames.clear();
        this.tokenizedFiles.clear();
        this.parsedStatements.clear();
        this.resolvedStatements.clear();
        this.assembledStatements.clear();

        this.globalSymbolTable.clear();
        this.localSymbolTables.clear();
        this.localSymbolTable = null;
        this.localSymbolsToGlobalize.clear();

        this.currentFilePatches.clear();
        this.remainingPatches.clear();

        this.dataSegment.setRange(Memory.getInstance().getLayout().staticRange);
        this.dataSegment.resetAddress();
        this.textSegment.setRange(Memory.getInstance().getLayout().textRange);
        this.textSegment.resetAddress();
        this.kernelDataSegment.setRange(Memory.getInstance().getLayout().kernelDataRange);
        this.kernelDataSegment.resetAddress();
        this.kernelTextSegment.setRange(Memory.getInstance().getLayout().kernelTextRange);
        this.kernelTextSegment.resetAddress();
        this.externSegment.setRange(Memory.getInstance().getLayout().externRange);
        this.externSegment.resetAddress();
        this.segment = this.textSegment;
    }

    public void assembleFilenames(List<String> sourceFilenames) throws AssemblyError {
        this.reset();
        this.resetExternalState();

        StringBuilder startMessage = new StringBuilder("Assembler started with file(s):");
        for (String filename : sourceFilenames) {
            startMessage.append("\n- ").append(filename);

            this.sourceFilenames.add(filename);
        }
        this.log.logInfo(null, startMessage.toString());

        // Tokenize all files and add them to the list
        for (String filename : sourceFilenames) {
            this.log.logInfo(null, "Tokenizing '" + filename + "'...");

            this.tokenizedFiles.add(Tokenizer.tokenizeFile(filename, this.log));
        }

        // If the tokenizer produced any errors, throw them instead of progressing to parsing and assembly
        if (this.log.hasMessages(LogLevel.ERROR)) {
            throw new AssemblyError(this.log);
        }

        this.assembleFiles();
    }

    /**
     * Parse and generate machine code for the given MIPS program. All source
     * files must have already been tokenized.
     *
     * @param sourceFiles The list of source files which have already been tokenized.
     */
    public void assembleFiles(List<SourceFile> sourceFiles) throws AssemblyError {
        this.reset();
        this.resetExternalState();

        // Populate the list of source filenames
        StringBuilder startMessage = new StringBuilder("Assembler restarted with tokenized file(s):");
        for (SourceFile sourceFile : sourceFiles) {
            startMessage.append("\n- ").append(sourceFile.getFilename());
            this.sourceFilenames.add(sourceFile.getFilename());
        }
        this.log.logInfo(null, startMessage.toString());

        this.tokenizedFiles.addAll(sourceFiles);

        this.assembleFiles();
    }

    private void assembleFiles() throws AssemblyError {
        // TODO: We shouldn't have to do this here...
        Simulator.getInstance().getBackStepper().setEnabled(false);

        // FIRST PASS: Parse each file into its syntax components, processing directives and populating the symbol
        // tables as label definitions are encountered

        for (SourceFile sourceFile : this.tokenizedFiles) {
            this.log.logInfo(null, "Parsing '" + sourceFile.getFilename() + "'...");

            // Create a new local symbol table for the file
            this.localSymbolTable = new SymbolTable(sourceFile.getFilename());
            this.localSymbolTables.put(sourceFile.getFilename(), this.localSymbolTable);
            // Start in text segment by default
            this.segment = this.textSegment;

            SyntaxParser parser = new SyntaxParser(sourceFile.getLines().iterator(), this.log);
            Syntax syntax;
            while ((syntax = parser.parseNextSyntax()) != null && !this.log.hasExceededMaxErrorCount()) {
                syntax.process(this);
            }

            // Move symbols specified by .globl directives from the local symbol table to the global symbol table
            this.transferGlobals();

            // Attempt to resolve forward label references that were discovered in operand fields
            // of data segment directives in current file. Those that are not resolved after this
            // call are either references to global labels not seen yet, or are undefined.
            // Cannot determine which until all files are parsed, so copy unresolved entries
            // into accumulated list and clear out this one for reuse with the next source file.
            this.currentFilePatches.removeIf(patch -> (
                patch.resolve(this.localSymbolTable) || patch.resolve(this.globalSymbolTable)
            ));
            this.remainingPatches.addAll(this.currentFilePatches);
            this.currentFilePatches.clear();

            if (this.log.hasExceededMaxErrorCount()) {
                Simulator.getInstance().getBackStepper().setEnabled(true);
                throw new AssemblyError(this.log);
            }
        }

        // Have processed all source files. Attempt to resolve any remaining forward label
        // references from global symbol table. Those that remain unresolved are undefined
        // and require error message.
        this.remainingPatches.removeIf(patch -> patch.resolve(this.globalSymbolTable));
        for (ForwardReferencePatch patch : this.remainingPatches) {
            this.log.logError(
                patch.getSourceLocation(),
                "Undefined symbol '" + patch.identifier + "'"
            );

            if (this.log.hasExceededMaxErrorCount()) {
                Simulator.getInstance().getBackStepper().setEnabled(true);
                throw new AssemblyError(this.log);
            }
        }

        // If the first pass produced any errors, throw them instead of progressing to the second pass
        if (this.log.hasMessages(LogLevel.ERROR)) {
            Simulator.getInstance().getBackStepper().setEnabled(true);
            throw new AssemblyError(this.log);
        }

        // SECOND PASS: Resolve the remaining label operands, etc. against the symbol table and generate
        // basic/extended statements with fully resolved operand lists

        this.log.logInfo(null, "Resolving symbols...");

        String previousLineFilename = null;
        for (var entry : this.parsedStatements.entrySet()) {
            int address = entry.getKey();
            StatementSyntax syntax = entry.getValue();

            String lineFilename = syntax.getSourceLine().getLocation().getFilename();
            if (!lineFilename.equals(previousLineFilename)) {
                previousLineFilename = lineFilename;
                this.localSymbolTable = this.localSymbolTables.get(lineFilename);

                // Should always be able to access the local symbol table, but log a warning if it somehow fails
                if (this.localSymbolTable == null) {
                    this.log.logWarning(
                        syntax.getFirstToken().getLocation(),
                        "Failed to access local symbol table (this is a bug!)"
                    );
                }
            }

            this.resolvedStatements.put(address, syntax.resolve(this, address));

            if (this.log.hasExceededMaxErrorCount()) {
                Simulator.getInstance().getBackStepper().setEnabled(true);
                throw new AssemblyError(this.log);
            }
        }

        // If the second pass produced any errors, throw them instead of progressing to the third pass
        if (this.log.hasMessages(LogLevel.ERROR)) {
            Simulator.getInstance().getBackStepper().setEnabled(true);
            throw new AssemblyError(this.log);
        }

        // THIRD PASS: Write resolved statements to memory and store the resulting basic statements in the list
        // of assembled statements

        this.log.logInfo(null, "Generating code...");

        for (var entry : this.resolvedStatements.entrySet()) {
            int address = entry.getKey();
            Statement statement = entry.getValue();

            // This call will take care of adding the statement(s) to this.assembledStatements
            statement.handlePlacement(this, address);

            if (this.log.hasExceededMaxErrorCount()) {
                Simulator.getInstance().getBackStepper().setEnabled(true);
                throw new AssemblyError(this.log);
            }
        }

        // If the third pass produced any errors, throw them instead of returning normally
        // This also includes warnings now if they are being treated as errors
        if (this.log.hasMessages(LogLevel.ERROR) || (
            Application.getSettings().warningsAreErrors.get() && this.log.hasMessages(LogLevel.WARNING)
        )) {
            Simulator.getInstance().getBackStepper().setEnabled(true);
            throw new AssemblyError(this.log);
        }

        // Clean up and perform final steps
        this.localSymbolTable = null;
        this.segment = this.textSegment;

        Processor.initializeProgramCounter(Application.getSettings().startAtMain.get());
        Simulator.getInstance().getBackStepper().setEnabled(true);

        this.log.logInfo(null, "Assembling finished.");
    }

    public void addParsedStatement(StatementSyntax statement) {
        StatementSyntax replacedStatement = this.parsedStatements.put(this.segment.getAddress(), statement);
        if (replacedStatement != null) {
            this.log.logError(
                statement.getFirstToken().getLocation(),
                "Attempted to place the statement at address "
                    + Binary.intToHexString(this.segment.getAddress())
                    + ", but a statement was already placed there from "
                    + replacedStatement.getSourceLine().getLocation()
            );
        }

        // Increment the current address by the statement size
        this.segment.incrementAddress(statement.getInstruction().getSizeBytes());
    }

    public void placeStatement(BasicStatement statement, int address) {
        BasicStatement replacedStatement = this.assembledStatements.put(address, statement);
        if (replacedStatement != null) {
            this.log.logError(
                statement.getSyntax().getFirstToken().getLocation(),
                "Attempted to place the statement at address "
                    + Binary.intToHexString(this.segment.getAddress())
                    + ", but a statement was already placed there (" + replacedStatement + ")"
            );
        }

        try {
            Memory.getInstance().storeStatement(address, statement, true);
        }
        catch (AddressErrorException exception) {
            this.log.logError(
                statement.getSyntax().getFirstToken().getLocation(),
                "Cannot place statement at " + Binary.intToHexString(address) + ": " + exception.getMessage()
            );
        }
    }

    public void alignSegmentAddress(int alignment) {
        // No action needed for byte alignment
        if (alignment > 1) {
            int currentAddress = this.segment.getAddress();
            int alignedAddress = Memory.alignToNext(currentAddress, alignment);

            this.segment.setAddress(alignedAddress);
            this.localSymbolTable.realignSymbols(currentAddress, alignedAddress);
        }
    }

    public void createForwardReferencePatch(int address, int length, Token identifier) {
        this.currentFilePatches.add(new ForwardReferencePatch(address, length, identifier));
    }

    public void defineExtern(Token identifier, int sizeBytes) {
        // Only define a new extern if the identifier is not already in the global symbol table
        if (this.globalSymbolTable.getSymbol(identifier.getLiteral()) == null) {
            this.globalSymbolTable.defineSymbol(
                identifier.getLiteral(),
                this.externSegment.getAddress(),
                true
            );
            this.externSegment.incrementAddress(sizeBytes);
        }
    }

    public void makeSymbolGlobal(Token identifier) {
        // Check to ensure the identifier does not conflict with any existing global symbols
        Token previousIdentifier = this.localSymbolsToGlobalize.get(identifier.getLiteral());
        if (previousIdentifier != null) {
            this.log.logWarning(
                identifier.getLocation(),
                "Symbol '" + identifier + "' was previously declared as global on line "
                    + previousIdentifier.getLocation().getLineIndex()
            );
        }
        else {
            this.localSymbolsToGlobalize.put(identifier.getLiteral(), identifier);
        }
    }

    /**
     * Process the list of .globl labels, if any, declared and defined in this file.
     * We'll just move their symbol table entries from local symbol table to global
     * symbol table at the end of the first assembly pass.
     */
    private void transferGlobals() {
        for (Token identifier : this.localSymbolsToGlobalize.values()) {
            Symbol symbol = this.localSymbolTable.getSymbol(identifier.getLiteral());
            if (symbol == null) {
                this.log.logError(
                    identifier.getLocation(),
                    "Symbol '" + identifier + "' has not been defined in this file"
                );
            }
            else if (this.globalSymbolTable.getSymbol(identifier.getLiteral()) != null) {
                this.log.logError(
                    identifier.getLocation(),
                    "Symbol '" + identifier + "' was declared as global in another file"
                );
            }
            else {
                // Transfer the symbol from local to global
                this.localSymbolTable.removeSymbol(symbol.getIdentifier());
                this.globalSymbolTable.defineSymbol(symbol);
            }
        }

        // We have now transfered all local symbols that needed to be globalized
        this.localSymbolsToGlobalize.clear();
    }

    /**
     * Private class to simultaneously track addresses in both user and kernel address spaces.
     * Instantiate one for data segment and one for text segment.
     */
    public static class Segment {
        private final boolean isData;
        private int firstAddress;
        private int lastAddress;
        private int address;

        private Segment(boolean isData) {
            this.isData = isData;
            this.firstAddress = 0;
            this.lastAddress = 0;
            this.resetAddress();
        }

        public boolean isData() {
            return this.isData;
        }

        public int getFirstAddress() {
            return this.firstAddress;
        }

        public int getLastAddress() {
            return this.lastAddress;
        }

        public void setRange(MemoryLayout.Range range) {
            this.firstAddress = range.minAddress();
            this.lastAddress = range.maxAddress();
        }

        public int getAddress() {
            return this.address;
        }

        public void setAddress(int address) {
            this.address = address;
        }

        public void incrementAddress(int numBytes) {
            this.address += numBytes;
        }

        public void resetAddress() {
            this.address = this.firstAddress;
        }
    }

    /**
     * Handy class to handle forward label references appearing as data
     * segment operands. This is needed because the data segment is comletely
     * processed by the end of the first assembly pass, and its directives may
     * contain labels as operands. When this occurs, the label's associated
     * address becomes the operand value. If it is a forward reference, we will
     * save the necessary information in this object for finding and patching in
     * the correct address at the end of the first pass (for this file or for all
     * files if more than one).
     * If such a parsed label refers to a local or global label not defined yet,
     * pertinent information is added to this object:
     * - memory address that needs the label's address,
     * - number of bytes (addresses are 4 bytes but may be used with any of
     * the integer directives: .word, .half, .byte)
     * - the label's identifier. Normally need only the name but error message needs more.
     */
    private record ForwardReferencePatch(int address, int length, Token identifier) {
        public SourceLocation getSourceLocation() {
            return this.identifier.getLocation();
        }

        /**
         * Will traverse the list of forward references, attempting to resolve them.
         * For each entry it will first search the provided local symbol table and
         * failing that, the global one. If passed the global symbol table, it will
         * perform a second, redundant, search. If search is successful, the patch
         * is applied and the forward reference removed. If search is not successful,
         * the forward reference remains (it is either undefined or a global label
         * defined in a file not yet parsed).
         */
        public boolean resolve(SymbolTable symbolTable) {
            // Find the symbol, if it exists
            Symbol symbol = symbolTable.getSymbol(this.identifier.getLiteral());
            if (symbol == null) {
                return false;
            }

            this.patch(symbol.getAddress());
            return true;
        }

        private void patch(int value) {
            // Perform the patch operation
            try {
                Memory.getInstance().store(this.address, value, this.length, true);
            }
            catch (AddressErrorException ignored) {
                // Should not happen
            }
        }
    }
}
