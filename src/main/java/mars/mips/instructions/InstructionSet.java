package mars.mips.instructions;

import mars.assembler.BasicStatement;
import mars.assembler.OperandType;
import mars.assembler.log.AssemblerLog;
import mars.assembler.log.LogLevel;
import mars.assembler.log.SourceLocation;
import mars.assembler.token.SourceFile;
import mars.assembler.extended.ExpansionTemplate;
import mars.assembler.extended.TemplateParser;
import mars.assembler.token.Tokenizer;
import mars.mips.hardware.*;
import mars.mips.instructions.syscalls.Syscall;
import mars.simulator.ExceptionCause;
import mars.simulator.Simulator;
import mars.simulator.SimulatorException;
import mars.util.Binary;
import mars.util.StringTrie;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

/*
Copyright (c) 2003-2013,  Pete Sanderson and Kenneth Vollmar

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
 * The list of Instruction objects, each of which represents a MIPS instruction.
 * The instruction may either be basic (translates into binary machine code) or
 * extended (translates into sequence of one or more basic instructions).
 *
 * @author Pete Sanderson and Ken Vollmar, August 2003-5
 */
public class InstructionSet {
    public static final String EXTENDED_INSTRUCTIONS_PATH = "/config/mips/extended_instructions.yaml";

    private final List<Instruction> allInstructionsList;
    private final List<BasicInstruction> basicInstructionsList;
    private final List<ExtendedInstruction> extendedInstructionsList;
    private final Map<String, List<Instruction>> allInstructions;
    private final Map<String, List<BasicInstruction>> basicInstructions;
    private final Map<String, List<ExtendedInstruction>> extendedInstructions;
    private final StringTrie<List<Instruction>> allInstructionsTrie;
    private final InstructionDecoder decoder;

    /**
     * Creates a new InstructionSet object.
     */
    public InstructionSet() {
        this.allInstructionsList = new ArrayList<>();
        this.basicInstructionsList = new ArrayList<>();
        this.extendedInstructionsList = new ArrayList<>();
        this.allInstructions = new HashMap<>();
        this.basicInstructions = new HashMap<>();
        this.extendedInstructions = new HashMap<>();
        this.allInstructionsTrie = new StringTrie<>();
        this.decoder = new InstructionDecoder();
    }

    /**
     * Retrieve the current instruction set.
     */
    public List<Instruction> getAllInstructions() {
        return this.allInstructionsList;
    }

    public List<BasicInstruction> getBasicInstructions() {
        return this.basicInstructionsList;
    }

    public List<ExtendedInstruction> getExtendedInstructions() {
        return this.extendedInstructionsList;
    }

    public Set<String> getAllMnemonics() {
        return this.allInstructions.keySet();
    }

    public InstructionDecoder getDecoder() {
        return this.decoder;
    }

    public void addBasicInstruction(BasicInstruction instruction) {
        this.basicInstructions.computeIfAbsent(instruction.getMnemonic(), mnemonic -> new ArrayList<>())
            .add(instruction);
        this.allInstructions.computeIfAbsent(instruction.getMnemonic(), mnemonic -> new ArrayList<>())
            .add(instruction);
        this.allInstructionsTrie.computeIfAbsent(instruction.getMnemonic(), mnemonic -> new ArrayList<>())
            .add(instruction);

        this.basicInstructionsList.add(instruction);
        this.allInstructionsList.add(instruction);

        this.decoder.addInstruction(instruction);
    }
    
    public void addExtendedInstruction(ExtendedInstruction instruction) {
        this.extendedInstructions.computeIfAbsent(instruction.getMnemonic(), mnemonic -> new ArrayList<>())
            .add(instruction);
        this.allInstructions.computeIfAbsent(instruction.getMnemonic(), mnemonic -> new ArrayList<>())
            .add(instruction);
        this.allInstructionsTrie.computeIfAbsent(instruction.getMnemonic(), mnemonic -> new ArrayList<>())
            .add(instruction);

        this.extendedInstructionsList.add(instruction);
        this.allInstructionsList.add(instruction);
    }

    /**
     * Adds all instructions to the set.  A given extended instruction may have
     * more than one Instruction object, depending on how many formats it can have.
     */
    public void populate() {
        // Load the set of basic instructions
        this.loadBasicInstructions();
        // Load the set of extended instructions from file
        this.loadExtendedInstructions();
        // Ensure syscalls are loaded
        SyscallManager.getSyscalls();
    }
    
