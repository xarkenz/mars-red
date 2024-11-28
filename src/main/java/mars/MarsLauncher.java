package mars;

import mars.assembler.Assembler;
import mars.assembler.log.AssemblyError;
import mars.mips.dump.DumpFormat;
import mars.mips.dump.DumpFormatManager;
import mars.mips.hardware.*;
import mars.simulator.Simulator;
import mars.simulator.SimulatorException;
import mars.util.Binary;
import mars.util.FilenameFinder;
import mars.util.MemoryDump;
import mars.util.NativeUtilities;
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
    private boolean assembleFolder; // assemble only the given file or all files in its directory
    private boolean pseudo; // pseudo instructions allowed in source code or not.
    private boolean delayedBranching; // MIPS delayed branching is enabled.
    private boolean warningsAreErrors; // Whether assembler warnings should be considered errors.
    private boolean startAtMain; // Whether to start execution at statement labeled 'main'
    private boolean countInstructions; // Whether to count and report number of instructions executed
    private boolean selfModifyingCode; // Whether to allow self-modifying code (e.g. write to text segment)

    private int displayFormat;
    private List<String> registerDisplayList;
    private List<String> memoryDisplayList;
    private List<String> filenameList;
    private int maxSteps;
    private int instructionCount;
    private PrintStream out; // stream for display of command line output
    private List<String[]> dumpTriples = null; // each element holds 3 arguments for dump option
    private List<String> programArgumentList; // optional program args for MIPS program (becomes argc, argv)
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
            this.launchGUI();
        }
        else {
            // Running from command line
            // Assure command mode works in headless environment (generates exception if not)
            System.setProperty("java.awt.headless", "true");
            this.simulate = true;
            this.displayFormat = HEXADECIMAL;
            this.verbose = true;
            this.assembleFolder = false;
            this.pseudo = true;
            this.delayedBranching = false;
            this.warningsAreErrors = false;
            this.startAtMain = false;
            this.countInstructions = false;
            this.selfModifyingCode = false;
            this.instructionCount = 0;
            this.assembleErrorExitCode = 0;
            this.simulateErrorExitCode = 0;
            this.registerDisplayList = new ArrayList<>();
            this.memoryDisplayList = new ArrayList<>();
            this.filenameList = new ArrayList<>();
            MemoryConfigurations.setCurrentConfiguration(MemoryConfigurations.getDefaultConfiguration());
            this.maxSteps = -1;
            this.out = System.out;
            if (this.parseCommandArgs(args)) {
                if (this.runCommand()) {
                    this.displayMiscellaneousPostMortem();
                    this.displayRegistersPostMortem();
                    this.displayMemoryPostMortem();
                }
                this.dumpSegments();
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
            NativeUtilities.setApplicationName(Application.NAME);
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
                int highAddress = Memory.getInstance().getAddressOfFirstNullWord(segmentInfo[0], segmentInfo[1]) - Memory.BYTES_PER_WORD;
                if (highAddress < segmentInfo[0]) {
                    out.println("This segment has not been written to, there is nothing to dump.");
                    continue;
                }
                format.dumpMemoryRange(file, segmentInfo[0], highAddress);
            }
            catch (FileNotFoundException exception) {
                out.println("Error while attempting to save dump, file " + file + " was not found!");
            }
            catch (AddressErrorException exception) {
                out.println("Error while attempting to save dump, file " + file + "!  Could not access address: " + exception.getAddress() + "!");
            }
            catch (IOException exception) {
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
        this.programArgumentList = null;
        if (args.length == 0) {
            return true; // should not get here...
        }
        // If the option to display MARS messages to standard error is used,
        // it must be processed before any others (since messages may be
        // generated during option parsing).
        this.processDisplayMessagesToErrSwitch(args, displayMessagesToErrSwitch);
        this.displayCopyright(args, noCopyrightSwitch);  // ..or not..
        if (args.length == 1 && args[0].equals("h")) {
            this.displayHelp();
            return false;
        }
        for (int i = 0; i < args.length; i++) {
            // We have seen "pa" switch, so all remaining args are program args
            // that will become "argc" and "argv" for the MIPS program.
            if (inProgramArgumentList) {
                if (this.programArgumentList == null) {
                    this.programArgumentList = new ArrayList<>();
                }
                this.programArgumentList.add(args[i]);
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
                if (i + 3 >= args.length) {
                    this.out.println("Dump command line argument requires a segment, format and file name.");
                    argsOK = false;
                }
                else {
                    if (this.dumpTriples == null) {
                        this.dumpTriples = new ArrayList<>();
                    }
                    this.dumpTriples.add(new String[] { args[++i], args[++i], args[++i] });
                }
                continue;
            }
            if (args[i].equalsIgnoreCase("mc")) {
                String configName = args[++i];
                MemoryConfiguration config = MemoryConfigurations.getConfiguration(configName);
                if (config == null) {
                    this.out.println("Invalid memory configuration: " + configName);
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
                    this.assembleErrorExitCode = Binary.decodeInteger(s);
                    continue;
                }
                catch (NumberFormatException exception) {
                    // Let it fall thru and get handled by catch-all
                }
            }
            // Set MARS exit code for simulate error
            if (args[i].toLowerCase().indexOf("se") == 0) {
                String s = args[i].substring(2);
                try {
                    this.simulateErrorExitCode = Binary.decodeInteger(s);
                    continue;
                }
                catch (NumberFormatException exception) {
                    // Let it fall thru and get handled by catch-all
                }
            }
            if (args[i].equalsIgnoreCase("d")) {
                Application.debug = true;
                continue;
            }
            if (args[i].equalsIgnoreCase("a")) {
                this.simulate = false;
                continue;
            }
            if (args[i].equalsIgnoreCase("ad") || args[i].equalsIgnoreCase("da")) {
                Application.debug = true;
                this.simulate = false;
                continue;
            }
            if (args[i].equalsIgnoreCase("p")) {
                this.assembleFolder = true;
                continue;
            }
            if (args[i].equalsIgnoreCase("dec")) {
                this.displayFormat = DECIMAL;
                continue;
            }
            if (args[i].equalsIgnoreCase("hex")) {
                this.displayFormat = HEXADECIMAL;
                continue;
            }
            if (args[i].equalsIgnoreCase("ascii")) {
                this.displayFormat = ASCII;
                continue;
            }
            if (args[i].equalsIgnoreCase("b")) {
                this.verbose = false;
                continue;
            }
            if (args[i].equalsIgnoreCase("db")) {
                this.delayedBranching = true;
                continue;
            }
            if (args[i].equalsIgnoreCase("np") || args[i].equalsIgnoreCase("ne")) {
                this.pseudo = false;
                continue;
            }
            if (args[i].equalsIgnoreCase("we")) { // added 14-July-2008 DPS
                this.warningsAreErrors = true;
                continue;
            }
            if (args[i].equalsIgnoreCase("sm")) { // added 17-Dec-2009 DPS
                this.startAtMain = true;
                continue;
            }
            if (args[i].equalsIgnoreCase("smc")) { // added 5-Jul-2013 DPS
                this.selfModifyingCode = true;
                continue;
            }
            if (args[i].equalsIgnoreCase("ic")) { // added 19-Jul-2012 DPS
                this.countInstructions = true;
                continue;
            }

            if (args[i].indexOf("$") == 0) {
                if (Processor.getRegister(args[i]) == null && Coprocessor1.getRegister(args[i]) == null) {
                    this.out.println("Invalid Register Name: " + args[i]);
                }
                else {
                    this.registerDisplayList.add(args[i]);
                }
                continue;
            }
            // check for register name w/o $.  added 14-July-2008 DPS
            if (Processor.getRegister("$" + args[i]) != null || Coprocessor1.getRegister("$" + args[i]) != null) {
                this.registerDisplayList.add("$" + args[i]);
                continue;
            }
            if (new File(args[i]).exists()) {  // is it a file name?
                this.filenameList.add(args[i]);
                continue;
            }
            // Check for stand-alone integer, which is the max execution steps option
            try {
                this.maxSteps = Integer.decode(args[i]);
                continue;
            }
            catch (NumberFormatException ignored) {
                // Do nothing.  next statement will handle it
            }
            // Check for integer address range (m-n)
            try {
                String[] memoryRange = this.checkMemoryAddressRange(args[i]);
                this.memoryDisplayList.add(memoryRange[0]); // low end of range
                this.memoryDisplayList.add(memoryRange[1]); // high end of range
                continue;
            }
            catch (NumberFormatException nfe) {
                this.out.println("Invalid/unaligned address or invalid range: " + args[i]);
                argsOK = false;
                continue;
            }
            catch (NullPointerException ignored) {
                // Do nothing.  next statement will handle it
            }

            this.out.println("Invalid Command Argument: " + args[i]);
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
        if (this.filenameList.isEmpty()) {
            return false;
        }
        try {
            Application.getSettings().delayedBranchingEnabled.setNonPersistent(this.delayedBranching);
            Application.getSettings().selfModifyingCodeEnabled.setNonPersistent(this.selfModifyingCode);
            List<String> filesToAssemble;
            if (this.assembleFolder) {
                filesToAssemble = FilenameFinder.findFilenames(new File(this.filenameList.get(0)).getParent(), Application.FILE_EXTENSIONS);
                if (this.filenameList.size() > 1) {
                    // Using "p" project option PLUS listing more than one filename on command line.
                    // Add the additional files, avoiding duplicates.
                    this.filenameList.remove(0); // First one has already been processed
                    List<String> moreFilesToAssemble = FilenameFinder.findFilenames(this.filenameList, FilenameFinder.MATCH_ALL_EXTENSIONS);
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
                filesToAssemble = FilenameFinder.findFilenames(this.filenameList, FilenameFinder.MATCH_ALL_EXTENSIONS);
            }
            if (Application.debug) {
                this.out.println("--------  ASSEMBLY BEGINS  -----------");
            }
            Assembler assembler = new Assembler();
            assembler.getLog().setOutput(this.out::println);
            assembler.assembleFilenames(filesToAssemble);
            Processor.initializeProgramCounter(this.startAtMain); // DPS 3/9/09
            if (this.simulate) {
                // store program args (if any) in MIPS memory
                Simulator.getInstance().storeProgramArguments(this.programArgumentList);
                // establish observer if specified
                this.establishObserver();
                if (Application.debug) {
                    this.out.println("--------  SIMULATION BEGINS  -----------");
                }
                Simulator.getInstance().simulate(this.maxSteps, null);
                if (this.maxSteps > 0) {
                    this.out.println("\nProgram terminated after " + this.maxSteps + " steps.");
                }
            }
            if (Application.debug) {
                this.out.println("\n--------  ALL PROCESSING COMPLETE  -----------");
            }
        }
        catch (AssemblyError error) {
            Application.exitCode = this.assembleErrorExitCode;
            this.out.println("Processing terminated due to errors.");
            return false;
        }
        catch (SimulatorException exception) {
            Application.exitCode = this.simulateErrorExitCode;
            this.out.println(exception.getMessage());
            this.out.println("Processing terminated due to errors.");
        }
        return true;
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
            Memory.Listener instructionCounter = new Memory.Listener() {
                private int lastAddress = -1;

                @Override
                public void memoryRead(int address, int length, int value, int wordAddress, int wordValue) {
                    if (wordAddress != this.lastAddress) {
                        this.lastAddress = wordAddress;
                        MarsLauncher.this.instructionCount++;
                    }
                }
            };
            Memory.getInstance().addListener(
                instructionCounter,
                Memory.getInstance().getAddress(MemoryConfigurations.TEXT_LOW),
                Memory.getInstance().getAddress(MemoryConfigurations.TEXT_HIGH)
            );
            Memory.getInstance().addListener(
                instructionCounter,
                Memory.getInstance().getAddress(MemoryConfigurations.KERNEL_TEXT_LOW),
                Memory.getInstance().getAddress(MemoryConfigurations.KERNEL_TEXT_HIGH)
            );
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
            Register integerRegister = Processor.getRegister(regName);
            if (integerRegister != null) {
                // integer register
                if (verbose) {
                    out.print(regName + "\t");
                }
                out.println(formatIntForDisplay(integerRegister.getValue()));
            }
            else {
                // floating point register
                int intValue = Coprocessor1.getValue(Coprocessor1.getRegisterNumber(regName));
                float floatValue = Float.intBitsToFloat(intValue);
                long longValue;
                double doubleValue;
                boolean hasDouble;
                try {
                    longValue = Coprocessor1.getPairValue(Coprocessor1.getRegisterNumber(regName));
                    doubleValue = Double.longBitsToDouble(longValue);
                    hasDouble = true;
                }
                catch (InvalidRegisterAccessException ignored) {
                    longValue = 0;
                    doubleValue = Double.NaN;
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
        // Display requested memory range contents
        Iterator<String> memIter = memoryDisplayList.iterator();
        int addressStart = 0, addressEnd = 0;
        while (memIter.hasNext()) {
            try {
                addressStart = Binary.decodeInteger(memIter.next());
                addressEnd = Binary.decodeInteger(memIter.next());
            }
            catch (NumberFormatException exception) {
                // Should never happen; error would have been caught during command arg parse
            }
            int valuesDisplayed = 0;
            for (int addr = addressStart; addr <= addressEnd; addr += Memory.BYTES_PER_WORD) {
                if (addr < 0 && addressEnd > 0) {
                    break; // Happens only if addressEnd is 0x7ffffffc
                }
                if (valuesDisplayed % MEMORY_WORDS_PER_LINE == 0) {
                    out.print((valuesDisplayed > 0) ? "\n" : "");
                    if (verbose) {
                        out.print("Mem[" + Binary.intToHexString(addr) + "]\t");
                    }
                }
                try {
                    // Allow display of binary text segment (machine code) DPS 14-July-2008
                    int value;
                    if (Memory.getInstance().isInTextSegment(addr) || Memory.getInstance().isInKernelTextSegment(addr)) {
                        Integer valueOrNull = Memory.getInstance().fetchWordOrNull(addr);
                        value = (valueOrNull == null) ? 0 : valueOrNull;
                    }
                    else {
                        value = Memory.getInstance().fetchWord(addr, false);
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
                this.out = System.err;
                return;
            }
        }
    }

    /**
     * Decide whether copyright should be displayed, and display if so.
     */
    private void displayCopyright(String[] args, String noCopyrightSwitch) {
        if (Arrays.stream(args).noneMatch(arg -> arg.equalsIgnoreCase(noCopyrightSwitch))) {
            this.out.println(Application.NAME + " " + Application.VERSION + " - Copyright (c) " + Application.COPYRIGHT_YEARS + " " + Application.COPYRIGHT_HOLDERS + "\n");
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
   	
   	
