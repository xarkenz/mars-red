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
    REGISTER("reg", 5),
    FP_REGISTER("freg", 5),
    PAREN_REGISTER("(reg)", 5),
    LABEL("label", 32),
    LABEL_OFFSET("label+", 32);

    private final String name;
    private final int bitWidth;

    OperandType(String name, int bitWidth) {
        this.name = name;
        this.bitWidth = bitWidth;
    }

    public String getName() {
        return this.name;
    }

    public int getBitWidth() {
        return this.bitWidth;
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
     * <table border="1">
     * <tr><th></th><th><code>u3</code></th><th><code>u5</code></th><th><code>u15</code></th><th><code>s16</code></th><th><code>u16</code></th><th><code>i16</code></th><th><code>i32</code></th><th><code>reg</code></th><th><code>freg</code></th><th><code>(reg)</code></th><th><code>label</code></th><th><code>label+</code></th></tr>
     * <tr><th><code>u3</code></th><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>u5</code></th><td>✓</td><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>u15</code></th><td>✓</td><td>✓</td><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>s16</code></th><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>u16</code></th><td>✓</td><td>✓</td><td>✓</td><td> </td><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>i16</code></th><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>i32</code></th><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td>✓</td><td> </td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>reg</code></th><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td>✓</td><td> </td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>freg</code></th><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td>✓</td><td> </td><td> </td><td> </td></tr>
     * <tr><th><code>(reg)</code></th><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td>✓</td><td> </td><td> </td></tr>
     * <tr><th><code>label</code></th><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td>✓</td><td> </td></tr>
     * <tr><th><code>label+</code></th><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td>✓</td><td>✓</td></tr>
     * </table>
     *
     * @param type
     * @return
     */
    public boolean accepts(OperandType type) {
        // TODO: not my finest piece of code
        if (this == type) {
            return true;
        }
        else if (this.isInteger() && type.isInteger()) {
            return this.ordinal() > type.ordinal() && !(this == INTEGER_16_UNSIGNED && type == INTEGER_16_SIGNED);
        }
        else {
            return this == LABEL_OFFSET && type == LABEL;
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
            case "reg" -> REGISTER;
            case "freg" -> FP_REGISTER;
            case "(reg)" -> PAREN_REGISTER;
            case "label" -> LABEL;
            case "label+" -> LABEL_OFFSET;
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
