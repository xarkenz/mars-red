package mars;

import mars.mips.dump.DumpFormat;
import mars.mips.dump.DumpFormatManager;
import mars.mips.hardware.*;
import mars.simulator.ProgramArgumentList;
import mars.util.Binary;
import mars.util.FilenameFinder;
import mars.util.MemoryDump;
import mars.venus.VenusUI;

import javax.swing.*;
import java.io.*;
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
 * Launcher for the MARS application, both command line and GUI.
 *
 * @author Pete Sanderson
 * @version December 2009
 */
public class MarsLauncher {
    private static final String RANGE_SEPARATOR = "-";
    private static final int MEMORY_WORDS_PER_LINE = 4; // display 4 memory words, tab separated, per line
    private static final int DECIMAL = 0; // memory and register display format
    private static final int HEXADECIMAL = 1; // memory and register display format
    private static final int ASCII = 2; // memory and register display format

    private boolean simulate;
    private boolean verbose; // display register name or address along with contents
    private boolean assembleProject; // assemble only the given file or all files in its directory
    private boolean pseudo; // pseudo instructions allowed in source code or not.
    private boolean delayedBranching; // MIPS delayed branching is enabled.
    private boolean warningsAreErrors; // Whether assembler warnings should be considered errors.
    private boolean startAtMain; // Whether to start execution at statement labeled 'main'
    private boolean countInstructions; // Whether to count and report number of instructions executed
    private boolean selfModifyingCode; // Whether to allow self-modifying code (e.g. write to text segment)

    private int displayFormat;
    private ArrayList<String> registerDisplayList;
    private ArrayList<String> memoryDisplayList;
    private ArrayList<String> filenameList;
    private Program code;
    private int maxSteps;
    private int instructionCount;
    private PrintStream out; // stream for display of command line output
    private ArrayList<String[]> dumpTriples = null; // each element holds 3 arguments for dump option
    private ArrayList<String> programArgumentList; // optional program args for MIPS program (becomes argc, argv)
    private int assembleErrorExitCode; // MARS command exit code to return if assemble error occurs
    private int simulateErrorExitCode; // MARS command exit code to return if simulation error occurs

    /**
     * Launch MARS Red as a standalone executable with the given command-line arguments. If no arguments are specified,
     * the application will launch in GUI mode.
     *
     * @param args Command line arguments.
     * @see MarsLauncher#MarsLauncher(String[])
     */
    public static void main(String[] args) {
        new MarsLauncher(args);
    }