    public void loadBasicInstructions() {
        //////////////////////////////// BASIC INSTRUCTIONS START HERE ////////////////////////////////

        this.addBasicInstruction(new BasicInstruction(
            "nop",
            List.of(),
            InstructionFormat.R_TYPE,
            false,
            "Null OPeration",
            "No action",
            "000000 00000 00000 00000 00000 000000",
            statement -> {} // Hey I like this so far!
        ));
        this.addBasicInstruction(new BasicInstruction(
            "add",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "ADDition",
            "Set {0} to ({1} plus {2}), exception on overflow",
            "000000 sssss ttttt fffff 00000 100000",
            statement -> {
                int add1 = Processor.getValue(statement.getOperand(1));
                int add2 = Processor.getValue(statement.getOperand(2));
                int sum = add1 + add2;
                // overflow on A+B detected when A and B have same sign and A+B has other sign.
                if ((add1 >= 0 && add2 >= 0 && sum < 0) || (add1 < 0 && add2 < 0 && sum >= 0)) {
                    throw new SimulatorException(statement, "arithmetic overflow", ExceptionCause.ARITHMETIC_OVERFLOW);
                }
                Processor.setValue(statement.getOperand(0), sum);
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "sub",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "SUBtraction",
            "Set {0} to ({1} minus {2}), exception on overflow",
            "000000 sssss ttttt fffff 00000 100010",
            statement -> {
                int sub1 = Processor.getValue(statement.getOperand(1));
                int sub2 = Processor.getValue(statement.getOperand(2));
                int diff = sub1 - sub2;
                // overflow on A-B detected when A and B have opposite signs and A-B has B's sign
                if ((sub1 >= 0 && sub2 < 0 && diff < 0) || (sub1 < 0 && sub2 >= 0 && diff >= 0)) {
                    throw new SimulatorException(statement, "arithmetic overflow", ExceptionCause.ARITHMETIC_OVERFLOW);
                }
                Processor.setValue(statement.getOperand(0), diff);
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "addi",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.INTEGER_16_SIGNED),
            InstructionFormat.I_TYPE,
            false,
            "ADDition Immediate",
            "Set {0} to ({1} plus sign-extended 16-bit immediate), exception on overflow",
            "001000 sssss fffff tttttttttttttttt",
            statement -> {
                int add1 = Processor.getValue(statement.getOperand(1));
                int add2 = statement.getOperand(2) << 16 >> 16;
                int sum = add1 + add2;
                // overflow on A+B detected when A and B have same sign and A+B has other sign.
                if ((add1 >= 0 && add2 >= 0 && sum < 0) || (add1 < 0 && add2 < 0 && sum >= 0)) {
                    throw new SimulatorException(statement, "arithmetic overflow", ExceptionCause.ARITHMETIC_OVERFLOW);
                }
                Processor.setValue(statement.getOperand(0), sum);
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "addu",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "ADDition Unsigned",
            "Set {0} to ({1} plus {2}), no exception on overflow",
            "000000 sssss ttttt fffff 00000 100001",
            statement -> {
                Processor.setValue(statement.getOperand(0), Processor.getValue(statement.getOperand(1)) + Processor.getValue(statement.getOperand(2)));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "subu",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "SUBtraction Unsigned",
            "Set {0} to ({1} minus {2}), no exception on overflow",
            "000000 sssss ttttt fffff 00000 100011",
            statement -> {
                Processor.setValue(statement.getOperand(0), Processor.getValue(statement.getOperand(1)) - Processor.getValue(statement.getOperand(2)));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "addiu",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.INTEGER_16_SIGNED),
            InstructionFormat.I_TYPE,
            false,
            "ADDition Immediate Unsigned",
            "Set {0} to ({1} plus sign-extended 16-bit immediate), no exception on overflow",
            "001001 sssss fffff tttttttttttttttt",
            statement -> {
                Processor.setValue(statement.getOperand(0), Processor.getValue(statement.getOperand(1)) + (statement.getOperand(2) << 16 >> 16));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "mult",
            List.of(OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "MULtiplication Temporary",
            "Set HI to high-order 32 bits, LO to low-order 32 bits of the product of {0} and {1} (use mfhi to access HI, mflo to access LO)",
            "000000 fffff sssss 00000 00000 011000",
            statement -> {
                long product = (long) Processor.getValue(statement.getOperand(0)) * (long) Processor.getValue(statement.getOperand(1));
                Processor.setHighOrder((int) (product >> 32));
                Processor.setLowOrder((int) (product << 32 >> 32));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "multu",
            List.of(OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "MULtiplication Temporary Unsigned",
            "Set HI to high-order 32 bits, LO to low-order 32 bits of the product of unsigned {0} and {1} (use mfhi to access HI, mflo to access LO)",
            "000000 fffff sssss 00000 00000 011001",
            statement -> {
                long product = ((long) Processor.getValue(statement.getOperand(0)) << 32 >>> 32) * ((long) Processor.getValue(statement.getOperand(1)) << 32 >>> 32);
                Processor.setHighOrder((int) (product >> 32));
                Processor.setLowOrder((int) (product << 32 >> 32));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "mul",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "MULtiplication",
            "Set HI to high-order 32 bits, LO and {0} to low-order 32 bits of the product of {1} and {2} (use mfhi to access HI, mflo to access LO)",
            "011100 sssss ttttt fffff 00000 000010",
            statement -> {
                long product = (long) Processor.getValue(statement.getOperand(1)) * (long) Processor.getValue(statement.getOperand(2));
                Processor.setValue(statement.getOperand(0), (int) (product << 32 >> 32));
                Processor.setHighOrder((int) (product >> 32));
                Processor.setLowOrder((int) (product << 32 >> 32));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "madd",
            List.of(OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "Multiplication then ADDition",
            "Multiply {0} by {1} then increment HI by high-order 32 bits of product, increment LO by low-order 32 bits of product (use mfhi to access HI, mflo to access LO)",
            "011100 fffff sssss 00000 00000 000000",
            statement -> {
                long product = (long) Processor.getValue(statement.getOperand(0)) * (long) Processor.getValue(statement.getOperand(1));
                long contentsHiLo = Binary.twoIntsToLong(Processor.getHighOrder(), Processor.getLowOrder());
                long sum = contentsHiLo + product;
                Processor.setHighOrder(Binary.highOrderLongToInt(sum));
                Processor.setLowOrder(Binary.lowOrderLongToInt(sum));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "maddu",
            List.of(OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "Multiplication then ADDition Unsigned",
            "Multiply {0} by {1} then increment HI by high-order 32 bits of product, increment LO by low-order 32 bits of product, unsigned (use mfhi to access HI, mflo to access LO)",
            "011100 fffff sssss 00000 00000 000001",
            statement -> {
                long product = (((long) Processor.getValue(statement.getOperand(0))) << 32 >>> 32) * (((long) Processor.getValue(statement.getOperand(1))) << 32 >>> 32);
                long contentsHiLo = Binary.twoIntsToLong(Processor.getHighOrder(), Processor.getLowOrder());
                long sum = contentsHiLo + product;
                Processor.setHighOrder(Binary.highOrderLongToInt(sum));
                Processor.setLowOrder(Binary.lowOrderLongToInt(sum));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "msub",
            List.of(OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "Multiplication then SUBtraction",
            "Multiply {0} by {1} then decrement HI by high-order 32 bits of product, decrement LO by low-order 32 bits of product (use mfhi to access HI, mflo to access LO)",
            "011100 fffff sssss 00000 00000 000100",
            statement -> {
                long product = (long) Processor.getValue(statement.getOperand(0)) * (long) Processor.getValue(statement.getOperand(1));
                // Register 33 is HIGH and 34 is LOW.
                long contentsHiLo = Binary.twoIntsToLong(Processor.getHighOrder(), Processor.getLowOrder());
                long diff = contentsHiLo - product;
                Processor.setHighOrder(Binary.highOrderLongToInt(diff));
                Processor.setLowOrder(Binary.lowOrderLongToInt(diff));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "msubu",
            List.of(OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "Multiplication then SUBtraction Unsigned",
            "Multiply {0} by {1} then decrement HI by high-order 32 bits of product, decement LO by low-order 32 bits of product, unsigned (use mfhi to access HI, mflo to access LO)",
            "011100 fffff sssss 00000 00000 000101",
            statement -> {
                long product = ((long) Processor.getValue(statement.getOperand(0)) << 32 >>> 32) * ((long) Processor.getValue(statement.getOperand(1)) << 32 >>> 32);
                // Register 33 is HIGH and 34 is LOW.
                long contentsHiLo = Binary.twoIntsToLong(Processor.getHighOrder(), Processor.getLowOrder());
                long diff = contentsHiLo - product;
                Processor.setHighOrder(Binary.highOrderLongToInt(diff));
                Processor.setLowOrder(Binary.lowOrderLongToInt(diff));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "div",
            List.of(OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "DIVision",
            "Divide {0} by {1} then set LO to quotient and HI to remainder (use mfhi to access HI, mflo to access LO)",
            "000000 fffff sssss 00000 00000 011010",
            statement -> {
                if (Processor.getValue(statement.getOperand(1)) == 0) {
                    // Note: no exception, undefined results for division by zero.
                    // COD3 Appendix A says "with overflow," but the MIPS32 instruction set
                    // specification states "No arithmetic exception occurs under any circumstances."
                    return;
                }
                // Register 33 is HIGH and 34 is LOW
                Processor.setHighOrder(Processor.getValue(statement.getOperand(0)) % Processor.getValue(statement.getOperand(1)));
                Processor.setLowOrder(Processor.getValue(statement.getOperand(0)) / Processor.getValue(statement.getOperand(1)));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "divu",
            List.of(OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "DIVision Unsigned",
            "Divide unsigned {0} by {1} then set LO to quotient and HI to remainder (use mfhi to access HI, mflo to access LO)",
            "000000 fffff sssss 00000 00000 011011",
            statement -> {
                if (Processor.getValue(statement.getOperand(1)) == 0) {
                    // Note: no exception, undefined results for division by zero.
                    // COD3 Appendix A says "with overflow," but the MIPS32 instruction set
                    // specification states "No arithmetic exception occurs under any circumstances."
                    return;
                }
                long oper1 = (long) Processor.getValue(statement.getOperand(0)) << 32 >>> 32;
                long oper2 = (long) Processor.getValue(statement.getOperand(1)) << 32 >>> 32;
                // Register 33 is HIGH and 34 is LOW
                Processor.setHighOrder((int) ((oper1 % oper2) << 32 >> 32));
                Processor.setLowOrder((int) ((oper1 / oper2) << 32 >> 32));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "mfhi",
            List.of(OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "Move From HI register",
            "Set {0} to contents of HI register",
            "000000 00000 00000 fffff 00000 010000",
            statement -> {
                Processor.setValue(statement.getOperand(0), Processor.getHighOrder());
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "mflo",
            List.of(OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "Move From LO register",
            "Set {0} to contents of LO register",
            "000000 00000 00000 fffff 00000 010010",
            statement -> {
                Processor.setValue(statement.getOperand(0), Processor.getLowOrder());
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "mthi",
            List.of(OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "Move to HI register",
            "Set HI register to contents of {0}",
            "000000 fffff 00000 00000 00000 010001",
            statement -> {
                Processor.setHighOrder(Processor.getValue(statement.getOperand(0)));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "mtlo",
            List.of(OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "Move to LO register",
            "Set LO register to contents of {0}",
            "000000 fffff 00000 00000 00000 010011",
            statement -> {
                Processor.setLowOrder(Processor.getValue(statement.getOperand(0)));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "and",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "AND",
            "Set {0} to ({1} bitwise-AND {2})",
            "000000 sssss ttttt fffff 00000 100100",
            statement -> {
                Processor.setValue(statement.getOperand(0), Processor.getValue(statement.getOperand(1)) & Processor.getValue(statement.getOperand(2)));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "or",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "OR",
            "Set {0} to ({1} bitwise-OR {2})",
            "000000 sssss ttttt fffff 00000 100101",
            statement -> {
                Processor.setValue(statement.getOperand(0), Processor.getValue(statement.getOperand(1)) | Processor.getValue(statement.getOperand(2)));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "andi",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.INTEGER_16_UNSIGNED),
            InstructionFormat.I_TYPE,
            false,
            "AND Immediate",
            "Set {0} to ({1} bitwise-AND unsigned 16-bit immediate)",
            "001100 sssss fffff tttttttttttttttt",
            statement -> {
                // ANDing with 0x0000FFFF zero-extends the immediate (high 16 bits always 0).
                Processor.setValue(statement.getOperand(0), Processor.getValue(statement.getOperand(1)) & (statement.getOperand(2) & 0x0000FFFF));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "ori",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.INTEGER_16_UNSIGNED),
            InstructionFormat.I_TYPE,
            false,
            "OR Immediate",
            "Set {0} to ({1} bitwise-OR unsigned 16-bit immediate)",
            "001101 sssss fffff tttttttttttttttt",
            statement -> {
                // ANDing with 0x0000FFFF zero-extends the immediate (high 16 bits always 0).
                Processor.setValue(statement.getOperand(0), Processor.getValue(statement.getOperand(1)) | (statement.getOperand(2) & 0x0000FFFF));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "nor",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "NOR",
            "Set {0} to inverse of ({1} bitwise-OR {2})",
            "000000 sssss ttttt fffff 00000 100111",
            statement -> {
                Processor.setValue(statement.getOperand(0), ~(Processor.getValue(statement.getOperand(1)) | Processor.getValue(statement.getOperand(2))));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "xor",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "XOR",
            "Set {0} to ({1} bitwise-exclusive-OR {2})",
            "000000 sssss ttttt fffff 00000 100110",
            statement -> {
                Processor.setValue(statement.getOperand(0), Processor.getValue(statement.getOperand(1)) ^ Processor.getValue(statement.getOperand(2)));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "xori",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.INTEGER_16_UNSIGNED),
            InstructionFormat.I_TYPE,
            false,
            "XOR Immediate",
            "Set {0} to ({1} bitwise-exclusive-OR unsigned 16-bit immediate)",
            "001110 sssss fffff tttttttttttttttt",
            statement -> {
                // ANDing with 0x0000FFFF zero-extends the immediate (high 16 bits always 0).
                Processor.setValue(statement.getOperand(0), Processor.getValue(statement.getOperand(1)) ^ (statement.getOperand(2) & 0x0000FFFF));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "sll",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.INTEGER_5_UNSIGNED),
            InstructionFormat.R_TYPE,
            false,
            "Shift Left Logical",
            "Set {0} to result of shifting {1} left by number of bits specified by immediate",
            "000000 00000 sssss fffff ttttt 000000",
            statement -> {
                Processor.setValue(statement.getOperand(0), Processor.getValue(statement.getOperand(1)) << statement.getOperand(2));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "sllv",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "Shift Left Logical Variable",
            "Set {0} to result of shifting {1} left by number of bits specified by value in low-order 5 bits of {2}",
            "000000 ttttt sssss fffff 00000 000100",
            statement -> {
                // Mask all but low 5 bits of register containing shamt.
                Processor.setValue(statement.getOperand(0), Processor.getValue(statement.getOperand(1)) << (
                    Processor.getValue(statement.getOperand(2)) & 0x0000001F));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "srl",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.INTEGER_5_UNSIGNED),
            InstructionFormat.R_TYPE,
            false,
            "Shift Right Logical",
            "Set {0} to result of shifting {1} right by number of bits specified by immediate",
            "000000 00000 sssss fffff ttttt 000010",
            statement -> {
                // must zero-fill, so use ">>>" instead of ">>".
                Processor.setValue(statement.getOperand(0), Processor.getValue(statement.getOperand(1)) >>> statement.getOperand(2));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "sra",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.INTEGER_5_UNSIGNED),
            InstructionFormat.R_TYPE,
            false,
            "Shift Right Arithmetic",
            "Set {0} to result of sign-extended shifting {1} right by number of bits specified by immediate",
            "000000 00000 sssss fffff ttttt 000011",
            statement -> {
                // must sign-fill, so use ">>".
                Processor.setValue(statement.getOperand(0), Processor.getValue(statement.getOperand(1)) >> statement.getOperand(2));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "srlv",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "Shift Right Logical Variable",
            "Set {0} to result of shifting {1} right by number of bits specified by value in low-order 5 bits of {2}",
            "000000 ttttt sssss fffff 00000 000110",
            statement -> {
                // Mask all but low 5 bits of register containing shamt. Use ">>>" to zero-fill.
                Processor.setValue(statement.getOperand(0), Processor.getValue(statement.getOperand(1)) >>> (
                    Processor.getValue(statement.getOperand(2)) & 0x0000001F));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "srav",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "Shift Right Arithmetic Variable",
            "Set {0} to result of sign-extended shifting {1} right by number of bits specified by value in low-order 5 bits of {2}",
            "000000 ttttt sssss fffff 00000 000111",
            statement -> {
                // Mask all but low 5 bits of register containing shamt. Use ">>" to sign-fill.
                Processor.setValue(statement.getOperand(0), Processor.getValue(statement.getOperand(1)) >> (
                    Processor.getValue(statement.getOperand(2)) & 0x0000001F));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "lw",
            List.of(OperandType.REGISTER, OperandType.INTEGER_16_SIGNED, OperandType.PAREN_REGISTER),
            InstructionFormat.I_TYPE,
            false,
            "Load Word",
            "Set {0} to contents of effective memory word address",
            "100011 ttttt fffff ssssssssssssssss",
            statement -> {
                try {
                    Processor.setValue(statement.getOperand(0), Memory.getInstance().fetchWord(Processor.getValue(statement.getOperand(2)) + statement.getOperand(1), true));
                }
                catch (AddressErrorException exception) {
                    throw new SimulatorException(statement, exception);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "ll",
            List.of(OperandType.REGISTER, OperandType.INTEGER_16_SIGNED, OperandType.PAREN_REGISTER),
            InstructionFormat.I_TYPE,
            false,
            "Load Linked",
            "Paired with Store Conditional (sc) to perform atomic read-modify-write; equivalent to Load Word (lw) because MARS does not simulate multiple processors",
            "110000 ttttt fffff ssssssssssssssss",
            // The ll (load link) command is supposed to be the front end of an atomic
            // operation completed by sc (store conditional), with success or failure
            // of the store depending on whether the memory block containing the
            // loaded word is modified in the meantime by a different processor.
            // Since MARS, like SPIM, simulates only a single processor, the store
            // conditional will always succeed so there is no need to do anything
            // special here.  In that case, ll is same as lw.  And sc does the same
            // thing as sw except in addition it writes 1 into the source register.
            statement -> {
                try {
                    Processor.setValue(statement.getOperand(0), Memory.getInstance().fetchWord(Processor.getValue(statement.getOperand(2)) + statement.getOperand(1), true));
                }
                catch (AddressErrorException exception) {
                    throw new SimulatorException(statement, exception);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "lwl",
            List.of(OperandType.REGISTER, OperandType.INTEGER_16_SIGNED, OperandType.PAREN_REGISTER),
            InstructionFormat.I_TYPE,
            false,
            "Load Word Left",
            "Load from 1 to 4 bytes left-justified into {0}, starting with effective memory byte address and continuing through the low-order byte of its word",
            "100010 ttttt fffff ssssssssssssssss",
            statement -> {
                try {
                    int address = Processor.getValue(statement.getOperand(2)) + statement.getOperand(1);
                    int result = Processor.getValue(statement.getOperand(0));
                    for (int i = 0; i <= address % Memory.BYTES_PER_WORD; i++) {
                        result = Binary.setByte(result, 3 - i, Memory.getInstance().fetchByte(address - i, true));
                    }
                    Processor.setValue(statement.getOperand(0), result);
                }
                catch (AddressErrorException exception) {
                    throw new SimulatorException(statement, exception);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "lwr",
            List.of(OperandType.REGISTER, OperandType.INTEGER_16_SIGNED, OperandType.PAREN_REGISTER),
            InstructionFormat.I_TYPE,
            false,
            "Load Word Right",
            "Load from 1 to 4 bytes right-justified into {0}, starting with effective memory byte address and continuing through the high-order byte of its word",
            "100110 ttttt fffff ssssssssssssssss",
            statement -> {
                try {
                    int address = Processor.getValue(statement.getOperand(2)) + statement.getOperand(1);
                    int result = Processor.getValue(statement.getOperand(0));
                    for (int i = 0; i <= 3 - (address % Memory.BYTES_PER_WORD); i++) {
                        result = Binary.setByte(result, i, Memory.getInstance().fetchByte(address + i, true));
                    }
                    Processor.setValue(statement.getOperand(0), result);
                }
                catch (AddressErrorException exception) {
                    throw new SimulatorException(statement, exception);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "sw",
            List.of(OperandType.REGISTER, OperandType.INTEGER_16_SIGNED, OperandType.PAREN_REGISTER),
            InstructionFormat.I_TYPE,
            false,
            "Store Word",
            "Store contents of {0} into effective memory word address",
            "101011 ttttt fffff ssssssssssssssss",
            statement -> {
                try {
                    Memory.getInstance().storeWord(Processor.getValue(statement.getOperand(2)) + statement.getOperand(1), Processor.getValue(statement.getOperand(0)), true);
                }
                catch (AddressErrorException exception) {
                    throw new SimulatorException(statement, exception);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "sc",
            List.of(OperandType.REGISTER, OperandType.INTEGER_16_SIGNED, OperandType.PAREN_REGISTER),
            InstructionFormat.I_TYPE,
            false,
            "Store Conditional",
            "Paired with Load Linked (ll) to perform atomic read-modify-write; store content {0} into effective address, then set {0} to 1 for success (always succeeds because MARS does not simulate multiple processors)",
            "111000 ttttt fffff ssssssssssssssss",
            // See comments with "ll" instruction above.  "sc" is implemented
            // like "sw", except that 1 is placed in the source register.
            statement -> {
                try {
                    Memory.getInstance().storeWord(Processor.getValue(statement.getOperand(2)) + statement.getOperand(1), Processor.getValue(statement.getOperand(0)), true);
                }
                catch (AddressErrorException exception) {
                    throw new SimulatorException(statement, exception);
                }
                Processor.setValue(statement.getOperand(0), 1); // always succeeds
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "swl",
            List.of(OperandType.REGISTER, OperandType.INTEGER_16_SIGNED, OperandType.PAREN_REGISTER),
            InstructionFormat.I_TYPE,
            false,
            "Store Word Left",
            "Store high-order 1 to 4 bytes of {0} into memory, starting with effective byte address and continuing through the low-order byte of its word",
            "101010 ttttt fffff ssssssssssssssss",
            statement -> {
                try {
                    int address = Processor.getValue(statement.getOperand(2)) + statement.getOperand(1);
                    int source = Processor.getValue(statement.getOperand(0));
                    for (int i = 0; i <= address % Memory.BYTES_PER_WORD; i++) {
                        Memory.getInstance().storeByte(address - i, Binary.getByte(source, 3 - i), true);
                    }
                }
                catch (AddressErrorException exception) {
                    throw new SimulatorException(statement, exception);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "swr",
            List.of(OperandType.REGISTER, OperandType.INTEGER_16_SIGNED, OperandType.PAREN_REGISTER),
            InstructionFormat.I_TYPE,
            false,
            "Store Word Right",
            "Store low-order 1 to 4 bytes of {0} into memory, starting with high-order byte of word containing effective byte address and continuing through that byte address",
            "101110 ttttt fffff ssssssssssssssss",
            statement -> {
                try {
                    int address = Processor.getValue(statement.getOperand(2)) + statement.getOperand(1);
                    int source = Processor.getValue(statement.getOperand(0));
                    for (int i = 0; i <= 3 - (address % Memory.BYTES_PER_WORD); i++) {
                        Memory.getInstance().storeByte(address + i, Binary.getByte(source, i), true);
                    }
                }
                catch (AddressErrorException exception) {
                    throw new SimulatorException(statement, exception);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "lui",
            List.of(OperandType.REGISTER, OperandType.INTEGER_16),
            InstructionFormat.I_TYPE,
            false,
            "Load Upper Immediate",
            "Set high-order 16 bits of {0} to 16-bit immediate and low-order 16 bits to 0",
            "001111 00000 fffff ssssssssssssssss",
            statement -> {
                Processor.setValue(statement.getOperand(0), statement.getOperand(1) << 16);
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "beq",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.BRANCH_OFFSET),
            InstructionFormat.I_TYPE_BRANCH,
            true,
            "Branch if EQual",
            "Branch to statement at label's address if {0} is equal to {1}",
            "000100 fffff sssss tttttttttttttttt",
            statement -> {
                if (Processor.getValue(statement.getOperand(0)) == Processor.getValue(statement.getOperand(1))) {
                    this.processBranch(statement.getOperand(2));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "bne",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.BRANCH_OFFSET),
            InstructionFormat.I_TYPE_BRANCH,
            true,
            "Branch if Not Equal",
            "Branch to statement at label's address unless {0} is equal to {1}",
            "000101 fffff sssss tttttttttttttttt",
            statement -> {
                if (Processor.getValue(statement.getOperand(0)) != Processor.getValue(statement.getOperand(1))) {
                    this.processBranch(statement.getOperand(2));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "bgez",
            List.of(OperandType.REGISTER, OperandType.BRANCH_OFFSET),
            InstructionFormat.I_TYPE_BRANCH,
            true,
            "Branch if Greater than or Equal to Zero",
            "Branch to statement at label's address if {0} is greater than or equal to zero",
            "000001 fffff 00001 ssssssssssssssss",
            statement -> {
                if (Processor.getValue(statement.getOperand(0)) >= 0) {
                    this.processBranch(statement.getOperand(1));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "bgezal",
            List.of(OperandType.REGISTER, OperandType.BRANCH_OFFSET),
            InstructionFormat.I_TYPE_BRANCH,
            true,
            "Branch if Greater than or Equal to Zero And Link",
            "If {0} is greater than or equal to zero, set $ra to the Program Counter and branch to statement at label's address",
            "000001 fffff 10001 ssssssssssssssss",
            statement -> {
                if (Processor.getValue(statement.getOperand(0)) >= 0) {
                    // the "and link" part
                    this.processLink(Processor.RETURN_ADDRESS);
                    this.processBranch(statement.getOperand(1));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "bgtz",
            List.of(OperandType.REGISTER, OperandType.BRANCH_OFFSET),
            InstructionFormat.I_TYPE_BRANCH,
            true,
            "Branch if Greater Than Zero",
            "Branch to statement at label's address if {0} is greater than zero",
            "000111 fffff 00000 ssssssssssssssss",
            statement -> {
                if (Processor.getValue(statement.getOperand(0)) > 0) {
                    this.processBranch(statement.getOperand(1));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "blez",
            List.of(OperandType.REGISTER, OperandType.BRANCH_OFFSET),
            InstructionFormat.I_TYPE_BRANCH,
            true,
            "Branch if Less than or Equal to Zero",
            "Branch to statement at label's address if {0} is less than or equal to zero",
            "000110 fffff 00000 ssssssssssssssss",
            statement -> {
                if (Processor.getValue(statement.getOperand(0)) <= 0) {
                    this.processBranch(statement.getOperand(1));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "bltz",
            List.of(OperandType.REGISTER, OperandType.BRANCH_OFFSET),
            InstructionFormat.I_TYPE_BRANCH,
            true,
            "Branch if Less Than Zero",
            "Branch to statement at label's address if {0} is less than zero",
            "000001 fffff 00000 ssssssssssssssss",
            statement -> {
                if (Processor.getValue(statement.getOperand(0)) < 0) {
                    this.processBranch(statement.getOperand(1));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "bltzal",
            List.of(OperandType.REGISTER, OperandType.BRANCH_OFFSET),
            InstructionFormat.I_TYPE_BRANCH,
            true,
            "Branch if Less Than Zero And Link",
            "If {0} is less than or equal to zero, set $ra to the Program Counter and branch to statement at label's address",
            "000001 fffff 10000 ssssssssssssssss",
            statement -> {
                if (Processor.getValue(statement.getOperand(0)) < 0) {
                    // the "and link" part
                    this.processLink(Processor.RETURN_ADDRESS);
                    this.processBranch(statement.getOperand(1));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "slt",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "Set Less Than",
            "If {1} is less than {2}, set {0} to 1, otherwise set {0} to 0",
            "000000 sssss ttttt fffff 00000 101010",
            statement -> {
                Processor.setValue(statement.getOperand(0), (Processor.getValue(statement.getOperand(1)) < Processor.getValue(statement.getOperand(2))) ? 1 : 0);
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "sltu",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "Set Less Than Unsigned",
            "If {1} is less than {2} using unsigned comparision, set {0} to 1, otherwise set {0} to 0",
            "000000 sssss ttttt fffff 00000 101011",
            statement -> {
                Processor.setValue(statement.getOperand(0), (Integer.compareUnsigned(Processor.getValue(statement.getOperand(1)), Processor.getValue(statement.getOperand(2))) < 0) ? 1 : 0);
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "slti",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.INTEGER_16_SIGNED),
            InstructionFormat.I_TYPE,
            false,
            "Set Less Than Immediate",
            "If {1} is less than sign-extended 16-bit immediate, set {0} to 1, otherwise set {0} to 0",
            "001010 sssss fffff tttttttttttttttt",
            statement -> {
                // 16 bit immediate value in statement.getOperand(2) is sign-extended
                Processor.setValue(statement.getOperand(0), (Processor.getValue(statement.getOperand(1)) < (statement.getOperand(2) << 16 >> 16)) ? 1 : 0);
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "sltiu",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.INTEGER_16_SIGNED),
            InstructionFormat.I_TYPE,
            false,
            "Set Less Than Immediate Unsigned",
            "If {1} is less than sign-extended 16-bit immediate using unsigned comparison, set {0} to 1, otherwise set {0} to 0",
            "001011 sssss fffff tttttttttttttttt",
            statement -> {
                // 16 bit immediate value in statement.getOperand(2) is sign-extended
                Processor.setValue(statement.getOperand(0), (Integer.compareUnsigned(Processor.getValue(statement.getOperand(1)), statement.getOperand(2) << 16 >> 16) < 0) ? 1 : 0);
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "movn",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "MOVe if Not zero",
            "Set {0} to {1} only if {2} is not zero",
            "000000 sssss ttttt fffff 00000 001011",
            statement -> {
                if (Processor.getValue(statement.getOperand(2)) != 0) {
                    Processor.setValue(statement.getOperand(0), Processor.getValue(statement.getOperand(1)));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "movz",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "MOVe if Zero",
            "Set {0} to {1} only if {2} is zero",
            "000000 sssss ttttt fffff 00000 001010",
            statement -> {
                if (Processor.getValue(statement.getOperand(2)) == 0) {
                    Processor.setValue(statement.getOperand(0), Processor.getValue(statement.getOperand(1)));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "movf",
            List.of(OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "MOVe if condition flag False",
            "Set {0} to {1} only if FPU (Coprocessor 1) condition flag 0 is false (bit value is 0)",
            "000000 sssss 000 00 fffff 00000 000001",
            statement -> {
                if (Coprocessor1.getConditionFlag(0) == 0) {
                    Processor.setValue(statement.getOperand(0), Processor.getValue(statement.getOperand(1)));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "movf",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.INTEGER_3_UNSIGNED),
            InstructionFormat.R_TYPE,
            false,
            "MOVe if condition flag False",
            "Set {0} to {1} only if FPU (Coprocessor 1) condition flag specified by the last operand is false (bit value is 0)",
            "000000 sssss ttt 00 fffff 00000 000001",
            statement -> {
                if (Coprocessor1.getConditionFlag(statement.getOperand(2)) == 0) {
                    Processor.setValue(statement.getOperand(0), Processor.getValue(statement.getOperand(1)));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "movt",
            List.of(OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "MOVe if condition flag True",
            "Set {0} to {1} only if FPU (Coprocessor 1) condition flag 0 is true (bit value is 1)",
            "000000 sssss 000 01 fffff 00000 000001",
            statement -> {
                if (Coprocessor1.getConditionFlag(0) == 1) {
                    Processor.setValue(statement.getOperand(0), Processor.getValue(statement.getOperand(1)));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "movt",
            List.of(OperandType.REGISTER, OperandType.REGISTER, OperandType.INTEGER_3_UNSIGNED),
            InstructionFormat.R_TYPE,
            false,
            "MOVe if condition flag True",
            "Set {0} to {1} only if FPU (Coprocessor 1) condition flag specified by the last operand is true (bit value is 1)",
            "000000 sssss ttt 01 fffff 00000 000001",
            statement -> {
                if (Coprocessor1.getConditionFlag(statement.getOperand(2)) == 1) {
                    Processor.setValue(statement.getOperand(0), Processor.getValue(statement.getOperand(1)));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "break",
            List.of(OperandType.INTEGER_16),
            InstructionFormat.R_TYPE,
            false,
            "BREAK execution",
            "Terminate program execution with the specified exception code",
            "000000 ffffffffffffffffffff 001101",
            statement -> {
                throw new SimulatorException(statement, "break instruction executed; code = " + statement.getOperand(0) + ".", ExceptionCause.BREAKPOINT);
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "break",
            List.of(),
            InstructionFormat.R_TYPE,
            false,
            "BREAK execution",
            "Terminate program execution with an exception",
            "000000 00000 00000 00000 00000 001101",
            statement -> {
                throw new SimulatorException(statement, "break instruction executed; no code given.", ExceptionCause.BREAKPOINT);
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "syscall",
            List.of(),
            InstructionFormat.R_TYPE,
            false,
            "SYStem CALL",
            "Issue a system call based on contents of $v0 (see syscall help for details)",
            "000000 00000 00000 00000 00000 001100",
            statement -> {
                this.processSyscall(statement, Processor.getValue(Processor.VALUE_0));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "j",
            List.of(OperandType.JUMP_LABEL),
            InstructionFormat.J_TYPE,
            true,
            "Jump",
            "Jump execution to statement at label's address",
            "000010 ffffffffffffffffffffffffff",
            statement -> {
                this.processJump(((Processor.getProgramCounter() & 0xF0000000) | (statement.getOperand(0) << 2)));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "jr",
            List.of(OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            true,
            "Jump to Register",
            "Jump execution to statement whose address is in {0}",
            "000000 fffff 00000 00000 00000 001000",
            statement -> {
                this.processJump(Processor.getValue(statement.getOperand(0)));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "jal",
            List.of(OperandType.JUMP_LABEL),
            InstructionFormat.J_TYPE,
            true,
            "Jump And Link",
            "Set $ra to the Program Counter (return address) then jump execution to statement at label's address",
            "000011 ffffffffffffffffffffffffff",
            statement -> {
                this.processLink(Processor.RETURN_ADDRESS);
                this.processJump((Processor.getProgramCounter() & 0xF0000000) | (statement.getOperand(0) << 2));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "jalr",
            List.of(OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            true,
            "Jump And Link to Register",
            "Set {0} to the Program Counter (return address) then jump execution to statement whose address is in {1}",
            "000000 sssss 00000 fffff 00000 001001",
            statement -> {
                this.processLink(statement.getOperand(0));
                this.processJump(Processor.getValue(statement.getOperand(1)));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "jalr",
            List.of(OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            true,
            "Jump And Link to Register",
            "Set $ra to the Program Counter (return address) then jump execution to statement whose address is in {0}",
            "000000 fffff 00000 11111 00000 001001",
            statement -> {
                this.processLink(Processor.RETURN_ADDRESS);
                this.processJump(Processor.getValue(statement.getOperand(0)));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "lb",
            List.of(OperandType.REGISTER, OperandType.INTEGER_16_SIGNED, OperandType.PAREN_REGISTER),
            InstructionFormat.I_TYPE,
            false,
            "Load Byte",
            "Set {0} to sign-extended 8-bit value from effective memory byte address",
            "100000 ttttt fffff ssssssssssssssss",
            statement -> {
                try {
                    Processor.setValue(statement.getOperand(0), Memory.getInstance().fetchByte(Processor.getValue(statement.getOperand(2)) + (statement.getOperand(1) << 16 >> 16), true) << 24 >> 24);
                }
                catch (AddressErrorException exception) {
                    throw new SimulatorException(statement, exception);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "lbu",
            List.of(OperandType.REGISTER, OperandType.INTEGER_16_SIGNED, OperandType.PAREN_REGISTER),
            InstructionFormat.I_TYPE,
            false,
            "Load Byte Unsigned",
            "Set {0} to zero-extended 8-bit value from effective memory byte address",
            "100100 ttttt fffff ssssssssssssssss",
            statement -> {
                try {
                    // offset is sign-extended and loaded byte value is zero-extended
                    Processor.setValue(statement.getOperand(0), Memory.getInstance().fetchByte(Processor.getValue(statement.getOperand(2)) + (statement.getOperand(1) << 16 >> 16), true));
                }
                catch (AddressErrorException exception) {
                    throw new SimulatorException(statement, exception);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "lh",
            List.of(OperandType.REGISTER, OperandType.INTEGER_16_SIGNED, OperandType.PAREN_REGISTER),
            InstructionFormat.I_TYPE,
            false,
            "Load Halfword",
            "Set {0} to sign-extended 16-bit value from effective memory halfword address",
            "100001 ttttt fffff ssssssssssssssss",
            statement -> {
                try {
                    Processor.setValue(statement.getOperand(0), Memory.getInstance().fetchHalfword(Processor.getValue(statement.getOperand(2)) + (statement.getOperand(1) << 16 >> 16), true) << 16 >> 16);
                }
                catch (AddressErrorException exception) {
                    throw new SimulatorException(statement, exception);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "lhu",
            List.of(OperandType.REGISTER, OperandType.INTEGER_16_SIGNED, OperandType.PAREN_REGISTER),
            InstructionFormat.I_TYPE,
            false,
            "Load Halfword Unsigned",
            "Set {0} to zero-extended 16-bit value from effective memory halfword address",
            "100101 ttttt fffff ssssssssssssssss",
            statement -> {
                try {
                    // offset is sign-extended and loaded halfword value is zero-extended
                    Processor.setValue(statement.getOperand(0), Memory.getInstance().fetchHalfword(Processor.getValue(statement.getOperand(2)) + (statement.getOperand(1) << 16 >> 16), true));
                }
                catch (AddressErrorException exception) {
                    throw new SimulatorException(statement, exception);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "sb",
            List.of(OperandType.REGISTER, OperandType.INTEGER_16_SIGNED, OperandType.PAREN_REGISTER),
            InstructionFormat.I_TYPE,
            false,
            "Store Byte",
            "Store the low-order 8 bits of {0} into the effective memory byte address",
            "101000 ttttt fffff ssssssssssssssss",
            statement -> {
                try {
                    Memory.getInstance().storeByte(Processor.getValue(statement.getOperand(2)) + (statement.getOperand(1) << 16 >> 16), Processor.getValue(statement.getOperand(0)), true);
                }
                catch (AddressErrorException exception) {
                    throw new SimulatorException(statement, exception);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "sh",
            List.of(OperandType.REGISTER, OperandType.INTEGER_16_SIGNED, OperandType.PAREN_REGISTER),
            InstructionFormat.I_TYPE,
            false,
            "Store Halfword",
            "Store the low-order 16 bits of {0} into the effective memory halfword address",
            "101001 ttttt fffff ssssssssssssssss",
            statement -> {
                try {
                    Memory.getInstance().storeHalfword(Processor.getValue(statement.getOperand(2)) + (statement.getOperand(1) << 16 >> 16), Processor.getValue(statement.getOperand(0)), true);
                }
                catch (AddressErrorException exception) {
                    throw new SimulatorException(statement, exception);
                }
            }
        ));
        // MIPS32 requires rd (first) operand to appear twice in machine code.
        // It has to be same as rt (third) operand in machine code, but the
        // source statement does not have or permit third operand.
        // In the machine code, rd and rt are adjacent, but my mask
        // substitution cannot handle adjacent placement of the same source
        // operand (e.g. "... sssss fffff fffff ...") because it would interpret
        // the mask to be the total length of both (10 bits).  I could code it
        // to have 3 statement then define a pseudo-instruction of two statement
        // to translate into this, but then both would show up in instruction set
        // list and I don't want that.  So I will use the convention of Computer
        // Organization and Design 3rd Edition, Appendix A, and code the rt bits
        // as 0's.  The generated code does not match SPIM and would not run
        // on a real MIPS machine but since I am providing no means of storing
        // the binary code that is not really an issue.
        this.addBasicInstruction(new BasicInstruction(
            "clo",
            List.of(OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "Count Leading Ones",
            "Set {0} to the count of leading one bits in {1} starting at most significant bit position",
            "011100 sssss 00000 fffff 00000 100001",
            statement -> {
                // Invert and count leading zeroes
                Processor.setValue(statement.getOperand(0), Integer.numberOfLeadingZeros(~Processor.getValue(statement.getOperand(1))));
            }
        ));
        // See comments for "clo" instruction above.  They apply here too.
        this.addBasicInstruction(new BasicInstruction(
            "clz",
            List.of(OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "Count Leading Zeroes",
            "Set {0} to the count of leading zero bits in {1} starting at most significant bit position",
            "011100 sssss 00000 fffff 00000 100000",
            statement -> {
                Processor.setValue(statement.getOperand(0), Integer.numberOfLeadingZeros(Processor.getValue(statement.getOperand(1))));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "mfc0",
            List.of(OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "Move From Coprocessor 0",
            "Set {0} to the value stored in Coprocessor 0 register {1}",
            "010000 00000 fffff sssss 00000 000000",
            statement -> {
                Processor.setValue(statement.getOperand(0), Coprocessor0.getValue(statement.getOperand(1)));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "mtc0",
            List.of(OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false,
            "Move To Coprocessor 0",
            "Set Coprocessor 0 register {1} to value stored in {0}",
            "010000 00100 fffff sssss 00000 000000",
            statement -> {
                Coprocessor0.updateRegister(statement.getOperand(1), Processor.getValue(statement.getOperand(0)));
            }
        ));

        /////////////////////// Floating Point Instructions Start Here ////////////////

        this.addBasicInstruction(new BasicInstruction(
            "add.s",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "ADDition, Single-precision",
            "Set {0} to single-precision floating point value of {1} plus {2}",
            "010001 10000 ttttt sssss fffff 000000",
            statement -> {
                Coprocessor1.setSingleFloat(statement.getOperand(0), Coprocessor1.getSingleFloat(statement.getOperand(1)) + Coprocessor1.getSingleFloat(statement.getOperand(2)));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "add.d",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "ADDition, Double-precision",
            "Set {0} to double-precision floating point value of {1} plus {2}",
            "010001 10001 ttttt sssss fffff 000000",
            statement -> {
                try {
                    Coprocessor1.setDoubleFloat(statement.getOperand(0), Coprocessor1.getDoubleFloat(statement.getOperand(1)) + Coprocessor1.getDoubleFloat(statement.getOperand(2)));
                }
                catch (InvalidRegisterAccessException exception) {
                    throw new SimulatorException(statement, "all registers must be even-numbered");
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "sub.s",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "SUBtraction, Single-precision",
            "Set {0} to single-precision floating point value of {1} minus {2}",
            "010001 10000 ttttt sssss fffff 000001",
            statement -> {
                Coprocessor1.setSingleFloat(statement.getOperand(0), Coprocessor1.getSingleFloat(statement.getOperand(1)) - Coprocessor1.getSingleFloat(statement.getOperand(2)));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "sub.d",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "SUBtraction, Double-precision",
            "Set {0} to double-precision floating point value of {1} minus {2}",
            "010001 10001 ttttt sssss fffff 000001",
            statement -> {
                try {
                    Coprocessor1.setDoubleFloat(statement.getOperand(0), Coprocessor1.getDoubleFloat(statement.getOperand(1)) - Coprocessor1.getDoubleFloat(statement.getOperand(2)));
                }
                catch (InvalidRegisterAccessException exception) {
                    throw new SimulatorException(statement, "all registers must be even-numbered");
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "mul.s",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "MULtiplication, Single-precision",
            "Set {0} to single-precision floating point value of {1} multiplied by {2}",
            "010001 10000 ttttt sssss fffff 000010",
            statement -> {
                Coprocessor1.setSingleFloat(statement.getOperand(0), Coprocessor1.getSingleFloat(statement.getOperand(1)) * Coprocessor1.getSingleFloat(statement.getOperand(2)));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "mul.d",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "MULtiplication, Double-precision",
            "Set {0} to double-precision floating point value of {1} multiplied by {2}",
            "010001 10001 ttttt sssss fffff 000010",
            statement -> {
                try {
                    Coprocessor1.setDoubleFloat(statement.getOperand(0), Coprocessor1.getDoubleFloat(statement.getOperand(1)) * Coprocessor1.getDoubleFloat(statement.getOperand(2)));
                }
                catch (InvalidRegisterAccessException exception) {
                    throw new SimulatorException(statement, "all registers must be even-numbered");
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "div.s",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "DIVision, Single-precision",
            "Set {0} to single-precision floating point value of {1} divided by {2}",
            "010001 10000 ttttt sssss fffff 000011",
            statement -> {
                Coprocessor1.setSingleFloat(statement.getOperand(0), Coprocessor1.getSingleFloat(statement.getOperand(1)) / Coprocessor1.getSingleFloat(statement.getOperand(2)));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "div.d",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "DIVision, Double-precision",
            "Set {0} to double-precision floating point value of {1} divided by {2}",
            "010001 10001 ttttt sssss fffff 000011",
            statement -> {
                try {
                    Coprocessor1.setDoubleFloat(statement.getOperand(0), Coprocessor1.getDoubleFloat(statement.getOperand(1)) / Coprocessor1.getDoubleFloat(statement.getOperand(2)));
                }
                catch (InvalidRegisterAccessException exception) {
                    throw new SimulatorException(statement, "all registers must be even-numbered");
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "neg.s",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "NEGation, Single-precision",
            "Set single-precision {0} to negation of single-precision value in {1}",
            "010001 10000 00000 sssss fffff 000111",
            statement -> {
                // Flip the sign bit
                Coprocessor1.setValue(statement.getOperand(0), Coprocessor1.getValue(statement.getOperand(1)) ^ Integer.MIN_VALUE);
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "neg.d",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "NEGation, Double-precision",
            "Set double-precision {0} to negation of double-precision value in {1}",
            "010001 10001 00000 sssss fffff 000111",
            statement -> {
                try {
                    // Flip the sign bit
                    Coprocessor1.setPairValue(statement.getOperand(0), Coprocessor1.getPairValue(statement.getOperand(1)) ^ Long.MIN_VALUE);
                }
                catch (InvalidRegisterAccessException exception) {
                    throw new SimulatorException(statement, "both registers must be even-numbered");
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "abs.s",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "ABSolute value, Single-precision",
            "Set {0} to absolute value of {1}, single-precision",
            "010001 10000 00000 sssss fffff 000101",
            statement -> {
                // Clear the sign bit
                Coprocessor1.setValue(statement.getOperand(0), Coprocessor1.getValue(statement.getOperand(1)) & Integer.MAX_VALUE);
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "abs.d",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "ABSolute value, Double-precision",
            "Set {0} to absolute value of {1}, double-precision",
            "010001 10001 00000 sssss fffff 000101",
            statement -> {
                try {
                    // Clear the sign bit
                    Coprocessor1.setPairValue(statement.getOperand(0), Coprocessor1.getPairValue(statement.getOperand(1)) & Long.MAX_VALUE);
                }
                catch (InvalidRegisterAccessException exception) {
                    throw new SimulatorException(statement, "both registers must be even-numbered");
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "sqrt.s",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "SQuare RooT, Single-precision",
            "Set {0} to single-precision floating point square root of {1}",
            "010001 10000 00000 sssss fffff 000100",
            statement -> {
                // This is subject to refinement later.  Release 4.0 defines floor, ceil, trunc, round
                // to act silently rather than raise Invalid Operation exception, so sqrt should do the
                // same.  An intermediate step would be to define a setting for FCSR Invalid Operation
                // flag, but the best solution is to simulate the FCSR register itself.
                // FCSR = Floating point unit Control and Status Register.  DPS 10-Aug-2010
                Coprocessor1.setSingleFloat(statement.getOperand(0), (float) Math.sqrt(Coprocessor1.getSingleFloat(statement.getOperand(1))));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "sqrt.d",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "SQuare RooT, Double-precision",
            "Set {0} to double-precision floating point square root of {1}",
            "010001 10001 00000 sssss fffff 000100",
            statement -> {
                // This is subject to refinement later.  Release 4.0 defines floor, ceil, trunc, round
                // to act silently rather than raise Invalid Operation exception, so sqrt should do the
                // same.  An intermediate step would be to define a setting for FCSR Invalid Operation
                // flag, but the best solution is to simulate the FCSR register itself.
                // FCSR = Floating point unit Control and Status Register.  DPS 10-Aug-2010
                try {
                    Coprocessor1.setDoubleFloat(statement.getOperand(0), Math.sqrt(Coprocessor1.getDoubleFloat(statement.getOperand(1))));
                }
                catch (InvalidRegisterAccessException exception) {
                    throw new SimulatorException(statement, "both registers must be even-numbered");
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "floor.w.s",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "FLOOR to Word from Single-precision",
            "Set {0} to 32-bit integer floor of single-precision float in {1}",
            "010001 10000 00000 sssss fffff 001111",
            statement -> {
                float value = Coprocessor1.getSingleFloat(statement.getOperand(1));
                int result;
                if (value >= (double) Integer.MIN_VALUE && value < (double) Integer.MAX_VALUE + 1.0) {
                    result = (int) Math.floor(value);
                }
                else {
                    // DPS 27-July-2010: Since MARS does not simulate the FSCR, I will take the default
                    // action of setting the result to 2^31-1, if the value is outside the 32 bit range.
                    result = Integer.MAX_VALUE;
                }
                Coprocessor1.setValue(statement.getOperand(0), result);
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "floor.w.d",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "FLOOR to Word from Double-precision",
            "Set {0} to 32-bit integer floor of double-precision float in {1}",
            "010001 10001 00000 sssss fffff 001111",
            statement -> {
                try {
                    double value = Coprocessor1.getDoubleFloat(statement.getOperand(1));
                    int result;
                    if (value >= (double) Integer.MIN_VALUE && value < (double) Integer.MAX_VALUE + 1.0) {
                        result = (int) Math.floor(value);
                    }
                    else {
                        // DPS 27-July-2010: Since MARS does not simulate the FSCR, I will take the default
                        // action of setting the result to 2^31-1, if the value is outside the 32 bit range.
                        result = Integer.MAX_VALUE;
                    }
                    Coprocessor1.setValue(statement.getOperand(0), result);
                }
                catch (InvalidRegisterAccessException exception) {
                    throw new SimulatorException(statement, "second register must be even-numbered");
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "ceil.w.s",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "CEILing to Word from Single-precision",
            "Set {0} to 32-bit integer ceiling of single-precision float in {1}",
            "010001 10000 00000 sssss fffff 001110",
            statement -> {
                float value = Coprocessor1.getSingleFloat(statement.getOperand(1));
                int result;
                if (value > (double) Integer.MIN_VALUE - 1.0 && value <= (double) Integer.MAX_VALUE) {
                    result = (int) Math.ceil(value);
                }
                else {
                    // DPS 27-July-2010: Since MARS does not simulate the FSCR, I will take the default
                    // action of setting the result to 2^31-1, if the value is outside the 32 bit range.
                    result = Integer.MAX_VALUE;
                }
                Coprocessor1.setValue(statement.getOperand(0), result);
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "ceil.w.d",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "CEILing to Word from Double-precision",
            "Set {0} to 32-bit integer ceiling of double-precision float in {1}",
            "010001 10001 00000 sssss fffff 001110",
            statement -> {
                try {
                    double value = Coprocessor1.getDoubleFloat(statement.getOperand(1));
                    int result;
                    if (value > (double) Integer.MIN_VALUE - 1.0 && value <= (double) Integer.MAX_VALUE) {
                        result = (int) Math.ceil(value);
                    }
                    else {
                        // DPS 27-July-2010: Since MARS does not simulate the FSCR, I will take the default
                        // action of setting the result to 2^31-1, if the value is outside the 32 bit range.
                        result = Integer.MAX_VALUE;
                    }
                    Coprocessor1.setValue(statement.getOperand(0), result);
                }
                catch (InvalidRegisterAccessException exception) {
                    throw new SimulatorException(statement, "second register must be even-numbered");
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "round.w.s",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "ROUND to Word from Single-precision",
            "Set {0} to 32-bit integer rounding of single-precision float in {1}",
            "010001 10000 00000 sssss fffff 001100",
            statement -> {
                // MIPS32 documentation (and IEEE 754) states that round rounds to the nearest but when
                // both are equally near it rounds to the even one!  SPIM rounds -4.5, -5.5,
                // 4.5 and 5.5 to (-4, -5, 5, 6).  Curiously, it rounds -5.1 to -4 and -5.6 to -5.
                // Until MARS 3.5, I used Math.round, which rounds to nearest but when both are
                // equal it rounds toward positive infinity.  With Release 3.5, I painstakingly
                // carry out the MIPS and IEEE 754 standard.
                float value = Coprocessor1.getSingleFloat(statement.getOperand(1));
                // According to MIPS32 spec, if any of these conditions is true, set
                // Invalid Operation in the FCSR (Floating point Control/Status Register) and
                // set result to be 2^31-1.  MARS does not implement this register (as of release 3.4.1).
                // It also mentions the "Invalid Operation Enable bit" in FCSR, that, if set, results
                // in immediate exception instead of default value.
                int result;
                if (value >= (double) Integer.MIN_VALUE - 0.5 && value < (double) Integer.MAX_VALUE + 0.5) {
                    // If we are EXACTLY in the middle, then round to even!  To determine this,
                    // find next higher integer and next lower integer, then see if distances
                    // are exactly equal.
                    int above;
                    int below;
                    if (value < 0.0f) {
                        above = (int) value; // Truncates
                        below = above - 1;
                    }
                    else {
                        below = (int) value; // Truncates
                        above = below + 1;
                    }
                    if (value - below == above - value) { // Exactly in the middle?
                        result = (above % 2 == 0) ? above : below;
                    }
                    else {
                        result = Math.round(value);
                    }
                }
                else {
                    // DPS 27-July-2010: Since MARS does not simulate the FSCR, I will take the default
                    // action of setting the result to 2^31-1, if the value is outside the 32 bit range.
                    result = Integer.MAX_VALUE;
                }
                Coprocessor1.setValue(statement.getOperand(0), result);
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "round.w.d",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "ROUND to Word from Double-precision",
            "Set {0} to 32-bit integer rounding of double-precision float in {1}",
            "010001 10001 00000 sssss fffff 001100",
            statement -> {
                // See comments in round.w.s above, concerning MIPS and IEEE 754 standard.
                // Until MARS 3.5, I used Math.round, which rounds to nearest but when both are
                // equal it rounds toward positive infinity.  With Release 3.5, I painstakingly
                // carry out the MIPS and IEEE 754 standard (round to nearest/even).
                try {
                    double value = Coprocessor1.getDoubleFloat(statement.getOperand(1));
                    int result;
                    if (value >= (double) Integer.MIN_VALUE - 0.5 && value < (double) Integer.MAX_VALUE + 0.5) {
                        // If we are EXACTLY in the middle, then round to even!  To determine this,
                        // find next higher integer and next lower integer, then see if distances
                        // are exactly equal.
                        int above;
                        int below;
                        if (value < 0.0) {
                            above = (int) value; // Truncates
                            below = above - 1;
                        }
                        else {
                            below = (int) value; // Truncates
                            above = below + 1;
                        }
                        if (value - below == above - value) { // Exactly in the middle?
                            result = (above % 2 == 0) ? above : below;
                        }
                        else {
                            result = (int) Math.round(value);
                        }
                    }
                    else {
                        // DPS 27-July-2010: Since MARS does not simulate the FSCR, I will take the default
                        // action of setting the result to 2^31-1, if the value is outside the 32 bit range.
                        result = Integer.MAX_VALUE;
                    }
                    Coprocessor1.setValue(statement.getOperand(0), result);
                }
                catch (InvalidRegisterAccessException exception) {
                    throw new SimulatorException(statement, "second register must be even-numbered");
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "trunc.w.s",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "TRUNCate to Word from Single-precision",
            "Set {0} to 32-bit integer truncation of single-precision float in {1}",
            "010001 10000 00000 sssss fffff 001101",
            statement -> {
                float value = Coprocessor1.getSingleFloat(statement.getOperand(1));
                int result;
                if (value > (double) Integer.MIN_VALUE - 1.0 && value < (double) Integer.MAX_VALUE + 1.0) {
                    result = (int) value; // Typecasting will round toward zero, the correct action
                }
                else {
                    // DPS 27-July-2010: Since MARS does not simulate the FSCR, I will take the default
                    // action of setting the result to 2^31-1, if the value is outside the 32 bit range.
                    result = Integer.MAX_VALUE;
                }
                Coprocessor1.setValue(statement.getOperand(0), result);
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "trunc.w.d",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "TRUNCate to Word from Double-precision",
            "Set {0} to 32-bit integer truncation of double-precision float in {1}",
            "010001 10001 00000 sssss fffff 001101", statement -> {
                try {
                    double value = Coprocessor1.getDoubleFloat(statement.getOperand(1));
                    int result;
                    if (value > (double) Integer.MIN_VALUE - 1.0 && value < (double) Integer.MAX_VALUE + 1.0) {
                        result = (int) value; // Typecasting will round toward zero, the correct action
                    }
                    else {
                        // DPS 27-July-2010: Since MARS does not simulate the FSCR, I will take the default
                        // action of setting the result to 2^31-1, if the value is outside the 32 bit range.
                        result = Integer.MAX_VALUE;
                    }
                    Coprocessor1.setValue(statement.getOperand(0), result);
                }
                catch (InvalidRegisterAccessException exception) {
                    throw new SimulatorException(statement, "second register must be even-numbered");
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "c.eq.s",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "Compare EQual, Single-precision",
            "If {0} is equal to {1}, set Coprocessor 1 condition flag 0 to true, otherwise set it to false",
            "010001 10000 sssss fffff 000 00 110010",
            statement -> {
                if (Coprocessor1.getSingleFloat(statement.getOperand(0)) == Coprocessor1.getSingleFloat(statement.getOperand(1))) {
                    Coprocessor1.setConditionFlag(0);
                }
                else {
                    Coprocessor1.clearConditionFlag(0);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "c.eq.s",
            List.of(OperandType.INTEGER_3_UNSIGNED, OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "Compare EQual, Single-precision",
            "If {0} is equal to {1}, set Coprocessor 1 condition flag specified by the first operand to true, otherwise set it to false",
            "010001 10000 ttttt sssss fff 00 110010",
            statement -> {
                if (Coprocessor1.getSingleFloat(statement.getOperand(1)) == Coprocessor1.getSingleFloat(statement.getOperand(2))) {
                    Coprocessor1.setConditionFlag(statement.getOperand(0));
                }
                else {
                    Coprocessor1.clearConditionFlag(statement.getOperand(0));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "c.eq.d",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "Compare EQual, Double-precision",
            "If {0} is equal to {1} (double-precision), set Coprocessor 1 condition flag 0 to true, otherwise set it to false",
            "010001 10001 sssss fffff 000 00 110010",
            statement -> {
                try {
                    if (Coprocessor1.getDoubleFloat(statement.getOperand(0)) == Coprocessor1.getDoubleFloat(statement.getOperand(1))) {
                        Coprocessor1.setConditionFlag(0);
                    }
                    else {
                        Coprocessor1.clearConditionFlag(0);
                    }
                }
                catch (InvalidRegisterAccessException exception) {
                    throw new SimulatorException(statement, "both registers must be even-numbered");
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "c.eq.d",
            List.of(OperandType.INTEGER_3_UNSIGNED, OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "Compare EQual, Double-precision",
            "If {0} is equal to {1} (double-precision), set Coprocessor 1 condition flag specified by the first operand to true, otherwise set it to false",
            "010001 10001 ttttt sssss fff 00 110010",
            statement -> {
                try {
                    if (Coprocessor1.getDoubleFloat(statement.getOperand(1)) == Coprocessor1.getDoubleFloat(statement.getOperand(2))) {
                        Coprocessor1.setConditionFlag(statement.getOperand(0));
                    }
                    else {
                        Coprocessor1.clearConditionFlag(statement.getOperand(0));
                    }
                }
                catch (InvalidRegisterAccessException exception) {
                    throw new SimulatorException(statement, "both registers must be even-numbered");
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "c.le.s",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "Compare Less than or Equal, Single-precision",
            "If {0} is less than or equal to {1}, set Coprocessor 1 condition flag 0 to true, otherwise set it to false",
            "010001 10000 sssss fffff 000 00 111110",
            statement -> {
                if (Coprocessor1.getSingleFloat(statement.getOperand(0)) <= Coprocessor1.getSingleFloat(statement.getOperand(1))) {
                    Coprocessor1.setConditionFlag(0);
                }
                else {
                    Coprocessor1.clearConditionFlag(0);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "c.le.s",
            List.of(OperandType.INTEGER_3_UNSIGNED, OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "Compare Less than or Equal, Single-precision",
            "If {0} is less than or equal to {1}, set Coprocessor 1 condition flag specified by the first operand to true, otherwise set it to false",
            "010001 10000 ttttt sssss fff 00 111110",
            statement -> {
                if (Coprocessor1.getSingleFloat(statement.getOperand(1)) <= Coprocessor1.getSingleFloat(statement.getOperand(2))) {
                    Coprocessor1.setConditionFlag(statement.getOperand(0));
                }
                else {
                    Coprocessor1.clearConditionFlag(statement.getOperand(0));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "c.le.d",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "Compare Less than or Equal, Double-precision",
            "If {0} is less than or equal to {1} (double-precision), set Coprocessor 1 condition flag 0 to true, otherwise set it to false",
            "010001 10001 sssss fffff 000 00 111110",
            statement -> {
                try {
                    if (Coprocessor1.getDoubleFloat(statement.getOperand(0)) <= Coprocessor1.getDoubleFloat(statement.getOperand(1))) {
                        Coprocessor1.setConditionFlag(0);
                    }
                    else {
                        Coprocessor1.clearConditionFlag(0);
                    }
                }
                catch (InvalidRegisterAccessException exception) {
                    throw new SimulatorException(statement, "both registers must be even-numbered");
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "c.le.d",
            List.of(OperandType.INTEGER_3_UNSIGNED, OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "Compare Less than or Equal, Double-precision",
            "If {0} is less than or equal to {1} (double-precision), set Coprocessor 1 condition flag specified by the first operand to true, otherwise set it to false",
            "010001 10001 ttttt sssss fff 00 111110",
            statement -> {
                try {
                    if (Coprocessor1.getDoubleFloat(statement.getOperand(1)) <= Coprocessor1.getDoubleFloat(statement.getOperand(2))) {
                        Coprocessor1.setConditionFlag(statement.getOperand(0));
                    }
                    else {
                        Coprocessor1.clearConditionFlag(statement.getOperand(0));
                    }
                }
                catch (InvalidRegisterAccessException exception) {
                    throw new SimulatorException(statement, "both registers must be even-numbered");
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "c.lt.s",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "Compare Less Than, Single-precision",
            "If {0} is less than {1}, set Coprocessor 1 condition flag 0 to true, otherwise set it to false",
            "010001 10000 sssss fffff 000 00 111100",
            statement -> {
                if (Coprocessor1.getSingleFloat(statement.getOperand(0)) < Coprocessor1.getSingleFloat(statement.getOperand(1))) {
                    Coprocessor1.setConditionFlag(0);
                }
                else {
                    Coprocessor1.clearConditionFlag(0);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "c.lt.s",
            List.of(OperandType.INTEGER_3_UNSIGNED, OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "Compare Less Than, Single-precision",
            "If {0} is less than {1}, set Coprocessor 1 condition flag specified by the first operand to true, otherwise set it to false",
            "010001 10000 ttttt sssss fff 00 111100",
            statement -> {
                if (Coprocessor1.getSingleFloat(statement.getOperand(1)) < Coprocessor1.getSingleFloat(statement.getOperand(2))) {
                    Coprocessor1.setConditionFlag(statement.getOperand(0));
                }
                else {
                    Coprocessor1.clearConditionFlag(statement.getOperand(0));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "c.lt.d",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "Compare Less Than, Double-precision",
            "If {0} is less than {1} (double-precision), set Coprocessor 1 condition flag 0 to true, otherwise set it to false",
            "010001 10001 sssss fffff 000 00 111100",
            statement -> {
                try {
                    if (Coprocessor1.getDoubleFloat(statement.getOperand(0)) < Coprocessor1.getDoubleFloat(statement.getOperand(1))) {
                        Coprocessor1.setConditionFlag(0);
                    }
                    else {
                        Coprocessor1.clearConditionFlag(0);
                    }
                }
                catch (InvalidRegisterAccessException exception) {
                    throw new SimulatorException(statement, "both registers must be even-numbered");
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "c.lt.d",
            List.of(OperandType.INTEGER_3_UNSIGNED, OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "Compare Less Than, Double-precision",
            "If {0} is less than {1} (double-precision), set Coprocessor 1 condition flag specified by the first operand to true, otherwise set it to false",
            "010001 10001 ttttt sssss fff 00 111100",
            statement -> {
                try {
                    if (Coprocessor1.getDoubleFloat(statement.getOperand(1)) < Coprocessor1.getDoubleFloat(statement.getOperand(2))) {
                        Coprocessor1.setConditionFlag(statement.getOperand(0));
                    }
                    else {
                        Coprocessor1.clearConditionFlag(statement.getOperand(0));
                    }
                }
                catch (InvalidRegisterAccessException exception) {
                    throw new SimulatorException(statement, "both registers must be even-numbered");
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "bc1f",
            List.of(OperandType.BRANCH_OFFSET),
            InstructionFormat.FI_TYPE_BRANCH,
            true,
            "Branch if Coprocessor 1 condition flag False (BC1F, not BCLF)",
            "Branch to statement at label's address only if Coprocessor 1 condition flag 0 is false (bit value 0)",
            "010001 01000 000 00 ffffffffffffffff",
            statement -> {
                if (Coprocessor1.getConditionFlag(0) == 0) {
                    this.processBranch(statement.getOperand(0));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "bc1f",
            List.of(OperandType.INTEGER_3_UNSIGNED, OperandType.BRANCH_OFFSET),
            InstructionFormat.FI_TYPE_BRANCH,
            true,
            "Branch if Coprocessor 1 condition flag False (BC1F, not BCLF)",
            "Branch to statement at label's address only if Coprocessor 1 condition flag specified by the first operand is false (bit value 0)",
            "010001 01000 fff 00 ssssssssssssssss",
            statement -> {
                if (Coprocessor1.getConditionFlag(statement.getOperand(0)) == 0) {
                    this.processBranch(statement.getOperand(1));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "bc1t",
            List.of(OperandType.BRANCH_OFFSET),
            InstructionFormat.FI_TYPE_BRANCH,
            true,
            "Branch if Coprocessor 1 condition flag True (BC1T, not BCLT)",
            "Branch to statement at label's address only if Coprocessor 1 condition flag 0 is true (bit value 1)",
            "010001 01000 000 01 ffffffffffffffff",
            statement -> {
                if (Coprocessor1.getConditionFlag(0) == 1) {
                    this.processBranch(statement.getOperand(0));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "bc1t",
            List.of(OperandType.INTEGER_3_UNSIGNED, OperandType.BRANCH_OFFSET),
            InstructionFormat.FI_TYPE_BRANCH,
            true,
            "Branch if Coprocessor 1 condition flag True (BC1T, not BCLT)",
            "Branch to statement at label's address only if Coprocessor 1 condition flag specified by the first operand is true (bit value 1)",
            "010001 01000 fff 01 ssssssssssssssss",
            statement -> {
                if (Coprocessor1.getConditionFlag(statement.getOperand(0)) == 1) {
                    this.processBranch(statement.getOperand(1));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "cvt.s.d",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "ConVerT to Single-precision from Double-precision",
            "Set {0} to single-precision equivalent of double-precision value in {1}",
            "010001 10001 00000 sssss fffff 100000",
            statement -> {
                try {
                    Coprocessor1.setSingleFloat(statement.getOperand(0), (float) Coprocessor1.getDoubleFloat(statement.getOperand(1)));
                }
                catch (InvalidRegisterAccessException exception) {
                    throw new SimulatorException(statement, "second register must be even-numbered");
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "cvt.d.s",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "ConVerT to Double-precision from Single-precision",
            "Set {0} to double-precision equivalent of single-precision value in {1}",
            "010001 10000 00000 sssss fffff 100001",
            statement -> {
                try {
                    Coprocessor1.setDoubleFloat(statement.getOperand(0), Coprocessor1.getSingleFloat(statement.getOperand(1)));
                }
                catch (InvalidRegisterAccessException exception) {
                    throw new SimulatorException(statement, "first register must be even-numbered");
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "cvt.w.s",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "ConVerT to Word from Single-precision",
            "Set {0} to 32-bit integer equivalent of single-precision value in {1}",
            "010001 10000 00000 sssss fffff 100100",
            statement -> {
                Coprocessor1.setValue(statement.getOperand(0), (int) Coprocessor1.getSingleFloat(statement.getOperand(1)));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "cvt.w.d",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "ConVerT to Word from Double-precision",
            "Set {0} to 32-bit integer equivalent of double-precision value in {1}",
            "010001 10001 00000 sssss fffff 100100",
            statement -> {
                try {
                    Coprocessor1.setValue(statement.getOperand(0), (int) Coprocessor1.getDoubleFloat(statement.getOperand(1)));
                }
                catch (InvalidRegisterAccessException exception) {
                    throw new SimulatorException(statement, "second register must be even-numbered");
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "cvt.s.w",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "ConVerT to Single-precision from Word",
            "Set {0} to single-precision equivalent of 32-bit integer value in {1}",
            "010001 10100 00000 sssss fffff 100000",
            statement -> {
                Coprocessor1.setSingleFloat(statement.getOperand(0), (float) Coprocessor1.getValue(statement.getOperand(1)));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "cvt.d.w",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "ConVerT to Double-precision from Word",
            "Set {0} to double-precision equivalent of 32-bit integer value in {1}",
            "010001 10100 00000 sssss fffff 100001",
            statement -> {
                try {
                    Coprocessor1.setDoubleFloat(statement.getOperand(0), Coprocessor1.getValue(statement.getOperand(1)));
                }
                catch (InvalidRegisterAccessException exception) {
                    throw new SimulatorException(statement, "first register must be even-numbered");
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "mov.s",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "MOVe, Single-precision",
            "Set single-precision {0} to single-precision value in {1}",
            "010001 10000 00000 sssss fffff 000110",
            statement -> {
                Coprocessor1.setValue(statement.getOperand(0), Coprocessor1.getValue(statement.getOperand(1)));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "mov.d",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "MOVe, Double-precision",
            "Set double-precision {0} to double-precision value in {1}",
            "010001 10001 00000 sssss fffff 000110",
            statement -> {
                if (statement.getOperand(0) % 2 == 1 || statement.getOperand(1) % 2 == 1) {
                    throw new SimulatorException(statement, "both registers must be even-numbered");
                }
                Coprocessor1.setValue(statement.getOperand(0), Coprocessor1.getValue(statement.getOperand(1)));
                Coprocessor1.setValue(statement.getOperand(0) + 1, Coprocessor1.getValue(statement.getOperand(1) + 1));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "movf.s",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "MOVe if condition flag False, Single-precision",
            "Set single-precision {0} to single-precision value in {1} only if condition flag 0 is false (bit value 0)",
            "010001 10000 000 00 sssss fffff 010001",
            statement -> {
                if (Coprocessor1.getConditionFlag(0) == 0) {
                    Coprocessor1.setValue(statement.getOperand(0), Coprocessor1.getValue(statement.getOperand(1)));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "movf.s",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER, OperandType.INTEGER_3_UNSIGNED),
            InstructionFormat.FR_TYPE,
            false,
            "MOVe if condition flag False, Single-precision",
            "Set single-precision {0} to single-precision value in {1} only if condition flag specified by the last operand is false (bit value 0)",
            "010001 10000 ttt 00 sssss fffff 010001",
            statement -> {
                if (Coprocessor1.getConditionFlag(statement.getOperand(2)) == 0) {
                    Coprocessor1.setValue(statement.getOperand(0), Coprocessor1.getValue(statement.getOperand(1)));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "movf.d",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "MOVe if condition flag False, Double-precision",
            "Set double-precision {0} to double-precision value in {1} only if condition flag 0 is false (bit value 0)",
            "010001 10001 000 00 sssss fffff 010001",
            statement -> {
                if (statement.getOperand(0) % 2 == 1 || statement.getOperand(1) % 2 == 1) {
                    throw new SimulatorException(statement, "both registers must be even-numbered");
                }
                if (Coprocessor1.getConditionFlag(0) == 0) {
                    Coprocessor1.setValue(statement.getOperand(0), Coprocessor1.getValue(statement.getOperand(1)));
                    Coprocessor1.setValue(statement.getOperand(0) + 1, Coprocessor1.getValue(statement.getOperand(1) + 1));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "movf.d",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER, OperandType.INTEGER_3_UNSIGNED),
            InstructionFormat.FR_TYPE,
            false,
            "MOVe if condition flag False, Double-precision",
            "Set double-precision {0} to double-precision value in {1} only if condition flag specified by the last operand is false (bit value 0)",
            "010001 10001 ttt 00 sssss fffff 010001",
            statement -> {
                if (statement.getOperand(0) % 2 == 1 || statement.getOperand(1) % 2 == 1) {
                    throw new SimulatorException(statement, "both registers must be even-numbered");
                }
                if (Coprocessor1.getConditionFlag(statement.getOperand(2)) == 0) {
                    Coprocessor1.setValue(statement.getOperand(0), Coprocessor1.getValue(statement.getOperand(1)));
                    Coprocessor1.setValue(statement.getOperand(0) + 1, Coprocessor1.getValue(statement.getOperand(1) + 1));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "movt.s",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "MOVe if condition flag True, Single-precision",
            "Set single-precision {0} to single-precision value in {1} only if condition flag 0 is true (bit value 1)",
            "010001 10000 000 01 sssss fffff 010001",
            statement -> {
                if (Coprocessor1.getConditionFlag(0) == 1) {
                    Coprocessor1.setValue(statement.getOperand(0), Coprocessor1.getValue(statement.getOperand(1)));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "movt.s",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER, OperandType.INTEGER_3_UNSIGNED),
            InstructionFormat.FR_TYPE,
            false,
            "MOVe if condition flag True, Single-precision",
            "Set single-precision {0} to single-precision value in {1} only if condition flag specified by the last operand is true (bit value 1)",
            "010001 10000 ttt 01 sssss fffff 010001",
            statement -> {
                if (Coprocessor1.getConditionFlag(statement.getOperand(2)) == 1) {
                    Coprocessor1.setValue(statement.getOperand(0), Coprocessor1.getValue(statement.getOperand(1)));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "movt.d",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "MOVe if condition flag True, Double-precision",
            "Set double-precision {0} to double-precision value in {1} only if condition flag 0 is true (bit value 1)",
            "010001 10001 000 01 sssss fffff 010001",
            statement -> {
                if (statement.getOperand(0) % 2 == 1 || statement.getOperand(1) % 2 == 1) {
                    throw new SimulatorException(statement, "both registers must be even-numbered");
                }
                if (Coprocessor1.getConditionFlag(0) == 1) {
                    Coprocessor1.setValue(statement.getOperand(0), Coprocessor1.getValue(statement.getOperand(1)));
                    Coprocessor1.setValue(statement.getOperand(0) + 1, Coprocessor1.getValue(statement.getOperand(1) + 1));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "movt.d",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER, OperandType.INTEGER_3_UNSIGNED),
            InstructionFormat.FR_TYPE,
            false,
            "MOVe if condition flag True, Double-precision",
            "Set double-precision {0} to double-precision value in {1} only if condition flag specified by the last operand is true (bit value 1)",
            "010001 10001 ttt 01 sssss fffff 010001",
            statement -> {
                if (statement.getOperand(0) % 2 == 1 || statement.getOperand(1) % 2 == 1) {
                    throw new SimulatorException(statement, "both registers must be even-numbered");
                }
                if (Coprocessor1.getConditionFlag(statement.getOperand(2)) == 1) {
                    Coprocessor1.setValue(statement.getOperand(0), Coprocessor1.getValue(statement.getOperand(1)));
                    Coprocessor1.setValue(statement.getOperand(0) + 1, Coprocessor1.getValue(statement.getOperand(1) + 1));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "movn.s",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER, OperandType.REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "MOVe if Not zero, Single-precision",
            "Set single-precision {0} to single-precision value in {1} only if {2} is not zero",
            "010001 10000 ttttt sssss fffff 010011",
            statement -> {
                if (Processor.getValue(statement.getOperand(2)) != 0) {
                    Coprocessor1.setValue(statement.getOperand(0), Coprocessor1.getValue(statement.getOperand(1)));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "movn.d",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER, OperandType.REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "MOVe if Not zero, Double-precision",
            "Set double-precision {0} to double-precision value in {1} only if {2} is not zero",
            "010001 10001 ttttt sssss fffff 010011",
            statement -> {
                if (statement.getOperand(0) % 2 == 1 || statement.getOperand(1) % 2 == 1) {
                    throw new SimulatorException(statement, "both registers must be even-numbered");
                }
                if (Processor.getValue(statement.getOperand(2)) != 0) {
                    Coprocessor1.setValue(statement.getOperand(0), Coprocessor1.getValue(statement.getOperand(1)));
                    Coprocessor1.setValue(statement.getOperand(0) + 1, Coprocessor1.getValue(statement.getOperand(1) + 1));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "movz.s",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER, OperandType.REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "MOVe if Zero, Single-precision",
            "Set single-precision {0} to single-precision value in {1} only if {2} is zero",
            "010001 10000 ttttt sssss fffff 010010",
            statement -> {
                if (Processor.getValue(statement.getOperand(2)) == 0) {
                    Coprocessor1.setValue(statement.getOperand(0), Coprocessor1.getValue(statement.getOperand(1)));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "movz.d",
            List.of(OperandType.FP_REGISTER, OperandType.FP_REGISTER, OperandType.REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "MOVe if Zero, Double-precision",
            "Set double-precision {0} to double-precision value in {1} only if {2} is zero",
            "010001 10001 ttttt sssss fffff 010010",
            statement -> {
                if (statement.getOperand(0) % 2 == 1 || statement.getOperand(1) % 2 == 1) {
                    throw new SimulatorException(statement, "both registers must be even-numbered");
                }
                if (Processor.getValue(statement.getOperand(2)) == 0) {
                    Coprocessor1.setValue(statement.getOperand(0), Coprocessor1.getValue(statement.getOperand(1)));
                    Coprocessor1.setValue(statement.getOperand(0) + 1, Coprocessor1.getValue(statement.getOperand(1) + 1));
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "mfc1",
            List.of(OperandType.REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "Move From Coprocessor 1 (FPU)",
            "Set {0} to value in Coprocessor 1 register {1}",
            "010001 00000 fffff sssss 00000 000000",
            statement -> {
                Processor.setValue(statement.getOperand(0), Coprocessor1.getValue(statement.getOperand(1)));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "mtc1",
            List.of(OperandType.REGISTER, OperandType.FP_REGISTER),
            InstructionFormat.FR_TYPE,
            false,
            "Move To Coprocessor 1 (FPU)",
            "Set Coprocessor 1 register {1} to value in {0}",
            "010001 00100 fffff sssss 00000 000000",
            statement -> {
                Coprocessor1.setValue(statement.getOperand(1), Processor.getValue(statement.getOperand(0)));
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "lwc1",
            List.of(OperandType.FP_REGISTER, OperandType.INTEGER_16_SIGNED, OperandType.PAREN_REGISTER),
            InstructionFormat.I_TYPE,
            false,
            "Load Word into Coprocessor 1 (FPU)",
            "Set {0} to 32-bit value from effective memory word address",
            "110001 ttttt fffff ssssssssssssssss",
            statement -> {
                try {
                    Coprocessor1.setValue(statement.getOperand(0), Memory.getInstance().fetchWord(Processor.getValue(statement.getOperand(2)) + statement.getOperand(1), true));
                }
                catch (AddressErrorException exception) {
                    throw new SimulatorException(statement, exception);
                }
            }
        ));
        // No printed reference, got opcode from SPIM
        this.addBasicInstruction(new BasicInstruction(
            "ldc1",
            List.of(OperandType.FP_REGISTER, OperandType.INTEGER_16_SIGNED, OperandType.PAREN_REGISTER),
            InstructionFormat.I_TYPE,
            false,
            "Load Doubleword into Coprocessor 1 (FPU)",
            "Set {0} to 64-bit value from effective memory doubleword address",
            "110101 ttttt fffff ssssssssssssssss",
            statement -> {
                try {
                    Coprocessor1.setPairValue(statement.getOperand(0), Memory.getInstance().fetchDoubleword(
                        Processor.getValue(statement.getOperand(2)) + statement.getOperand(1), true));
                }
                catch (AddressErrorException exception) {
                    throw new SimulatorException(statement, exception);
                }
                catch (InvalidRegisterAccessException exception) {
                    throw new SimulatorException(statement, "first register must be even-numbered");
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "swc1",
            List.of(OperandType.FP_REGISTER, OperandType.INTEGER_16_SIGNED, OperandType.PAREN_REGISTER),
            InstructionFormat.I_TYPE,
            false,
            "Store Word from Coprocessor 1 (FPU)",
            "Store 32-bit value in {0} at effective memory word address",
            "111001 ttttt fffff ssssssssssssssss",
            statement -> {
                try {
                    Memory.getInstance().storeWord(Processor.getValue(statement.getOperand(2)) + statement.getOperand(1), Coprocessor1.getValue(statement.getOperand(0)), true);
                }
                catch (AddressErrorException exception) {
                    throw new SimulatorException(statement, exception);
                }
            }
        ));
        // No printed reference, got opcode from SPIM
        this.addBasicInstruction(new BasicInstruction(
            "sdc1",
            List.of(OperandType.FP_REGISTER, OperandType.INTEGER_16_SIGNED, OperandType.PAREN_REGISTER),
            InstructionFormat.I_TYPE,
            false,
            "Store Doubleword from Coprocessor 1 (FPU)",
            "Store 64-bit value in {0} at effective memory doubleword address",
            "111101 ttttt fffff ssssssssssssssss",
            statement -> {
                try {
                    Memory.getInstance().storeDoubleword(Processor.getValue(statement.getOperand(2)) + statement.getOperand(1), Coprocessor1.getPairValue(statement.getOperand(0)), true);
                }
                catch (AddressErrorException exception) {
                    throw new SimulatorException(statement, exception);
                }
                catch (InvalidRegisterAccessException exception) {
                    throw new SimulatorException(statement, "first register must be even-numbered");
                }
            }
        ));

        ////////////////////////////  THE TRAP INSTRUCTIONS & ERET  ////////////////////////////

        this.addBasicInstruction(new BasicInstruction(
            "teq",
            List.of(OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false, // Throws an exception, does not jump through the typical mechanism
            "Trap if EQual",
            "Trap if {0} is equal to {1}",
            "000000 fffff sssss 00000 00000 110100",
            statement -> {
                if (Processor.getValue(statement.getOperand(0)) == Processor.getValue(statement.getOperand(1))) {
                    throw new SimulatorException(statement, "trap", ExceptionCause.TRAP);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "teqi",
            List.of(OperandType.REGISTER, OperandType.INTEGER_16_SIGNED),
            InstructionFormat.I_TYPE,
            false, // Throws an exception, does not jump through the typical mechanism
            "Trap if EQual to Immediate",
            "Trap if {0} is equal to sign-extended 16-bit immediate",
            "000001 fffff 01100 ssssssssssssssss",
            statement -> {
                if (Processor.getValue(statement.getOperand(0)) == (statement.getOperand(1) << 16 >> 16)) {
                    throw new SimulatorException(statement, "trap", ExceptionCause.TRAP);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "tne",
            List.of(OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false, // Throws an exception, does not jump through the typical mechanism
            "Trap if Not Equal",
            "Trap unless {0} is equal to {1}",
            "000000 fffff sssss 00000 00000 110110",
            statement -> {
                if (Processor.getValue(statement.getOperand(0)) != Processor.getValue(statement.getOperand(1))) {
                    throw new SimulatorException(statement, "trap", ExceptionCause.TRAP);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "tnei",
            List.of(OperandType.REGISTER, OperandType.INTEGER_16_SIGNED),
            InstructionFormat.I_TYPE,
            false, // Throws an exception, does not jump through the typical mechanism
            "Trap if Not Equal to Immediate",
            "Trap unless {0} is equal to sign-extended 16-bit immediate",
            "000001 fffff 01110 ssssssssssssssss",
            statement -> {
                if (Processor.getValue(statement.getOperand(0)) != (statement.getOperand(1) << 16 >> 16)) {
                    throw new SimulatorException(statement, "trap", ExceptionCause.TRAP);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "tge",
            List.of(OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false, // Throws an exception, does not jump through the typical mechanism
            "Trap if Greater than or Equal",
            "Trap if {0} is greater than or equal to {1}",
            "000000 fffff sssss 00000 00000 110000",
            statement -> {
                if (Processor.getValue(statement.getOperand(0)) >= Processor.getValue(statement.getOperand(1))) {
                    throw new SimulatorException(statement, "trap", ExceptionCause.TRAP);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "tgeu",
            List.of(OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false, // Throws an exception, does not jump through the typical mechanism
            "Trap if Greater than or Equal Unsigned",
            "Trap if {0} is greater than or equal to {1} using unsigned comparision",
            "000000 fffff sssss 00000 00000 110001",
            statement -> {
                if (Integer.compareUnsigned(Processor.getValue(statement.getOperand(0)), Processor.getValue(statement.getOperand(1))) >= 0) {
                    throw new SimulatorException(statement, "trap", ExceptionCause.TRAP);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "tgei",
            List.of(OperandType.REGISTER, OperandType.INTEGER_16_SIGNED),
            InstructionFormat.I_TYPE,
            false, // Throws an exception, does not jump through the typical mechanism
            "Trap if Greater than or Equal Immediate",
            "Trap if {0} is greater than or equal to sign-extended 16-bit immediate",
            "000001 fffff 01000 ssssssssssssssss",
            statement -> {
                if (Processor.getValue(statement.getOperand(0)) >= (statement.getOperand(1) << 16 >> 16)) {
                    throw new SimulatorException(statement, "trap", ExceptionCause.TRAP);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "tgeiu",
            List.of(OperandType.REGISTER, OperandType.INTEGER_16_SIGNED),
            InstructionFormat.I_TYPE,
            false, // Throws an exception, does not jump through the typical mechanism
            "Trap if Greater than or Equal Immediate Unsigned",
            "Trap if {0} is greater than or equal to sign-extended 16-bit immediate using unsigned comparison",
            "000001 fffff 01001 ssssssssssssssss",
            statement -> {
                if (Integer.compareUnsigned(Processor.getValue(statement.getOperand(0)), statement.getOperand(1) << 16 >> 16) >= 0) {
                    throw new SimulatorException(statement, "trap", ExceptionCause.TRAP);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "tlt",
            List.of(OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false, // Throws an exception, does not jump through the typical mechanism
            "Trap if Less Than",
            "Trap if {0} is less than {1}",
            "000000 fffff sssss 00000 00000 110010",
            statement -> {
                if (Processor.getValue(statement.getOperand(0)) < Processor.getValue(statement.getOperand(1))) {
                    throw new SimulatorException(statement, "trap", ExceptionCause.TRAP);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "tltu",
            List.of(OperandType.REGISTER, OperandType.REGISTER),
            InstructionFormat.R_TYPE,
            false, // Throws an exception, does not jump through the typical mechanism
            "Trap if Less Than Unsigned",
            "Trap if {0} is less than {1} using unsigned comparison",
            "000000 fffff sssss 00000 00000 110011",
            statement -> {
                if (Integer.compareUnsigned(Processor.getValue(statement.getOperand(0)), Processor.getValue(statement.getOperand(1))) < 0) {
                    throw new SimulatorException(statement, "trap", ExceptionCause.TRAP);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "tlti",
            List.of(OperandType.REGISTER, OperandType.INTEGER_16_SIGNED),
            InstructionFormat.I_TYPE,
            false, // Throws an exception, does not jump through the typical mechanism
            "Trap if Less Than Immediate",
            "Trap if {0} is less than sign-extended 16-bit immediate",
            "000001 fffff 01010 ssssssssssssssss",
            statement -> {
                if (Processor.getValue(statement.getOperand(0)) < (statement.getOperand(1) << 16 >> 16)) {
                    throw new SimulatorException(statement, "trap", ExceptionCause.TRAP);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "tltiu",
            List.of(OperandType.REGISTER, OperandType.INTEGER_16_SIGNED),
            InstructionFormat.I_TYPE,
            false, // Throws an exception, does not jump through the typical mechanism
            "Trap if Less Than Immediate Unsigned",
            "Trap if {0} is less than sign-extended 16-bit immediate using unsigned comparison",
            "000001 fffff 01011 ssssssssssssssss",
            statement -> {
                if (Integer.compareUnsigned(Processor.getValue(statement.getOperand(0)), statement.getOperand(1) << 16 >> 16) < 0) {
                    throw new SimulatorException(statement, "trap", ExceptionCause.TRAP);
                }
            }
        ));
        this.addBasicInstruction(new BasicInstruction(
            "eret",
            List.of(),
            InstructionFormat.R_TYPE,
            true,
            "Exception RETurn",
            "Set Program Counter to Coprocessor 0 EPC ($14) register value, set Coprocessor 0 Status ($12) register bit 1 (exception level) to zero",
            "010000 1 0000000000000000000 011000",
            statement -> {
                // Set EXL bit (bit 1) in Status register to 0 and jump to EPC
                Coprocessor0.updateRegister(Coprocessor0.STATUS, Binary.clearBit(Coprocessor0.getValue(Coprocessor0.STATUS), Coprocessor0.EXL_BIT));
                this.processJump(Coprocessor0.getValue(Coprocessor0.EPC));
            }
        ));
    }

    public void loadExtendedInstructions() {
        Yaml yaml = new Yaml(new LoaderOptions());

        List<?> groups;
        try {
            groups = yaml.load(this.getClass().getResourceAsStream(EXTENDED_INSTRUCTIONS_PATH));
        }
        catch (Exception exception) {
            System.err.println("Error: failed to load YAML content at '" + EXTENDED_INSTRUCTIONS_PATH + "', see below:");
            exception.printStackTrace(System.err);
            return;
        }

        for (Object groupRaw : groups) {
            Map<?, ?> group = downcastMap(groupRaw);
            if (group == null) {
                System.err.println("Error: invalid instruction group specification:\n\t" + groupRaw);
                continue;
            }

            Object mnemonicRaw = group.get("mnemonic");
            String mnemonic = downcastString(mnemonicRaw);
            if (mnemonic == null) {
                System.err.println((mnemonicRaw == null)
                    ? "Error: mnemonic not specified for instruction group"
                    : "Error: invalid mnemonic: " + mnemonicRaw);
                continue;
            }

            String groupTitle = downcastString(group.get("title"), "");
            String groupDescription = downcastString(group.get("description"), "");

            List<?> matches = downcastList(group.get("matches"), List.of());
            if (matches.isEmpty()) {
                System.out.println("Warning: no instruction matches given for '" + mnemonic + "'");
            }

            matchLoop:
            for (Object matchRaw : matches) {
                Map<?, ?> match = downcastMap(matchRaw);
                if (match == null) {
                    System.err.println("Error: invalid match specification for '" + mnemonic + "':\n\t" + matchRaw);
                    continue;
                }

                String title = downcastString(match.get("title"), groupTitle);
                String description = downcastString(match.get("description"), groupDescription);

                Object operandsRaw = match.get("operands");
                List<?> operands = downcastList(operandsRaw);
                if (operands == null) {
                    System.err.println((operandsRaw == null)
                        ? "Error: operands not specified for match of '" + mnemonic + "'"
                        : "Error: invalid match operands for '" + mnemonic + "':\n\t" + operandsRaw);
                    continue;
                }

                List<OperandType> operandTypes = new ArrayList<>(operands.size());
                for (Object operandTypeRaw : operands) {
                    OperandType operandType = OperandType.fromName(downcastString(operandTypeRaw));
                    if (operandType == null) {
                        System.err.println("Error: invalid operand type: " + operandTypeRaw);
                        continue matchLoop;
                    }
                    operandTypes.add(operandType);
                }

                Object expansionCodeRaw = match.get("expansion");
                String expansionCode = downcastString(expansionCodeRaw);
                if (expansionCode == null) {
                    System.err.println((expansionCodeRaw == null)
                        ? "Error: missing match expansion for '" + mnemonic + "'"
                        : "Error: invalid match expansion for '" + mnemonic + "':\n\t" + expansionCodeRaw);
                    continue;
                }

                Object compactExpansionCodeRaw = match.get("compact_expansion");
                String compactExpansionCode = downcastString(compactExpansionCodeRaw);
                if (compactExpansionCode == null && compactExpansionCodeRaw != null) {
                    System.err.println("Warning: invalid match compact expansion for '" + mnemonic + "':\n\t" + compactExpansionCodeRaw);
                }

                ExtendedInstruction instruction = new ExtendedInstruction(mnemonic, operandTypes, title, description);

                var statements = parseExpansionStatements(mnemonic, operandTypes, expansionCode);
                if (statements == null) {
                    continue;
                }
                instruction.setStandardExpansionTemplate(new ExpansionTemplate(instruction, statements));

                if (compactExpansionCode != null) {
                    statements = parseExpansionStatements(mnemonic, operandTypes, compactExpansionCode);
                    if (statements != null) {
                        instruction.setCompactExpansionTemplate(new ExpansionTemplate(instruction, statements));
                    }
                }

                this.addExtendedInstruction(instruction);
            }
        }
    }

    private static List<ExpansionTemplate.Statement> parseExpansionStatements(String mnemonic, List<OperandType> operandTypes, String expansionCode) {
        AssemblerLog log = new AssemblerLog();
        log.setOutput(System.err::println);
        String filename = mnemonic + " " + operandTypes; // Takes the form of "addi [reg, reg, s16]"

        SourceFile expansionLines = Tokenizer.tokenizeLines(
            filename,
            List.of(expansionCode.split("\n")),
            log,
            true
        );

        if (log.hasMessages(LogLevel.ERROR)) {
            return null;
        }

        TemplateParser parser = new TemplateParser(operandTypes, expansionLines.getLines().iterator());
        List<ExpansionTemplate.Statement> statements = new ArrayList<>();
        try {
            while (true) {
                ExpansionTemplate.Statement statement = parser.parseNextStatement();
                if (statement == null) {
                    break;
                }
                statements.add(statement);
            }
        }
        catch (RuntimeException exception) {
            log.logError(new SourceLocation(filename), exception.getMessage());
        }

        if (log.hasMessages(LogLevel.ERROR)) {
            return null;
        }

        return statements;
    }

    private static String downcastString(Object object) {
        return downcastString(object, null);
    }

    private static String downcastString(Object object, String defaultValue) {
        return (object instanceof String value) ? value : defaultValue;
    }

    private static List<?> downcastList(Object object) {
        return downcastList(object, null);
    }

    private static List<?> downcastList(Object object, List<?> defaultValue) {
        return (object instanceof List<?> value) ? value : defaultValue;
    }

    private static Map<?, ?> downcastMap(Object object) {
        return downcastMap(object, null);
    }

    private static Map<?, ?> downcastMap(Object object, Map<?, ?> defaultValue) {
        return (object instanceof Map<?, ?> value) ? value : defaultValue;
    }

    /**
     * Given an operator mnemonic, will return the corresponding Instruction object(s)
     * from the instruction set.
     *
     * @param mnemonic operator mnemonic (e.g. addi, sw)
     * @return list of corresponding Instruction object(s), empty if none match.
     */
    public List<BasicInstruction> matchBasicMnemonic(String mnemonic) {
        return this.basicInstructions.getOrDefault(mnemonic.toLowerCase(), List.of());
    }

    /**
     * Given an operator mnemonic, will return the corresponding Instruction object(s)
     * from the instruction set.
     *
     * @param mnemonic operator mnemonic (e.g. addi, sw)
     * @return list of corresponding Instruction object(s), empty if none match.
     */
    public List<ExtendedInstruction> matchExtendedMnemonic(String mnemonic) {
        return this.extendedInstructions.getOrDefault(mnemonic.toLowerCase(), List.of());
    }

    /**
     * Given an operator mnemonic, will return the corresponding Instruction object(s)
     * from the instruction set.
     *
     * @param mnemonic operator mnemonic (e.g. addi, sw)
     * @return list of corresponding Instruction object(s), empty if none match.
     */
    public List<Instruction> matchMnemonic(String mnemonic) {
        return this.allInstructions.getOrDefault(mnemonic.toLowerCase(), List.of());
    }

    /**
     * Given a string, will return the Instruction object(s) from the instruction
     * set whose operator mnemonic prefix matches it.  Case-insensitive.  For example
     * "s" will match "sw", "sh", "sb", etc.
     *
     * @param mnemonicPrefix a string
     * @return list of matching Instruction object(s), empty if none match.
     */
    public List<Instruction> matchMnemonicPrefix(String mnemonicPrefix) {
        List<Instruction> matchingInstructions = new ArrayList<>();

        StringTrie<List<Instruction>> subTrie = this.allInstructionsTrie.getSubTrieIfPresent(mnemonicPrefix.toLowerCase());
        if (subTrie != null) {
            for (List<Instruction> variants : subTrie.values()) {
                matchingInstructions.addAll(variants);
            }
        }

        return matchingInstructions;
    }

    public static <T extends Instruction> T matchInstruction(List<T> mnemonicMatches, List<OperandType> givenTypes) {
        for (T match : mnemonicMatches) {
            if (match.acceptsOperands(givenTypes)) {
                return match;
            }
        }

        return null;
    }

    public static <T extends Instruction> T matchInstructionLoosely(List<T> mnemonicMatches, List<OperandType> givenTypes) {
        for (T match : mnemonicMatches) {
            if (match.acceptsOperandsLoosely(givenTypes)) {
                return match;
            }
        }

        return null;
    }

    /**
     * Method to find and invoke a syscall given its service number.
     */
    private void processSyscall(BasicStatement statement, int number) throws SimulatorException, InterruptedException {
        Syscall service = SyscallManager.getSyscall(number);
        if (service != null) {
            service.simulate(null);
            return;
        }
        throw new SimulatorException(statement, "invalid or unimplemented syscall service: " + number, ExceptionCause.SYSCALL);
    }

    /**
     * Method to process a successful branch condition.  DO NOT USE WITH JUMP
     * INSTRUCTIONS!  The branch operand is a relative displacement in words
     * whereas the jump operand is an absolute address in bytes.
     * Handles delayed branching if that setting is enabled.
     *
     * @param displacement Displacement operand from instruction.
     */
    // 4 January 2008 DPS:  The subtraction of 4 bytes (instruction length) after
    // the shift has been removed.  It is left in as commented-out code below.
    // This has the effect of always branching as if delayed branching is enabled,
    // even if it isn't.  This mod must work in conjunction with
    // BasicStatement.java, buildBasicStatementFromBasicInstruction() method near
    // the bottom (currently line 194, heavily commented).
    private void processBranch(int displacement) {
        this.processJump(Processor.getProgramCounter() + (displacement << 2));
    }

    /**
     * Method to process a jump.  DO NOT USE WITH BRANCH INSTRUCTIONS!
     * The branch operand is a relative displacement in words
     * whereas the jump operand is an absolute address in bytes.
     * Handles delayed branching if that setting is enabled.
     *
     * @param targetAddress Jump target absolute byte address.
     */
    private void processJump(int targetAddress) {
        Simulator.getInstance().processJump(targetAddress);
    }

    /**
     * Method to process storing of a return address in the given
     * register.  This is used only by the "and link"
     * instructions: jal, jalr, bltzal, bgezal.  If delayed branching
     * setting is off, the return address is the address of the
     * next instruction (e.g. the current PC value).  If on, the
     * return address is the instruction following that, to skip over
     * the delay slot.
     *
     * @param register Register number to receive the return address.
     */
    private void processLink(int register) {
        Processor.setValue(register, Processor.getProgramCounter() + Instruction.BYTES_PER_INSTRUCTION);
    }
}

