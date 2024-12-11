package mars.assembler;

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
public enum OperandType {
    INTEGER_3_UNSIGNED("u3", 3),
    INTEGER_5_UNSIGNED("u5", 5),
    INTEGER_15_UNSIGNED("u15", 15),
    INTEGER_16_SIGNED("s16", 16),
    INTEGER_16_UNSIGNED("u16", 16),
    INTEGER_16("i16", 16),
    INTEGER_32("i32", 32),
    REGISTER("gpr", 5),
    FP_REGISTER("fpr", 5),
    PAREN_REGISTER("(gpr)", 5),
    LABEL("label", 32),
    LABEL_OFFSET("label+", 32),
    BRANCH_OFFSET("broff", 16),
    JUMP_LABEL("jlabel", 26);

    private final String name;
    private final int bitWidth;
    private final int mask;

    OperandType(String name, int bitWidth) {
        this.name = name;
        this.bitWidth = bitWidth;
        // e.g. for bitWidth = 5, shifts to get 0b100000, then subtracts 1 to get 0b011111
        this.mask = (1 << bitWidth) - 1;
    }

    public String getName() {
        return this.name;
    }

    public int getBitWidth() {
        return this.bitWidth;
    }

    public int getMask() {
        return this.mask;
    }

    public boolean isInteger() {
        return this == INTEGER_3_UNSIGNED
            || this == INTEGER_5_UNSIGNED
            || this == INTEGER_15_UNSIGNED
            || this == INTEGER_16_SIGNED
            || this == INTEGER_16_UNSIGNED
            || this == INTEGER_16
            || this == INTEGER_32;
    }

    /**
     * Determine whether this type "accepts" another type; that is, whether an operand with the given type can be
     * interpreted as having this type. This is used to determine which instruction variant is matched by the syntax
     * of a statement.
     * <p>
     * The following table describes the return value, where a "✓" indicates that the row type accepts the column type.
     * <p>
     * <table border="1">
     * <caption>Type compatibility table</caption>
     * <tr><th></th><th><code>u3</code></th><th><code>u5</code></th><th><code>u15</code></th><th><code>s16</code></th><th><code>u16</code></th><th><code>i16</code></th><th><code>i32</code></th><th><code>gpr</code></th><th><code>fpr</code></th><th><code>(gpr)</code></th><th><code>label</code></th><th><code>label+</code></th><th><code>broff</code></th><th><code>jlabel</code></th></tr>
     * <tr><th><code>u3</code>    </th><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>u5</code>    </th><td>✓</td><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>u15</code>   </th><td>✓</td><td>✓</td><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>s16</code>   </th><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>u16</code>   </th><td>✓</td><td>✓</td><td>✓</td><td> </td><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>i16</code>   </th><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>i32</code>   </th><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>gpr</code>   </th><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>fpr</code>   </th><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>(gpr)</code> </th><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td>✓</td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>label</code> </th><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td>✓</td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>label+</code></th><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td>✓</td><td>✓</td><td> </td><td> </td></tr>
     * <tr><th><code>broff</code> </th><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td>✓</td><td> </td><td>✓</td><td> </td></tr>
     * <tr><th><code>jlabel</code></th><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td> </td><td> </td><td> </td><td>✓</td><td> </td><td> </td><td>✓</td></tr>
     * </table>
     *
     * @param fromType The type to check for acceptance.
     * @return <code>true</code> if this type accepts <code>fromType</code> according to the table above,
     *         or <code>false</code> otherwise.
     */
    public boolean accepts(OperandType fromType) {
        // This could just take the form of a lookup table, but that could get pretty large, and there are patterns
        // we can take advantage of, with few exceptions
        if (this == fromType) {
            // In the trivial case, any type accepts itself
            return true;
        }
        else if (this.isInteger() && fromType.isInteger()) {
            // For integer types, a wider type accepts a narrower type because no information is lost,
            // but not the other way around. The enum variants are purposely ordered by bit width for this purpose
            return this.ordinal() > fromType.ordinal()
                // The exception to this rule is that purely signed and purely unsigned integers cannot be mixed
                // (only need to check one case because the ordinal check above covers the other case)
                && !(this == INTEGER_16_UNSIGNED && fromType == INTEGER_16_SIGNED);
        }
        else if (this == BRANCH_OFFSET) {
            // A branch offset can be either a label or a signed 16-bit immediate
            return fromType == LABEL
                || (fromType.isInteger() && fromType.ordinal() <= INTEGER_16_UNSIGNED.ordinal());
        }
        else if (this == JUMP_LABEL) {
            // A jump label can be either a label or a 32-bit immediate
            return fromType == LABEL
                || fromType == LABEL_OFFSET
                || fromType.isInteger();
        }
        else {
            // A label without an offset can be interpreted as a label with an offset of 0
            return this == LABEL_OFFSET && fromType == LABEL;
        }
    }