    /**
     * Launch an instance of MARS Red with the given command-line arguments. If no arguments are specified,
     * the application will launch in GUI mode.
     * <p>
     * Usage: <code>Mars [options] filename</code>
     * <p>
     * Valid options (not case sensitive, separate by spaces) are:
     * <ul>
     * <li><code>a</code>  -- Assemble only, do not simulate.
     * <li><code>ae&lt;n&gt;</code>  -- Terminate MARS with integer exit code <i>n</i> if an assemble error occurs.
     * <li><code>ascii</code>  -- Display memory or register contents interpreted as ASCII.
     * <li><code>b</code>  -- Brief; do not display register/memory address along with contents.
     * <li><code>d</code>  -- Print debugging statements.
     * <li><code>da</code>, <code>ad</code>  -- Both <code>a</code> and <code>d</code>.
     * <li><code>db</code>  -- Enable delayed branching.
     * <li><code>dec</code>  -- Display memory or register contents in decimal.
     * <li><code>dump &lt;segment&gt; &lt;format&gt; &lt;file&gt;</code>  -- Dump memory contents to file.
     *     Supports an address range (see <code>&lt;m&gt;-&lt;n&gt;</code> below).  Current supported
     *     segments are <code>.text</code> and <code>.data</code>.  Current supported dump formats
     *     are <code>Binary</code>, <code>HexText</code>, <code>BinaryText</code>.
     * <li><code>h</code>  -- Display help.  Use by itself and with no filename.
     * <li><code>hex</code>  -- Display memory or register contents in hexadecimal (default).
     * <li><code>ic</code>  -- Display count of MIPS basic instructions 'executed'.
     * <li><code>mc &lt;config&gt;</code>  -- Set memory configuration, where <i>config</i> is <code>Default</code>
     *     for the MARS default 32-bit address space, <code>CompactDataAtZero</code> for
     *     a 32KB address space with data segment at address 0, or <code>CompactTextAtZero</code>
     *     for a 32KB address space with text segment at address 0.
     * <li><code>me</code>  -- Display MARS messages to standard error instead of standard output. Can separate via redirection.
     * <li><code>nc</code>  -- Do not display copyright notice (for cleaner redirected/piped output).
     * <li><code>np</code>, <code>ne</code>  -- No extended instructions (pseudo-instructions) allowed.
     * <li><code>p</code>  -- Project mode; assemble all files in the same directory as given file.
     * <li><code>se&lt;n&gt;</code>  -- Terminate MARS with integer exit code <i>n</i> if a simulation error occurs.
     * <li><code>sm</code>  -- Start execution at <code>main</code>. Execution will start at program statement globally labeled <code>main</code>.
     * <li><code>smc</code>  -- Allow self-modifying code. If enabled, the program can write and branch to either text or data segment.
     * <li><code>we</code>  -- Assembler warnings will be considered errors.
     * <li><code>&lt;n&gt;</code>  -- Set the step limit, where <i>n</i> is the maximum number of steps to simulate.
     *     If 0, negative or not specified, no step limit will be applied.
     * <li><code>&lt;reg&gt;</code>  -- Display register contents after simulation, where <i>reg</i> is the number or name
     *     (e.g. <code>$5</code>, <code>$t3</code>, <code>$f10</code>) of a register.  May be repeated to specify multiple registers.
     *     The <code>$</code> is not required, except for register numbers such as <code>$5</code>.
     * <li><code>&lt;m&gt;-&lt;n&gt;</code>  -- Display memory address range from <i>m</i> (inclusive)
     *     to <i>n</i> (exclusive) after simulation, where <i>m</i> and <i>n</i> may be hex or decimal,
     *     <i>m</i> &le; <i>n</i>, and both must lie on a word boundary.
     *     May be repeated to specify multiple memory address ranges.
     * <li><code>pa</code>  -- Specify program arguments separated by spaces.
     *     This option must be the last one specified since everything that follows it is interpreted as a
     *     program argument to be made available to the MIPS program at runtime.
     * </ul>
     *
     * @param args Command line arguments.
     */
    public MarsLauncher(String[] args) {
        Application.initialize();
        if (args.length == 0) {
            // Running graphical user interface
            launchGUI();
        }
        else {
            // Running from command line
            // Assure command mode works in headless environment (generates exception if not)
            System.setProperty("java.awt.headless", "true");
            simulate = true;
            displayFormat = HEXADECIMAL;
            verbose = true;
            assembleProject = false;
            pseudo = true;
            delayedBranching = false;
            warningsAreErrors = false;
            startAtMain = false;
            countInstructions = false;
            selfModifyingCode = false;
            instructionCount = 0;
            assembleErrorExitCode = 0;
            simulateErrorExitCode = 0;
            registerDisplayList = new ArrayList<>();
            memoryDisplayList = new ArrayList<>();
            filenameList = new ArrayList<>();
            MemoryConfigurations.setCurrentConfiguration(MemoryConfigurations.getDefaultConfiguration());
            // do NOT use Globals.program for command line MARS -- it triggers 'backstep' log.
            code = new Program();
            maxSteps = -1;
            out = System.out;
            if (parseCommandArgs(args)) {
                if (runCommand()) {
                    displayMiscellaneousPostMortem();
                    displayRegistersPostMortem();
                    displayMemoryPostMortem();
                }
                dumpSegments();
            }
            System.exit(Application.exitCode);
        }
    }

