package mars.simulator;

import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryConfigurations;
import mars.mips.hardware.RegisterFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
	
/*
Copyright (c) 2003-2008,  Pete Sanderson and Kenneth Vollmar

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
 * Models Program Arguments, one or more strings provided to the MIPS
 * program at runtime. Equivalent to C's main(int argc, char **argv) or
 * Java's main(String[] args).
 *
 * @author Pete Sanderson
 * @version July 2008
 */
public class ProgramArgumentList {
    List<String> programArgumentList;

    /**
     * Constructor that parses string to produce list.  Delimiters
     * are the default {@link StringTokenizer} delimiters (space, tab,
     * newline, return, form feed)
     *
     * @param args String containing delimiter-separated arguments
     */
    public ProgramArgumentList(String args) {
        this(args.split("\\s+"));
    }

    /**
     * Constructor that gets list from String array, one argument per element.
     *
     * @param args Array of String, each element containing one argument
     */
    public ProgramArgumentList(String[] args) {
        this(args, 0);
    }

    /**
     * Constructor that gets list from section of String array, one
     * argument per element.
     *
     * @param args          Array of String, each element containing one argument
     * @param startPosition Index of array element containing the first argument; all remaining
     *                      elements are assumed to contain an argument.
     */
    public ProgramArgumentList(String[] args, int startPosition) {
        this.programArgumentList = new ArrayList<>(args.length - startPosition);
        this.programArgumentList.addAll(Arrays.asList(args).subList(startPosition, args.length));
    }

    /**
     * Constructor that gets list from ArrayList of String, one argument per element.
     *
     * @param args ArrayList of String, each element containing one argument
     */
    public ProgramArgumentList(ArrayList<String> args) {
        this(args, 0);
    }

    /**
     * Constructor that gets list from section of String ArrayList, one
     * argument per element.
     *
     * @param args          ArrayList of String, each element containing one argument
     * @param startPosition Index of array element containing the first argument; all remaining
     *                      elements are assumed to contain an argument.
     */
    public ProgramArgumentList(ArrayList<String> args, int startPosition) {
        if (args == null || args.size() < startPosition) {
            this.programArgumentList = new ArrayList<>(0);
        }
        else {
            this.programArgumentList = new ArrayList<>(args.size() - startPosition);
            this.programArgumentList.addAll(args.subList(startPosition, args.size()));
        }
    }

    /**
     * Place any program arguments into MIPS memory and registers
     * Arguments are stored starting at highest word of non-kernel
     * memory and working back toward runtime stack (there is a 4096
     * byte gap in between).  The argument count (argc) and pointers
     * to the arguments are stored on the runtime stack.  The stack
     * pointer register $sp is adjusted accordingly and $a0 is set
     * to the argument count (argc), and $a1 is set to the stack
     * address holding the first argument pointer (argv).
     */
    public void storeProgramArguments() {
        if (this.programArgumentList == null || this.programArgumentList.isEmpty()) {
            return;
        }

        // Runtime stack initialization from stack top-down (each is 4 bytes) :
        //    programArgumentList.size()
        //    address of first character of first program argument
        //    address of first character of second program argument
        //    ....repeat for all program arguments
        //    0x00000000    (null terminator for list of string pointers)
        // $sp will be set to the address holding the arg list size
        // $a0 will be set to the arg list size (argc)
        // $a1 will be set to stack address just "below" arg list size (argv)
        //
        // Each of the arguments themselves will be stored starting at
        // Memory.stackBaseAddress (0x7ffffffc) and working down from there:
        // 0x7ffffffc will contain null terminator for first arg
        // 0x7ffffffb will contain last character of first arg
        // 0x7ffffffa will contain next-to-last character of first arg
        // Etc down to first character of first arg.
        // Previous address will contain null terminator for second arg
        // Previous-to-that contains last character of second arg
        // Etc down to first character of second arg.
        // Follow this pattern for all remaining arguments.

        // Highest non-kernel address, sits "under" stack
        int highAddress = Memory.alignToPrevious(Memory.getInstance().getAddress(MemoryConfigurations.DYNAMIC_HIGH), Memory.BYTES_PER_WORD);
        List<Integer> argStartAddresses = new ArrayList<>(this.programArgumentList.size());
        try {
            for (String programArgument : this.programArgumentList) {
                Memory.getInstance().storeByte(highAddress, 0, true);  // trailing null byte for each argument
                for (int index = programArgument.length() - 1; index >= 0; index--) {
                    Memory.getInstance().storeByte(--highAddress, programArgument.charAt(index), true);
                }
                argStartAddresses.add(highAddress + 1);
            }
            // Now place a null word, the arg starting addresses, and arg count onto stack.
            // Base address for runtime stack
            int stackAddress = Memory.getInstance().getAddress(MemoryConfigurations.STACK_POINTER);
            if (highAddress < stackAddress) {
                // Based on current values for stackBaseAddress and stackPointer, this will
                // only happen if the combined lengths of program arguments is greater than
                // 0x7ffffffc - 0x7fffeffc = 0x00001000 = 4096 bytes.  In this case, set
                // stackAddress to next lower word boundary minus 4 for clearance (since every
                // byte from highAddress+1 is filled).
                stackAddress = Memory.alignToPrevious(highAddress, Memory.BYTES_PER_WORD) - Memory.BYTES_PER_WORD;
            }
            Memory.getInstance().storeWord(stackAddress, 0, true); // null word for end of argv array
            stackAddress -= Memory.BYTES_PER_WORD;
            for (int index = argStartAddresses.size() - 1; index >= 0; index--) {
                Memory.getInstance().storeWord(stackAddress, argStartAddresses.get(index), true);
                stackAddress -= Memory.BYTES_PER_WORD;
            }
            Memory.getInstance().storeWord(stackAddress, argStartAddresses.size(), true); // argc
            stackAddress -= Memory.BYTES_PER_WORD;

            // Set $sp register to stack address, $a0 to argc, $a1 to argv
            // Bypass the backstepping mechanism by using Register.setValue() instead of RegisterFile.updateRegister()
            RegisterFile.getRegisters()[RegisterFile.STACK_POINTER].setValue(stackAddress + Memory.BYTES_PER_WORD);
            RegisterFile.getRegisters()[RegisterFile.ARGUMENT_0].setValue(argStartAddresses.size()); // argc
            RegisterFile.getRegisters()[RegisterFile.ARGUMENT_1].setValue(stackAddress + 2 * Memory.BYTES_PER_WORD); // argv
        }
        catch (AddressErrorException exception) {
            System.out.println("Internal Error: Memory write error occurred while storing program arguments! " + exception);
            System.exit(0);
        }
    }
}