    /**
     * Determine whether this type "accepts" another type; that is, whether an operand with the given type can be
     * interpreted as having this type. This is used to determine which instruction variant is matched by the syntax
     * of a statement.
     * <p>
     * The following table describes the return value, where a "✓" indicates that the row type accepts the column type.
     * <p>
     * <table border="1">
     * <caption>Type compatibility table</caption>
     * <tr><th></th><th><code>u3</code></th><th><code>u5</code></th><th><code>u15</code></th><th><code>s16</code></th><th><code>u16</code></th><th><code>i16</code></th><th><code>i32</code></th><th><code>gpr</code></th><th><code>fpr</code></th><th><code>(gpr)</code></th><th><code>label</code></th><th><code>label+</code></th><th><code>broff</code></th><th><code>jlabel</code></th></tr>
     * <tr><th><code>u3</code>    </th><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>u5</code>    </th><td>✓</td><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>u15</code>   </th><td>✓</td><td>✓</td><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>s16</code>   </th><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>u16</code>   </th><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>i16</code>   </th><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>i32</code>   </th><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>gpr</code>   </th><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td>✓</td><td> </td><td>✓</td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>fpr</code>   </th><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>(gpr)</code> </th><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td>✓</td><td> </td><td>✓</td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>label</code> </th><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td>✓</td><td>✓</td><td> </td><td> </td></tr>
     * <tr><th><code>label+</code></th><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td>✓</td><td>✓</td><td> </td><td> </td></tr>
     * <tr><th><code>broff</code> </th><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td> </td><td> </td><td> </td><td> </td><td>✓</td><td>✓</td><td>✓</td><td> </td></tr>
     * <tr><th><code>jlabel</code></th><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td> </td><td> </td><td> </td><td>✓</td><td>✓</td><td> </td><td>✓</td></tr>
     * </table>
     *
     * @param fromType The type to check for acceptance.
     * @return <code>true</code> if this type accepts <code>fromType</code> according to the table above,
     *         or <code>false</code> otherwise.
     */
    public boolean acceptsLoosely(OperandType fromType) {
        // This could just take the form of a lookup table, but that could get pretty large, and there are patterns
        // we can take advantage of, with few exceptions
        if (this == fromType) {
            // In the trivial case, any type accepts itself
            return true;
        }
        else if (this.isInteger() && fromType.isInteger()) {
            // For integer types, a wider type accepts a narrower type because no information is lost,
            // but not the other way around. Since we are loosely checking, signedness is ignored
            return this.getBitWidth() >= fromType.getBitWidth();
        }
        else if (this == BRANCH_OFFSET) {
            // A branch offset can be either a label or a any 16-bit immediate
            return fromType == LABEL
                || fromType == LABEL_OFFSET
                || (fromType.isInteger() && fromType.getBitWidth() <= INTEGER_16.getBitWidth());
        }
        else if (this == JUMP_LABEL) {
            // A jump label can be either a label or a 32-bit immediate
            return fromType == LABEL
                || fromType == LABEL_OFFSET
                || fromType.isInteger();
        }
        else {
            // A label without an offset can be interpreted as a label with an offset of 0, and vice versa
            return (this == LABEL_OFFSET && fromType == LABEL)
                || (this == LABEL && fromType == LABEL_OFFSET)
                // A (gpr) can be interpreted as a gpr and vice versa, mainly for extended instruction template purposes
                || (this == REGISTER && fromType == PAREN_REGISTER)
                || (this == PAREN_REGISTER && fromType == REGISTER);
        }
    }

    @Override
    public String toString() {
        return this.getName();
    }

    public static OperandType fromName(String name) {
        return switch (name) {
            case "u3" -> INTEGER_3_UNSIGNED;
            case "u5" -> INTEGER_5_UNSIGNED;
            case "u15" -> INTEGER_15_UNSIGNED;
            case "s16" -> INTEGER_16_SIGNED;
            case "u16" -> INTEGER_16_UNSIGNED;
            case "i16" -> INTEGER_16;
            case "i32" -> INTEGER_32;
            case "gpr" -> REGISTER;
            case "fpr" -> FP_REGISTER;
            case "(gpr)" -> PAREN_REGISTER;
            case "label" -> LABEL;
            case "label+" -> LABEL_OFFSET;
            case "broff" -> BRANCH_OFFSET;
            default -> null;
        };
    }

    public static OperandType union(OperandType type1, OperandType type2) {
        if (type1.accepts(type2)) {
            return type1;
        }
        else if (type2.accepts(type1)) {
            return type2;
        }
        else if ((type1 == INTEGER_16_SIGNED && type2 == INTEGER_16_UNSIGNED) || (type1 == INTEGER_16_UNSIGNED && type2 == INTEGER_16_SIGNED)) {
            return INTEGER_16;
        }
        else {
            return null;
        }
    }
}