    /**
     * There are no command arguments, so run in interactive mode by
     * launching the GUI-fronted integrated development environment.
     */
    private void launchGUI() {
        SwingUtilities.invokeLater(() -> {
            Application.setupLookAndFeel();
            // Initialize the GUI
            new VenusUI(Application.getSettings(), Application.NAME + " " + Application.VERSION);
        });
    }

    /**
     * Perform any specified dump operations.  See "dump" option.
     */
    private void dumpSegments() {
        if (dumpTriples == null) {
            return;
        }

        for (String[] triple : dumpTriples) {
            File file = new File(triple[2]);
            Integer[] segmentInfo = MemoryDump.getSegmentBounds(triple[0]);
            // If not segment name, see if it is address range instead.  DPS 14-July-2008
            if (segmentInfo == null) {
                try {
                    String[] memoryRange = checkMemoryAddressRange(triple[0]);
                    segmentInfo = new Integer[2];
                    segmentInfo[0] = Binary.decodeInteger(memoryRange[0]); // Low end of range
                    segmentInfo[1] = Binary.decodeInteger(memoryRange[1]); // High end of range
                }
                catch (NumberFormatException | NullPointerException exception) {
                    segmentInfo = null;
                }
            }
            if (segmentInfo == null) {
                out.println("Error while attempting to save dump, segment/address-range " + triple[0] + " is invalid!");
                continue;
            }
            DumpFormat format = DumpFormatManager.getDumpFormat(triple[1]);
            if (format == null) {
                out.println("Error while attempting to save dump, format " + triple[1] + " was not found!");
                continue;
            }
            try {
                int highAddress = Application.memory.getAddressOfFirstNull(segmentInfo[0], segmentInfo[1]) - Memory.BYTES_PER_WORD;
                if (highAddress < segmentInfo[0]) {
                    out.println("This segment has not been written to, there is nothing to dump.");
                    continue;
                }
                format.dumpMemoryRange(file, segmentInfo[0], highAddress);
            }
            catch (FileNotFoundException e) {
                out.println("Error while attempting to save dump, file " + file + " was not found!");
            }
            catch (AddressErrorException e) {
                out.println("Error while attempting to save dump, file " + file + "!  Could not access address: " + e.getAddress() + "!");
            }
            catch (IOException e) {
                out.println("Error while attempting to save dump, file " + file + "!  Disk IO failed!");
            }
        }
    }

