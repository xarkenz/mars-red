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
    private final SortedMap<Integer, OperationMatcher> operationMatchers;

    public InstructionDecoder() {
        this.operationMatchers = new TreeMap<>();
    }

    public void addInstruction(BasicInstruction instruction) {
        this.operationMatchers.computeIfAbsent(instruction.getOperationMask(), OperationMatcher::new)
            .add(instruction);
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

    // Based on InstructionSet.MatchMap
    private static class OperationMatcher implements Comparable<OperationMatcher> {
        private final int operationMask;
        private final int maskBitCount;
        private final Map<Integer, BasicInstruction> instructions;

        public OperationMatcher(int operationMask) {
            this.operationMask = operationMask;
            this.maskBitCount = Integer.bitCount(operationMask);
            this.instructions = new HashMap<>();
        }

        public void add(BasicInstruction instruction) {
            this.instructions.put(instruction.getOperationKey(), instruction);
        }

        public BasicInstruction match(int binary) {
            int operationKey = binary & this.operationMask;
            return this.instructions.get(operationKey);
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof OperationMatcher matcher && matcher.operationMask == this.operationMask;
        }

        @Override
        public int compareTo(OperationMatcher matcher) {
            int difference = matcher.maskBitCount - this.maskBitCount;
            if (difference == 0) {
                difference = this.operationMask - matcher.operationMask;
            }
            return difference;
        }
    }
}
