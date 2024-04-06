package mars.assembler;

import mars.ErrorList;
import mars.ErrorMessage;
import mars.Application;
import mars.mips.instructions.Instruction;
import mars.util.Binary;

import java.util.ArrayList;

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
 * Provides utility method related to MIPS operand formats.
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public class OperandFormat {
    private OperandFormat() {
    }

    /**
     * Syntax test for correct match in both numbers and types of operands.
     *
     * @param tokenList List of tokens generated from programmer's MIPS statement.
     * @param instruction The (presumably best matched) MIPS instruction.
     * @param errors ErrorList into which any error messages generated here will be added.
     *
     * @return Returns <tt>true</tt> if the programmer's statement matches the MIPS
     * specification, else returns <tt>false</tt>.
     */
    static boolean tokenOperandMatch(TokenList tokenList, Instruction instruction, ErrorList errors) {
        if (!numOperandsCheck(tokenList, instruction, errors)) {
            return false;
        }
        return operandTypeCheck(tokenList, instruction, errors);
    }

    /**
     * If candidate operator token matches more than one instruction mnemonic, then select
     * first such Instruction that has an exact operand match.  If none match,
     * return the first Instruction and let client deal with operand mismatches.
     */
    static Instruction bestOperandMatch(TokenList tokenList, ArrayList<Instruction> instructionMatches) {
        if (instructionMatches == null) {
            return null;
        }
        if (instructionMatches.size() == 1) {
            return instructionMatches.get(0);
        }
        for (Instruction potentialMatch : instructionMatches) {
            if (tokenOperandMatch(tokenList, potentialMatch, new ErrorList())) {
                return potentialMatch;
            }
        }
        return instructionMatches.get(0);
    }

    // Simply check to see if numbers of operands are correct and generate error message if not.
    private static boolean numOperandsCheck(TokenList tokenList, Instruction instruction, ErrorList errors) {
        int numOperands = tokenList.size() - 1;
        int expectedNumOperands = instruction.getTokenList().size() - 1;
        Token operator = tokenList.get(0);
        if (numOperands == expectedNumOperands) {
            return true;
        }
        else if (numOperands < expectedNumOperands) {
            String message = "Too few or incorrectly formatted operands. Expected: " + instruction.getExampleFormat();
            generateMessage(operator, message, errors);
        }
        else {
            String message = "Too many or incorrectly formatted operands. Expected: " + instruction.getExampleFormat();
            generateMessage(operator, message, errors);
        }
        return false;
    }

    // Generate error message if operand is not of correct type for this operation & operand position
    private static boolean operandTypeCheck(TokenList tokenList, Instruction instruction, ErrorList errors) {
        for (int i = 1; i < instruction.getTokenList().size(); i++) {
            Token candidateToken = tokenList.get(i);
            Token expectedToken = instruction.getTokenList().get(i);
            TokenType candidateType = candidateToken.getType();
            TokenType expectedType = expectedToken.getType();
            // Type mismatch is error EXCEPT when (1) instruction calls for register name and candidate is
            // register number, (2) instruction calls for register number, candidate is register name and
            // names are permitted, (3) instruction calls for integer of specified max bit length and
            // candidate is integer of smaller bit length.
            // Type match is error when instruction calls for register name, candidate is register name, and
            // names are not permitted.

            // added 2-July-2010 DPS
            // Not an error if instruction calls for identifier and candidate is operator, since operator names can be used as labels.
            if (expectedType == TokenType.IDENTIFIER && candidateType == TokenType.OPERATOR) {
                Token replacement = new Token(TokenType.IDENTIFIER, candidateToken.getValue(), candidateToken.getSourceMIPSprogram(), candidateToken.getSourceLine(), candidateToken.getStartPos());
                tokenList.set(i, replacement);
                continue;
            }
            // end 2-July-2010 addition

            if ((expectedType == TokenType.REGISTER_NAME || expectedType == TokenType.REGISTER_NUMBER) && candidateType == TokenType.REGISTER_NAME) {
                if (Application.getSettings().bareMachineEnabled.get()) {
                    // On 10-Aug-2010, I noticed this cannot happen since the IDE provides no access
                    // to this setting, whose default value is false.
                    generateMessage(candidateToken, "Use register number instead of name.  See Settings.", errors);
                    return false;
                }
                else {
                    continue;
                }
            }
            if (expectedType == TokenType.REGISTER_NAME && candidateType == TokenType.REGISTER_NUMBER) {
                continue;
            }
            if ((expectedType == TokenType.INTEGER_16 && candidateType == TokenType.INTEGER_5)
                || (expectedType == TokenType.INTEGER_16U && candidateType == TokenType.INTEGER_5)
                || (expectedType == TokenType.INTEGER_32 && candidateType == TokenType.INTEGER_5)
                || (expectedType == TokenType.INTEGER_32 && candidateType == TokenType.INTEGER_16U)
                || (expectedType == TokenType.INTEGER_32 && candidateType == TokenType.INTEGER_16))
            {
                continue;
            }
            if (candidateType == TokenType.INTEGER_16U || candidateType == TokenType.INTEGER_16) {
                int candidateValue = Binary.stringToInt(candidateToken.getValue());
                if (expectedType == TokenType.INTEGER_16 && candidateType == TokenType.INTEGER_16U
                    && candidateValue >= DataTypes.MIN_HALF_VALUE && candidateValue <= DataTypes.MAX_HALF_VALUE)
                {
                    continue;
                }
                if (expectedType == TokenType.INTEGER_16U && candidateType == TokenType.INTEGER_16
                    && candidateValue >= DataTypes.MIN_UHALF_VALUE && candidateValue <= DataTypes.MAX_UHALF_VALUE)
                {
                    continue;
                }
            }
            if ((expectedType == TokenType.INTEGER_5 && candidateType == TokenType.INTEGER_16)
                || (expectedType == TokenType.INTEGER_5 && candidateType == TokenType.INTEGER_16U)
                || (expectedType == TokenType.INTEGER_5 && candidateType == TokenType.INTEGER_32)
                || (expectedType == TokenType.INTEGER_16 && candidateType == TokenType.INTEGER_16U)
                || (expectedType == TokenType.INTEGER_16U && candidateType == TokenType.INTEGER_16)
                || (expectedType == TokenType.INTEGER_16U && candidateType == TokenType.INTEGER_32)
                || (expectedType == TokenType.INTEGER_16 && candidateType == TokenType.INTEGER_32))
            {
                generateMessage(candidateToken, "operand is out of range", errors);
                return false;
            }
            if (candidateType != expectedType) {
                generateMessage(candidateToken, "operand is of incorrect type", errors);
                return false;
            }
        }

        return true;
    }

    // Handy utility for all parse errors...
    private static void generateMessage(Token token, String message, ErrorList errors) {
        errors.add(new ErrorMessage(token.getSourceMIPSprogram(), token.getSourceLine(), token.getStartPos(), "\"" + token.getValue() + "\": " + message));
    }
}