    /**
     * Parse command line arguments.  The initial parsing has already been
     * done, since each space-separated argument is already in a String array
     * element.  Here, we check for validity, set switch variables as appropriate
     * and build data structures.  For help option (h), display the help.
     *
     * @return true if command args parse OK, false otherwise.
     */
    private boolean parseCommandArgs(String[] args) {
        String noCopyrightSwitch = "nc";
        String displayMessagesToErrSwitch = "me";
        boolean argsOK = true;
        boolean inProgramArgumentList = false;
        programArgumentList = null;
        if (args.length == 0) {
            return true; // should not get here...
        }
        // If the option to display MARS messages to standard error is used,
        // it must be processed before any others (since messages may be
        // generated during option parsing).
        processDisplayMessagesToErrSwitch(args, displayMessagesToErrSwitch);
        displayCopyright(args, noCopyrightSwitch);  // ..or not..
        if (args.length == 1 && args[0].equals("h")) {
            displayHelp();
            return false;
        }
        for (int i = 0; i < args.length; i++) {
            // We have seen "pa" switch, so all remaining args are program args
            // that will become "argc" and "argv" for the MIPS program.
            if (inProgramArgumentList) {
                if (programArgumentList == null) {
                    programArgumentList = new ArrayList<>();
                }
                programArgumentList.add(args[i]);
                continue;
            }
            // Once we hit "pa", all remaining command args are assumed
            // to be program arguments.
            if (args[i].equalsIgnoreCase("pa")) {
                inProgramArgumentList = true;
                continue;
            }
            // messages-to-standard-error switch already processed, so ignore.
            if (args[i].toLowerCase().equals(displayMessagesToErrSwitch)) {
                continue;
            }
            // no-copyright switch already processed, so ignore.
            if (args[i].toLowerCase().equals(noCopyrightSwitch)) {
                continue;
            }
            if (args[i].equalsIgnoreCase("dump")) {
                if (args.length <= (i + 3)) {
                    out.println("Dump command line argument requires a segment, format and file name.");
                    argsOK = false;
                }
                else {
                    if (dumpTriples == null) {
                        dumpTriples = new ArrayList<>();
                    }
                    dumpTriples.add(new String[]{args[++i], args[++i], args[++i]});
                    //simulate = false;
                }
                continue;
            }
            if (args[i].equalsIgnoreCase("mc")) {
                String configName = args[++i];
                MemoryConfiguration config = MemoryConfigurations.getConfiguration(configName);
                if (config == null) {
                    out.println("Invalid memory configuration: " + configName);
                    argsOK = false;
                }
                else {
                    MemoryConfigurations.setCurrentConfiguration(config);
                }
                continue;
            }
            // Set MARS exit code for assemble error
            if (args[i].toLowerCase().indexOf("ae") == 0) {
                String s = args[i].substring(2);
                try {
                    assembleErrorExitCode = Integer.decode(s);
                    continue;
                }
                catch (NumberFormatException nfe) {
                    // Let it fall thru and get handled by catch-all
                }
            }
            // Set MARS exit code for simulate error
            if (args[i].toLowerCase().indexOf("se") == 0) {
                String s = args[i].substring(2);
                try {
                    simulateErrorExitCode = Integer.decode(s);
                    continue;
                }
                catch (NumberFormatException nfe) {
                    // Let it fall thru and get handled by catch-all
                }
            }
            if (args[i].equalsIgnoreCase("d")) {
                Application.debug = true;
                continue;
            }
            if (args[i].equalsIgnoreCase("a")) {
                simulate = false;
                continue;
            }
            if (args[i].equalsIgnoreCase("ad") || args[i].equalsIgnoreCase("da")) {
                Application.debug = true;
                simulate = false;
                continue;
            }
            if (args[i].equalsIgnoreCase("p")) {
                assembleProject = true;
                continue;
            }
            if (args[i].equalsIgnoreCase("dec")) {
                displayFormat = DECIMAL;
                continue;
            }
            if (args[i].equalsIgnoreCase("hex")) {
                displayFormat = HEXADECIMAL;
                continue;
            }
            if (args[i].equalsIgnoreCase("ascii")) {
                displayFormat = ASCII;
                continue;
            }
            if (args[i].equalsIgnoreCase("b")) {
                verbose = false;
                continue;
            }
            if (args[i].equalsIgnoreCase("db")) {
                delayedBranching = true;
                continue;
            }
            if (args[i].equalsIgnoreCase("np") || args[i].equalsIgnoreCase("ne")) {
                pseudo = false;
                continue;
            }
            if (args[i].equalsIgnoreCase("we")) { // added 14-July-2008 DPS
                warningsAreErrors = true;
                continue;
            }
            if (args[i].equalsIgnoreCase("sm")) { // added 17-Dec-2009 DPS
                startAtMain = true;
                continue;
            }
            if (args[i].equalsIgnoreCase("smc")) { // added 5-Jul-2013 DPS
                selfModifyingCode = true;
                continue;
            }
            if (args[i].equalsIgnoreCase("ic")) { // added 19-Jul-2012 DPS
                countInstructions = true;
                continue;
            }

            if (args[i].indexOf("$") == 0) {
                if (RegisterFile.getUserRegister(args[i]) == null && Coprocessor1.getRegister(args[i]) == null) {
                    out.println("Invalid Register Name: " + args[i]);
                }
                else {
                    registerDisplayList.add(args[i]);
                }
                continue;
            }
            // check for register name w/o $.  added 14-July-2008 DPS
            if (RegisterFile.getUserRegister("$" + args[i]) != null || Coprocessor1.getRegister("$" + args[i]) != null) {
                registerDisplayList.add("$" + args[i]);
                continue;
            }
            if (new File(args[i]).exists()) {  // is it a file name?
                filenameList.add(args[i]);
                continue;
            }
            // Check for stand-alone integer, which is the max execution steps option
            try {
                maxSteps = Integer.decode(args[i]);
                continue;
            }
            catch (NumberFormatException ignored) {
                // Do nothing.  next statement will handle it
            }
            // Check for integer address range (m-n)
            try {
                String[] memoryRange = checkMemoryAddressRange(args[i]);
                memoryDisplayList.add(memoryRange[0]); // low end of range
                memoryDisplayList.add(memoryRange[1]); // high end of range
                continue;
            }
            catch (NumberFormatException nfe) {
                out.println("Invalid/unaligned address or invalid range: " + args[i]);
                argsOK = false;
                continue;
            }
            catch (NullPointerException ignored) {
                // Do nothing.  next statement will handle it
            }
            out.println("Invalid Command Argument: " + args[i]);
            argsOK = false;
        }
        return argsOK;
    }

