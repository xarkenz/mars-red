import re

# L   Low-order bits              (use when low-order is zero-extended)
# AL  Arithmetic Low-order bits   (use when low-order is sign-extended)
# H   High-order bits             (use when low-order is zero-extended)
# AH  Arithmetic High-order bits  (use when low-order is sign-extended)
# B   Branch offset
# DB  Delayed Branching
# +m  x => x + m
# -   x => -x

class Instruction:
    def __init__(self, mnemonic, title):
        self.mnemonic = mnemonic
        self.title = title
        self.matches = []

class InstructionMatch:
    def __init__(self, description, operands, expansion, compact_expansion):
        self.description = description
        self.operands = operands
        self.expansion = expansion
        self.compact_expansion = compact_expansion

def get_token_type(tokens, index):
    token = tokens[index]
    if not token or token in "()+":
        return None

    prefix = None if index - 1 < 0 else tokens[index - 1]
    # The "N" in "label+N" is treated as part of the previous operand of type "label+"
    if prefix == "+":
        return None
    if not prefix or prefix not in "(":
        prefix = ""
    suffix = None if index + 1 >= len(tokens) else tokens[index + 1]
    if not suffix or suffix not in ")+":
        suffix = ""

    if token.startswith("$"):
        return prefix + ("freg" if token.startswith("$f") else "reg") + suffix
    try:
        immediate = int(token)
        if 0 <= immediate < (1 << 3):
            return prefix + "u3" + suffix
        elif 0 <= immediate < (1 << 5):
            return prefix + "u5" + suffix
        elif 0 <= immediate < (1 << 16):
            return prefix + "u16" + suffix
        elif -(1 << 15) <= immediate < 0:
            return prefix + "s16" + suffix
        else:
            return prefix + "i32" + suffix
    except:
        return prefix + "label" + suffix

def tokenize(statement):
    tokens = re.split(r"\s*(\+|\(|\))\s*|(?:(?<!-)\s|,)+", statement.strip())
    return [token and token.replace(" ", "") for token in tokens]

def detokenize(tokens):
    if not tokens:
        return ""
    statement = tokens[0] + " "
    for index in range(2, len(tokens), 2):
        if tokens[index - 1]:
            statement += tokens[index - 1]
        elif index > 2:
            statement += ", "
        statement += tokens[index]
    return statement

# RGn => {n}
# NRn => {n:+1}
# OPn => {n}
# LLn => {n:AL}
# LLnU => {n:L}
# LLnPm => {n:+m,AL}
# LHn => {n:AH}
# LHnPm => {n:+m,AH}
# VLn => {n:AL}
# VLnU => {n:L}
# VLnPm => {n:+m,AL}
# VLnPmU => {n:+m,L}
# VHLn => {n:H}
# VHn => {n:AH}
# VHLnPm => {n:+m,H}
# VHnPm => {n:+m,AH}
# LLP => {1:AL}
# LLPU => {1:L}
# LLPPm => {1:+m,AL}
# LHPA => {1:AH}
# LHPN => {1:H}
# LHPAPm => {1:+m,AH}
# LHL => {1:H}
# LAB => {n:B}
# S32 => {n:-}
# DBNOP => {DB:nop}
# BROFFnm => {DB:m,n}

