package mars.assembler.extended;

import mars.assembler.Operand;
import mars.assembler.OperandType;
import mars.util.Binary;

import java.util.HashMap;
import java.util.Map;

public interface OperandModifier {
    int getValue(Operand operand, int address);

    OperandType getType(OperandType inputType);

    default Operand apply(Operand operand, int address) {
        return new Operand(this.getType(operand.getType()), this.getValue(operand, address));
    }

    OperandModifier LOGICAL_LOW_ORDER = new OperandModifier() {
        @Override
        public int getValue(Operand operand, int address) {
            return operand.getValue() & 0x0000FFFF;
        }

        @Override
        public OperandType getType(OperandType inputType) {
            return OperandType.INTEGER_16_UNSIGNED;
        }
    };
    OperandModifier LOGICAL_HIGH_ORDER = new OperandModifier() {
        @Override
        public int getValue(Operand operand, int address) {
            return operand.getValue() >> 16;
        }

        @Override
        public OperandType getType(OperandType inputType) {
            return OperandType.INTEGER_16_UNSIGNED;
        }
    };
    OperandModifier ARITHMETIC_LOW_ORDER = new OperandModifier() {
        @Override
        public int getValue(Operand operand, int address) {
            return operand.getValue() << 16 >> 16;
        }

        @Override
        public OperandType getType(OperandType inputType) {
            return OperandType.INTEGER_16_SIGNED;
        }
    };
    OperandModifier ARITHMETIC_HIGH_ORDER = new OperandModifier() {
        @Override
        public int getValue(Operand operand, int address) {
            return (operand.getValue() + ((operand.getValue() & 0x8000) << 1)) >> 16;
        }

        @Override
        public OperandType getType(OperandType inputType) {
            return OperandType.INTEGER_16_SIGNED;
        }
    };
    OperandModifier BRANCH_OFFSET = new OperandModifier() {
        @Override
        public int getValue(Operand operand, int address) {
            return ((operand.getValue() - address) >> 2) - 1;
        }

        @Override
        public OperandType getType(OperandType inputType) {
            return OperandType.INTEGER_16_SIGNED;
        }
    };
    OperandModifier ADDITIVE_INVERSE = new OperandModifier() {
        @Override
        public int getValue(Operand operand, int address) {
            return (1 << operand.getType().getBitWidth()) - operand.getValue();
        }

        @Override
        public OperandType getType(OperandType inputType) {
            return inputType;
        }
    };

    Map<Integer, OperandModifier> PLUS_CONSTANT_MODIFIER_POOL = new HashMap<>();

    static OperandModifier plusConstant(int constant) {
        return PLUS_CONSTANT_MODIFIER_POOL.computeIfAbsent(constant, key -> {
            return new OperandModifier() {
                @Override
                public int getValue(Operand operand, int address) {
                    return operand.getValue() + constant;
                }

                @Override
                public OperandType getType(OperandType inputType) {
                    return inputType;
                }
            };
        });
    }

    static OperandModifier parse(String key) {
        return switch (key.toLowerCase()) {
            case "l" -> LOGICAL_LOW_ORDER;
            case "h" -> LOGICAL_HIGH_ORDER;
            case "al" -> ARITHMETIC_LOW_ORDER;
            case "ah" -> ARITHMETIC_HIGH_ORDER;
            case "b" -> BRANCH_OFFSET;
            case "-" -> ADDITIVE_INVERSE;
            default -> {
                if (key.startsWith("+")) {
                    key = key.substring(1).stripLeading();
                    try {
                        int constant = Binary.decodeInteger(key);
                        yield plusConstant(constant);
                    }
                    catch (NumberFormatException exception) {
                        // Invalid modifier, continue
                    }
                }
                yield null;
            }
        };
    }
}