    /**
     * Carry out the mars command: assemble then optionally run.
     *
     * @return true if a simulation (run) occurs, false otherwise.
     */
    private boolean runCommand() {
        if (filenameList.isEmpty()) {
            return false;
        }
        boolean programRan = false;
        try {
            Application.getSettings().delayedBranchingEnabled.setNonPersistent(delayedBranching);
            Application.getSettings().selfModifyingCodeEnabled.setNonPersistent(selfModifyingCode);
            File mainFile = new File(filenameList.get(0)).getAbsoluteFile(); // First file is "main" file
            List<String> filesToAssemble;
            if (assembleProject) {
                filesToAssemble = FilenameFinder.findFilenames(mainFile.getParent(), Application.FILE_EXTENSIONS);
                if (filenameList.size() > 1) {
                    // Using "p" project option PLUS listing more than one filename on command line.
                    // Add the additional files, avoiding duplicates.
                    filenameList.remove(0); // First one has already been processed
                    List<String> moreFilesToAssemble = FilenameFinder.findFilenames(filenameList, FilenameFinder.MATCH_ALL_EXTENSIONS);
                    // Remove any duplicates then merge the two lists.
                    for (int index = 0; index < moreFilesToAssemble.size(); index++) {
                        for (String fileToAssemble : filesToAssemble) {
                            if (fileToAssemble.equals(moreFilesToAssemble.get(index))) {
                                moreFilesToAssemble.remove(index);
                                // Adjust for left shift in moreFilesToAssemble
                                index--;
                                // Break out of inner loop
                                break;
                            }
                        }
                    }
                    filesToAssemble.addAll(moreFilesToAssemble);
                }
            }
            else {
                filesToAssemble = FilenameFinder.findFilenames(filenameList, FilenameFinder.MATCH_ALL_EXTENSIONS);
            }
            if (Application.debug) {
                out.println("--------  TOKENIZING BEGINS  -----------");
            }
            List<Program> programsToAssemble = code.prepareFilesForAssembly(filesToAssemble, mainFile.getAbsolutePath(), null);
            if (Application.debug) {
                out.println("--------  ASSEMBLY BEGINS  -----------");
            }
            // Added logic to check for warnings and print if any. DPS 11/28/06
            ErrorList warnings = code.assemble(programsToAssemble, pseudo, warningsAreErrors);
            if (warnings != null && warnings.warningsOccurred()) {
                out.println(warnings.generateWarningReport());
            }
            RegisterFile.initializeProgramCounter(startAtMain); // DPS 3/9/09
            if (simulate) {
                // store program args (if any) in MIPS memory
                new ProgramArgumentList(programArgumentList).storeProgramArguments();
                // establish observer if specified
                establishObserver();
                if (Application.debug) {
                    out.println("--------  SIMULATION BEGINS  -----------");
                }
                programRan = true;
                code.simulate(maxSteps);
                if (maxSteps > 0) {
                    out.println("\nProgram terminated after " + maxSteps + " steps.");
                }
            }
            if (Application.debug) {
                out.println("\n--------  ALL PROCESSING COMPLETE  -----------");
            }
        }
        catch (ProcessingException exception) {
            Application.exitCode = (programRan) ? simulateErrorExitCode : assembleErrorExitCode;
            out.println(exception.getErrors().generateErrorAndWarningReport());
            out.println("Processing terminated due to errors.");
        }
        return programRan;
    }

