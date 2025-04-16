---
layout: default
title: Preprocessing
permalink: /mips/language/preprocessing
parent: Assembly Language
---

{: .no_toc }
# {{page.title}}

{: .no_toc .text-delta }
## {{site.toc_header}}

- TOC
{:toc}

{: .note }
This page is largely based on the [original MARS documentation](https://dpetersanderson.github.io/Help/MacrosHelp.html).

## Equivalences

The [`.eqv`]({{site.url}}/mips/language/directives#eqv) directive (short for "equivalence")
can be used to define an identifier which is replaced by a short fragment of syntax during
assembly preprocessing.

This feature is most commonly used to define constants in a similar manner to using the
`#define` directive in the C language. In general, equivalences should be used for inline
text substitution, whereas macros are a better choice for generating entire instructions.

Any string of language elements (excluding newlines) can be used in an equivalence, even
identifiers already established as equivalences:

```
    # After this directive, "LIMIT" will be replaced with "20"
    .eqv LIMIT 20
    # After this directive, "COUNTER" will be replaced with "$t2"
    .eqv COUNTER $t2
    # After this directive, "CLEAR" will be replaced with "li $t2, 0"
    .eqv CLEAR li COUNTER, 0
```

These equivalences can then be referenced anywhere in the subsequent code:

```
    li $v0, 1
    CLEAR
loop:
    move $a0, COUNTER
    syscall
    addi COUNTER, COUNTER, 1
    blt COUNTER, LIMIT, loop
    CLEAR

# Expands to...
    li $v0, 1
    li $t2, 0
loop:
    move $a0, $t2
    syscall
    addi $t2, $t2, 1
    blt $t2, 20, loop
    li $t2, 0
```

This particular example prints the integers 0 to 19 in order without any spacing.

### Additional Notes

- An assembler warning will be produced if multiple equivalences are defined with the
  same identifier. However, the assembler will proceed to override the previous definition
  with the new definition anyway, so this warning may be ignored if desired. The primary
  reason for this warning is to maintain parity with the GNU `.eqv` directive, though
  this may be revisited in the future.

- The `.eqv` directive may be used in the body of a macro definition, but it will only
  be executed once when the macro definition is being preprocessed unlike most other
  directives which are executed in each call to the macro.

## Macros

Patterson and Hennessy define a *macro* as "a pattern-matching and replacement facility
that provides a simple mechanism to name a frequently used sequence of instructions."[^1]
This permits the programmer to specify the instruction sequence by invoking the macro,
requiring only one line of code for each use instead of repeatedly typing
in the instruction sequence each time. It follows the axiom "define once, use many times," which
not only reduces the chance for error but also facilitates program maintenance.

Macros are like procedures (subroutines) in this sense, but unlike procedures, they only exist during
the assembly process. Procedures in MIPS assembly language follow particular protocols for definition,
calling and returning. Macros operate by substituting the macro body for each use at the time of
assembly in a process called *macro expansion*. They do not require the protocols and execution
overhead of procedures.

In some ways, a macro is similar to an extended (pseudo) instruction. The main difference is that
extended instructions are an internal assembler mechanism which can manipulate argument values to,
for example, separate the high-order bits of an immediate from the low-order bits. Macros do not
have this capability.

### Defining a Macro

For example, the Exit syscall (10) is used to exit a MIPS program. Invoking the syscall only requires two
instructions:

```
    li $v0, 10
    syscall
```

This is still somewhat tedious to write, though, and it isn't immediately clear what the function
of the code is without comments. Instead, this can be turned into a macro, which we can name
something like `exit`, using the [`.macro`]({{site.url}}/mips/language/directives#macro) and
[`.end_macro`]({{site.url}}/mips/language/directives#end_macro) directives:

```
    .macro exit
    li $v0, 10
    syscall
    .end_macro
```

The `.macro` preprocessor directive denotes the start of a macro definition, and this is where we
give the macro a name. When the program is assembled, the assembler will replace each occurrence
of the statement `exit` with the macro expansion, which consists of the sequence of statements
between the `.macro` and `.end_macro` directives.

### Parameters

Macros may optionally accept parameters which are used to influence the macro expansion. For example,
the Exit2 syscall (17) can be used instead of Exit to specify an exit code. We can design another
macro for this purpose, which we'll name `exit_value`:

```
    # The parentheses here are optional
    .macro exit_value (%value)
    li $a0, %value
    li $v0, 17
    syscall
    .end_macro
```

When calling the `exit_value` macro, the `%value` parameter must be included:

```
    # The parentheses here are optional as well
    exit_value (1)

# Expands to...
    li $a0, 1 # "%value" is substituted with the given argument
    li $v0, 17
    syscall
```

{: .note }
This is effectively like replacing each `%value` with `1` in the source code. As such, the argument
values should be appropriate for the instruction when replacing the corresponding parameters.
For instance, the macro call `exit_value ($t0)` will *expand* properly, but the expanded code will
fail to *assemble* since `li $a0, $t0` is not valid syntax for the `li` instruction.

When multiple parameters are used, they are separated by the typical assembly delimiters (whitespace
and/or comma):

```
    .macro my_add (%x, %y, %z)
    add %x, %y, %z
    .end_macro

    my_add ($sp, $sp, $t0)

# Expands to...
    add $sp, $sp, $t0
```

### Overloading

Multiple macros can be simultaneously defined under the same name, provided they accept differing
numbers of parameters. To disambiguate the macros in error messages, a shorthand is used which
joins the macro name and number of parameters with a slash. For example:

{: .note }
This is not a good example of what *should* be done with macro overloading, but it demonstrates
what is possible.

```
    .macro mystery () # "mystery/0"
    li $v0, 10
    syscall
    .end_macro

    .macro mystery (%x, %y) # "mystery/2"
    lw %x, (%y)
    .end_macro

    .macro mystery (%x, %y, %z) # "mystery/3"
    addi %y, %z, %x
    .end_macro

    mystery (0, $t0, $sp)
    mystery ($t1, $t0)
    mystery ()

# Expands to...
# mystery/3
    addi $t0, $sp, 0
# mystery/2
    lw $t1, ($t0)
# mystery/0
    li $v0, 10
    syscall
```

If a macro definition specifies the same number of parameters as a previous definition under the
same name, it will instead *override* the previous definition. Relying on this behavior is not
recommended, though.

### Labels

Labels can be defined and used within the body of a macro, allowing for internal branching or
storing data in the data segment for each instance of a macro. For an example of the latter:

```
    .macro print_string (%str)
    .data
string:
    .asciiz %str
    .text
    li $v0, 4
    la $a0, string
    syscall
    .end_macro

    print_string ("MARS")
    print_string ("MIPS")
```

If the two macro calls in this example were to be expanded with the `string` label kept as-is,
the label would have two conflicting definitions:

```
# First call expansion
    .data
string:
    .asciiz "MARS"
    .text
    li $v0, 4
    la $a0, string
    syscall
# Second call expansion
    .data
string:
    .asciiz "MIPS"
    .text
    li $v0, 4
    la $a0, string
    syscall
```

To avoid this problem, the assembler automatically appends a suffix to the label which is
unique to each instance of the macro:

```
# First call expansion (id 0)
    .data
string_M0:
    .asciiz "MARS"
    .text
    li $v0, 4
    la $a0, string_M0
    syscall
# Second call expansion (id 1)
    .data
string_M1:
    .asciiz "MIPS"
    .text
    li $v0, 4
    la $a0, string_M1
    syscall
```

{: .warning }
While the suffixed names of labels defined inside macros are relatively predictable,
referencing them explicitly is not recommended.

### Nested Calls

A useful feature of macros is that other macros can be called within a macro definition.
Rather than expanding these nested calls only once when the macro is defined, they are
expanded each time the macro itself is expanded. In practice, this means the order of
macro definitions rarely matters, and it is also possible to parameterize the name of
the macro in a nested call. For example, it is possible to define a macro which iterates
over a range of integers and performs an action with each one:

```
    # Assume "print_string/1" is defined as in the above section

    .macro print_integer (%reg)
    li $v0, 1
    move $a0, %reg
    syscall
    .end_macro

    .macro for_range (%iter_reg, %min, %max, %body_macro)
    # This macro uses extended instructions which permit %min and %max to be either immediates or registers
    add %iter_reg, $zero, %min
start_loop:
    %body_macro ()
    addi %iter_reg, %iter_reg, 1
    ble %iter_reg, %max, start_loop
    .end_macro

    # This sequence will be executed in each iteration
    .macro my_loop_body ()
    print_string "iterating with: "
    print_integer $t0
    print_string "\n"
    .end_macro

    # Run "my_loop_body" for integers 1 to 10
    for_range ($t0, 1, 10, my_loop_body)

# Expands to...
# Start of for_range/4
    add $t0, $zero, 1
start_loop_M0:
# Start of my_loop_body/0
    # These are left as-is for brevity here, but would normally be expanded by the assembler
    print_string "iterating with: "
    print_integer $t0
    print_string "\n"
# End of my_loop_body/0
    addi $t0, $t0, 1
    ble $t0, 10, start_loop_M0
# End of for_range/4
```

### Additional Notes

- When MARS references the source location of a statement generated by a macro expansion,
  rather than simply using a line number, a sequence of line numbers corresponding to
  the macro call stack is used. The notation `X → Y` references line `Y` in the source
  code, which is a line in the definition of a macro called on line `X`. For example, the
  call stack `30 → 20 → 10` represents that a macro was called on line 30, which in turn
  called a macro on line 20, which contains the statement on line 10.

- Like equivalences, macro definitions are local to the current source file. To use
  the same macro across multiple files, create a separate file with the macro definition
  and use the `.include` directive to import it.

- Forward references are not supported. That is, a macro can only be used following its
  definition in the source code.

- Nested macro *definitions* are not supported (though nested *calls* are). The `.macro`
  directive should not appear inside the body of a macro definition.

- Each argument in a macro call represents a single language element (token). For instance,
  writing `4($t0)` in an argument list actually becomes four separate arguments: `4`, `(`,
  `$t0`, and `)`.

- Macros are a feature of the assembler, not the architecture itself. As a result, the syntax
  may differ from that of other assemblers. For compatibility with the SPIM simulator,
  macro parameters may start with `$` instead of `%`.

## Includes

The [`.include`]({{site.url}}/mips/language/directives#include) directive can be used to
effectively "paste" the contents of another assembly file during assembly preprocessing,
much like the `#include` directive in the C language.

Includes are designed to make macros and equivalences more convenient to use across multiple
files. Both macros and equivalences are local to the source file where they are defined,
which means that other source files in a multi-file project cannot access them. Without
`.include`, you would have to repeat their definitions in every file which uses them.
Tedium aside, this is poor programming practice and would defeat the "define once, use many
times" paradigm that macros and equivalences follow. Includes allow you to define macros and
equivalences in a separate file, then include it in any file where you want to use them.

Take, for example, the `exit` macro from above:

```
    .macro exit
    li $v0, 10
    syscall
    .end_macro
```

If we want to use this macro in multiple source files, we can move the macro definition into
a new file, which we'll call `macros.asm`. Then, we can use the `.include` directive to
include the macro definition in each file where it is needed:

```
    .include "macros.asm"
    .data
value:
    .word 13
    .text
    li $v0, 1
    lw $a0, value
    syscall
    exit

# Expands (without macro processing) to...
    .macro exit
    li $v0, 10
    syscall
    .end_macro
    .data
value:
    .word 13
    .text
    li $v0, 1
    lw $a0, value
    syscall
    exit
```

### Additional Notes

- The file path specified in an `.include` directive is interpreted relative to the
  location of the file containing that directive.

- The assembler preprocessor detects recursive includes (cases where a file includes
  itself, whether directly or indirectly) and reports them as errors.

- The nature of includes presents some challenges for source code numbering in error
  messages and the *Text Segment* display. If a file being included has any assembly
  errors, the filename and line number in the error message should refer to the file
  which was included, not the file that included it. Similarly, the line numbers given
  in the *Text Segment* source code display are based on the line numbers of the
  original file as seen in the editor.

- The `.include` directive may be used in the body of a macro definition, but it will only
  be executed once when the macro definition is being preprocessed unlike most other
  directives which are executed in each call to the macro. As such, the path of the file
  to include cannot be parameterized.

## Acknowledgements

The MARS macro facility was developed in 2012 by Mohammad Hossein Sekhavat
([sekhavat17@gmail.com](mailto:sekhavat17@gmail.com)), while an engineering student at
Sharif University in Tehran. MARS creators Pete and Ken are incredibly grateful for his
contribution! Pete developed `.eqv` and `.include` at about the same time.

[^1]:
	*Computer Organization and Design: The Hardware/Software Interface, Fourth Edition*,
	Patterson and Hennessy, Morgan Kauffman Publishers, 2009.