def translate_token(token, token_operand_indices):
    if not token or not token.isupper():
        return token
    elif token == "DBNOP":
        return "{DB:nop:}"
    elif len(token) == 7 and token.startswith("BROFF"):
        return "{DB:" + token[6] + ":" + token[5] + "}"
    elif token in ("LAB", "S32"):
        # Index of last operand
        operand_index = len(token_operand_indices) - token_operand_indices.count(None) - 1
        return "{" + str(operand_index) + (":B}" if token == "LAB" else ":-}")

    # All substitutions not specifying a token index refer to a label at operand index 1
    operand_index = 1
    modifiers = []
    is_unsigned = False
    key = token

    if len(key) >= 1 and key[-1] == "U":
        is_unsigned = True
        key = key[:-1]
    # OPn should not trigger this condition
    if len(key) > 3 and key[-2] == "P" and key[-1].isdigit():
        modifiers.append("+" + key[-1])
        key = key[:-2]
    if len(key) >= 1 and key[-1].isdigit():
        operand_index = token_operand_indices[int(key[-1])]
        key = key[:-1]

    if len(key) < 2:
        return token

    if key in ("RG", "NR", "OP"):
        if key == "NR":
            modifiers.append("+1")
    elif key in ("LL", "VL", "LLP"):
        modifiers.append("L" if is_unsigned else "AL")
    elif key in ("LHL", "VHL", "LHPN"):
        modifiers.append("H")
    elif key in ("LH", "VH", "LHPA"):
        modifiers.append("AH")
    else:
        return token

    substitution = "{" + str(operand_index)
    if modifiers:
        substitution += ":" + ",".join(modifiers)
    return substitution + "}"

def translate_template(template, token_operand_indices):
    return [
        detokenize([
            translate_token(token, token_operand_indices)
            for token in tokenize(statement)
        ])
        for statement in template if statement
    ]

def convert(read_line, write):
    instructions = {}
    lines = []

    line = read_line()
    while line:
        line = line.strip()
        if not line or line.startswith("#"):
            lines.append((False, line))
            line = read_line()
            continue

        title = ""
        description = ""
        if "#" in line:
            [line, description] = line.split("#", maxsplit=1)
            if ":" in description:
                [title, description] = description.split(":", maxsplit=1)
                title = title.strip()
            description = description.lstrip()
            line = line.rstrip()

        compact_template = None
        if "COMPACT" in line:
            [line, compact_line] = line.split("COMPACT", maxsplit=1)
            compact_line = compact_line.lstrip()
            line = line.rstrip()
            compact_template = compact_line.split("\t")

        template = line.split("\t")
        example = template.pop(0)
        example_tokens = tokenize(example)
        mnemonic = example_tokens[0]

        token_operand_indices = [None]
        operands = []
        operand_index = 0
        for token_index, token in enumerate(example_tokens):
            if token_index == 0 or not token:
                continue
            token_type = get_token_type(example_tokens, token_index)
            if token_type:
                description = description.replace(token, "{" + str(operand_index) + "}")
                operands.append(token_type)
                token_operand_indices.append(operand_index)
                operand_index += 1
            else:
                token_operand_indices.append(None)

        expansion = translate_template(template, token_operand_indices)
        compact_expansion = None
        if compact_template:
            compact_expansion = translate_template(compact_template, token_operand_indices)

        instruction_match = InstructionMatch(description, operands, expansion, compact_expansion)
        if mnemonic in instructions:
            instructions[mnemonic].matches.append(instruction_match)
        else:
            instruction = Instruction(mnemonic, title)
            instruction.matches.append(instruction_match)
            instructions[mnemonic] = instruction
            lines.append((True, mnemonic))

        line = read_line()

    for (is_mnemonic, content) in lines:
        if not is_mnemonic:
            write(content + "\n")
            continue

        instruction = instructions[content]

        write(f"- mnemonic: {instruction.mnemonic}\n")
        if instruction.title:
            write(f"  title: \"{instruction.title}\"\n")
        write("  matches:\n")
        for instruction_match in instruction.matches:
            write("    - ")
            if instruction_match.description:
                write(f"description: \"{instruction_match.description}\"\n      ")
            if instruction_match.operands:
                write(f"operands: [ {', '.join(instruction_match.operands)} ]\n")
            else:
                write("operands: []\n")
            write(f"      expansion: |\n")
            for statement in instruction_match.expansion:
                write(f"        {statement}\n")
            if instruction_match.compact_expansion:
                write(f"      compact_expansion: |\n")
                for statement in instruction_match.compact_expansion:
                    write(f"        {statement}\n")

if __name__ == "__main__":
    import sys
    if len(sys.argv) < 3:
        raise ValueError("usage: <src> <dst>")

    with open(sys.argv[1], "r") as src_file:
        with open(sys.argv[2], "w") as dst_file:
            convert(src_file.readline, dst_file.write)