    /**
     * Check for memory address subrange.  Has to be two integers separated
     * by "-"; no embedded spaces.  e.g. 0x00400000-0x00400010
     * If number is not multiple of 4, will be rounded up to next higher.
     */
    private String[] checkMemoryAddressRange(String arg) throws NumberFormatException {
        String[] memoryRange = null;
        if (arg.indexOf(RANGE_SEPARATOR) > 0 && arg.indexOf(RANGE_SEPARATOR) < arg.length() - 1) {
            // assume correct format, two numbers separated by -, no embedded spaces.
            // If that doesn't work it is invalid.
            memoryRange = new String[2];
            memoryRange[0] = arg.substring(0, arg.indexOf(RANGE_SEPARATOR));
            memoryRange[1] = arg.substring(arg.indexOf(RANGE_SEPARATOR) + 1);
            // NOTE: I will use homegrown decoder, because Integer.decode will throw
            // exception on address higher than 0x7FFFFFFF (e.g. sign bit is 1).
            if (Binary.decodeInteger(memoryRange[0]) > Binary.decodeInteger(memoryRange[1]) || !Memory.isWordAligned(Binary.decodeInteger(memoryRange[0])) || !Memory.isWordAligned(Binary.decodeInteger(memoryRange[1]))) {
                throw new NumberFormatException();
            }
        }
        return memoryRange;
    }

    /**
     * Required for counting instructions executed, if that option is specified.
     * DPS 19 July 2012
     */
    private void establishObserver() {
        if (countInstructions) {
            Memory.getInstance().addListener(new Memory.Listener() {
                private int lastAddress = -1;

                @Override
                public void memoryRead(int address, int length, int value, int wordAddress, int wordValue) {
                    if (wordAddress != this.lastAddress) {
                        this.lastAddress = wordAddress;
                        MarsLauncher.this.instructionCount++;
                    }
                }
            }, Memory.textBaseAddress, Memory.textLimitAddress - 1);
        }
    }

    /**
     * Displays any specified runtime properties. Initially just instruction count.
     * DPS 19 July 2012
     */
    private void displayMiscellaneousPostMortem() {
        if (countInstructions) {
            out.println("\n" + instructionCount);
        }
    }

    /**
     * Displays requested register or registers.
     */
    private void displayRegistersPostMortem() {
        // Display requested register contents
        out.println();
        for (String regName : registerDisplayList) {
            Register integerRegister = RegisterFile.getUserRegister(regName);
            if (integerRegister != null) {
                // integer register
                if (verbose) {
                    out.print(regName + "\t");
                }
                out.println(formatIntForDisplay(integerRegister.getValue()));
            }
            else {
                // floating point register
                float floatValue = Coprocessor1.getFloatFromRegister(regName);
                int intValue = Coprocessor1.getIntFromRegister(regName);
                double doubleValue;
                long longValue;
                boolean hasDouble;
                try {
                    doubleValue = Coprocessor1.getDoubleFromRegisterPair(regName);
                    longValue = Coprocessor1.getLongFromRegisterPair(regName);
                    hasDouble = true;
                }
                catch (InvalidRegisterAccessException ignored) {
                    doubleValue = Double.NaN;
                    longValue = 0;
                    hasDouble = false;
                }
                if (verbose) {
                    out.print(regName + "\t");
                }
                if (displayFormat == HEXADECIMAL) {
                    // display float (and double, if applicable) in hex
                    out.print(Binary.intToHexString(intValue));
                    if (hasDouble) {
                        out.println("\t" + Binary.longToHexString(longValue));
                    }
                    else {
                        out.println();
                    }
                }
                else if (displayFormat == DECIMAL) {
                    // display float (and double, if applicable) in decimal
                    out.print(floatValue);
                    if (hasDouble) {
                        out.println("\t" + doubleValue);
                    }
                    else {
                        out.println();
                    }
                }
                else { // displayFormat == ASCII
                    out.print(Binary.intToAscii(intValue));
                    if (hasDouble) {
                        out.println("\t" + Binary.intToAscii(Binary.highOrderLongToInt(longValue)) + Binary.intToAscii(Binary.lowOrderLongToInt(longValue)));
                    }
                    else {
                        out.println();
                    }
                }
            }
        }
    }

