package mars.mips.instructions;

import mars.assembler.BasicStatement;

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

public class InstructionDecoder {
    private final SortedMap<OperationMask, OperationMatcher> operationMatchers;

    public InstructionDecoder() {
        this.operationMatchers = new TreeMap<>();
    }

    public void addInstruction(BasicInstruction instruction) {
        OperationMask mask = new OperationMask(instruction.getOperationMask());
        this.operationMatchers.computeIfAbsent(mask, OperationMatcher::new).add(instruction);
    }

    public BasicInstruction decodeInstruction(int binary) {
        for (OperationMatcher matcher : this.operationMatchers.values()) {
            BasicInstruction instruction = matcher.match(binary);
            if (instruction != null) {
                return instruction;
            }
        }
        return null;
    }

    public BasicStatement decodeStatement(int binary) {
        BasicInstruction instruction = this.decodeInstruction(binary);
        if (instruction == null) {
            return new BasicStatement(null, null, List.of(), binary);
        }
        else {
            return new BasicStatement(null, instruction, instruction.decodeOperands(binary), binary);
        }
    }

    private static class OperationMask implements Comparable<OperationMask> {
        private final int value;
        private final int bitCount;

        public OperationMask(int value) {
            this.value = value;
            this.bitCount = Integer.bitCount(value);
        }

        @Override
        public int compareTo(OperationMask that) {
            int difference = that.bitCount - this.bitCount;
            if (difference == 0) {
                difference = this.value - that.value;
            }
            return difference;
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof OperationMask mask && mask.value == this.value;
        }
    }

    // Based on InstructionSet.MatchMap
    private static class OperationMatcher {
        private final int operationMask;
        private final Map<Integer, BasicInstruction> instructions;

        public OperationMatcher(OperationMask operationMask) {
            this.operationMask = operationMask.value;
            this.instructions = new HashMap<>();
        }

        public void add(BasicInstruction instruction) {
            this.instructions.put(instruction.getOperationKey(), instruction);
        }

        public BasicInstruction match(int binary) {
            int operationKey = binary & this.operationMask;
            return this.instructions.get(operationKey);
        }
    }
}