    /**
     * Formats int value for display: decimal, hex, ascii
     */
    private String formatIntForDisplay(int value) {
        return switch (displayFormat) {
            case DECIMAL -> Integer.toString(value);
            case HEXADECIMAL -> Binary.intToHexString(value);
            case ASCII -> Binary.intToAscii(value);
            default -> Binary.intToHexString(value);
        };
    }

    /**
     * Displays requested memory range or ranges
     */
    private void displayMemoryPostMortem() {
        int value;
        // Display requested memory range contents
        Iterator<String> memIter = memoryDisplayList.iterator();
        int addressStart = 0, addressEnd = 0;
        while (memIter.hasNext()) {
            try { // This will succeed; error would have been caught during command arg parse
                addressStart = Binary.decodeInteger(memIter.next());
                addressEnd = Binary.decodeInteger(memIter.next());
            }
            catch (NumberFormatException ignored) {
            }
            int valuesDisplayed = 0;
            for (int addr = addressStart; addr <= addressEnd; addr += Memory.BYTES_PER_WORD) {
                if (addr < 0 && addressEnd > 0) {
                    break;  // happens only if addressEnd is 0x7ffffffc
                }
                if (valuesDisplayed % MEMORY_WORDS_PER_LINE == 0) {
                    out.print((valuesDisplayed > 0) ? "\n" : "");
                    if (verbose) {
                        out.print("Mem[" + Binary.intToHexString(addr) + "]\t");
                    }
                }
                try {
                    // Allow display of binary text segment (machine code) DPS 14-July-2008
                    if (Memory.isInTextSegment(addr) || Memory.isInKernelTextSegment(addr)) {
                        Integer iValue = Application.memory.getRawWordOrNull(addr);
                        value = (iValue == null) ? 0 : iValue;
                    }
                    else {
                        value = Application.memory.getWord(addr);
                    }
                    out.print(formatIntForDisplay(value) + "\t");
                }
                catch (AddressErrorException aee) {
                    out.print("Invalid address: " + addr + "\t");
                }
                valuesDisplayed++;
            }
            out.println();
        }
    }

    /**
     * If option to display MARS messages to standard err (System.err) is
     * present, it must be processed before all others.  Since messages may
     * be output as early as during the command parse.
     */
    private void processDisplayMessagesToErrSwitch(String[] args, String displayMessagesToErrSwitch) {
        for (String arg : args) {
            if (arg.toLowerCase().equals(displayMessagesToErrSwitch)) {
                out = System.err;
                return;
            }
        }
    }

    /**
     * Decide whether copyright should be displayed, and display if so.
     */
    private void displayCopyright(String[] args, String noCopyrightSwitch) {
        if (Arrays.stream(args).noneMatch(arg -> arg.equalsIgnoreCase(noCopyrightSwitch))) {
            out.println(Application.NAME + " " + Application.VERSION + " - Copyright (c) " + Application.COPYRIGHT_YEARS + " " + Application.COPYRIGHT_HOLDERS + "\n");
        }
    }

    /**
     * Display command line help text.
     */
    private void displayHelp() {
        String segments = String.join(", ", List.of(MemoryDump.getSegmentNames()));
        String formats = String.join(", ", Arrays.stream(DumpFormatManager.getDumpFormats())
            .map(DumpFormat::getCommandDescriptor)
            .toList());
        out.println("Usage:  Mars  [options] filename [additional filenames]");
        out.println("  Valid options (not case sensitive, separate by spaces) are:");
        out.println("      a  -- assemble only, do not simulate");
        out.println("  ae<n>  -- terminate MARS with integer exit code <n> if an assemble error occurs.");
        out.println("  ascii  -- display memory or register contents interpreted as ASCII codes.");
        out.println("      b  -- brief - do not display register/memory address along with contents");
        out.println("      d  -- display MARS debugging statements");
        out.println("     db  -- MIPS delayed branching is enabled");
        out.println("    dec  -- display memory or register contents in decimal.");
        out.println("   dump <segment> <format> <file> -- memory dump of specified memory segment");
        out.println("            in specified format to specified file.  Option may be repeated.");
        out.println("            Dump occurs at the end of simulation unless 'a' option is used.");
        out.println("            Segment and format are case-sensitive and possible values are:");
        out.println("            <segment> = " + segments);
        out.println("            <format> = " + formats);
        out.println("      h  -- display this help.  Use by itself with no filename.");
        out.println("    hex  -- display memory or register contents in hexadecimal (default)");
        out.println("     ic  -- display count of MIPS basic instructions 'executed'");
        out.println("     mc <config>  -- set memory configuration.  Argument <config> is");
        out.println("            case-sensitive and possible values are: Default for the default");
        out.println("            32-bit address space, CompactDataAtZero for a 32KB memory with");
        out.println("            data segment at address 0, or CompactTextAtZero for a 32KB");
        out.println("            memory with text segment at address 0.");
        out.println("     me  -- display MARS messages to standard err instead of standard out. ");
        out.println("            Can separate messages from program output using redirection");
        out.println("     nc  -- do not display copyright notice (for cleaner redirected/piped output).");
        out.println("     np  -- use of pseudo instructions and formats not permitted");
        out.println("      p  -- Project mode - assemble all files in the same directory as given file.");
        out.println("  se<n>  -- terminate MARS with integer exit code <n> if a simulation (run) error occurs.");
        out.println("     sm  -- start execution at statement with global label main, if defined");
        out.println("    smc  -- Self Modifying Code - Program can write and branch to either text or data segment");
        out.println("    <n>  -- where <n> is an integer maximum count of steps to simulate.");
        out.println("            If 0, negative or not specified, there is no maximum.");
        out.println(" $<reg>  -- where <reg> is number or name (e.g. 5, t3, f10) of register whose ");
        out.println("            content to display at end of run.  Option may be repeated.");
        out.println("<reg_name>  -- where <reg_name> is name (e.g. t3, f10) of register whose");
        out.println("            content to display at end of run.  Option may be repeated. ");
        out.println("            The $ is not required.");
        out.println("<m>-<n>  -- memory address range from <m> to <n> whose contents to");
        out.println("            display at end of run. <m> and <n> may be hex or decimal,");
        out.println("            must be on word boundary, <m> <= <n>.  Option may be repeated.");
        out.println("     pa  -- Program Arguments follow in a space-separated list.  This");
        out.println("            option must be placed AFTER ALL FILE NAMES, because everything");
        out.println("            that follows it is interpreted as a program argument to be");
        out.println("            made available to the MIPS program at runtime.");
        out.println("If more than one filename is listed, the first is assumed to be the main");
        out.println("unless the global statement label 'main' is defined in one of the files.");
        out.println("Exception handler not automatically assembled.  Add it to the file list.");
        out.println("Options used here do not affect MARS Settings menu values and vice versa.");
    }
}
   	
   	
