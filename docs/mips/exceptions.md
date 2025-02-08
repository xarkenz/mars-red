---
layout: default
title: Exceptions, Traps, and Interrupts
permalink: /mips/exceptions
parent: MIPS Reference
---

{: .no_toc }
# {{page.title}}

{: .no_toc .text-delta }
## {{site.toc_header}}

- TOC
{:toc}

{: .note }
This page is largely based on the [original MARS documentation](https://dpetersanderson.github.io/Help/MarsExceptions.html).

## Introduction

Exceptions, traps, and interrupts are all distinct from each other, though they use the same
underlying mechanism. Exceptions are caused by exceptional conditions that occur at runtime
such as invalid memory address references. Traps are caused explicitly by certain instructions,
most of which have a mnemonic starting with "`t`". Interrupts are caused by external devices
in memory-mapped I/O (MMIO).

MARS simulates basic elements of the MIPS32 exception mechanism, but many features are not supported.

When an exception, trap, or interrupt occurs, the following steps are taken by the simulator:

- Bit 1 of Coprocessor 0 Status (`$12`) is set.
- Bits 2-6 of Coprocessor 0 Cause (`$13`) are set to the exception type (see below for the
  list of supported exception types).
- Coprocessor 0 EPC (`$14`) is set to the address of the instruction that triggered the
  exception or trap, or in the case of an interrupt, the address of the instruction being executed
  when the interrupt occurred.
- If the exception was caused by an invalid memory address, Coprocessor 0 BadVAddr (`$8`)
  is set to the invalid address.
- Program execution flow jumps to the MIPS instruction at memory location `0x80000180`.
  Or, if there is no instruction at location `0x80000180`, MARS will terminate the MIPS program
  with an appropriate error message by default. This address in the kernel text segment is the standard
  MIPS32 exception handler location. The only way to change it in MARS is to change the MIPS memory
  configuration through the *Settings→Memory Configuration* dialog.

The following exception causes are used by MARS:

|:--|:--|
| `ADDRESS_FETCH` (4) | Attempted to read memory at an invalid or misaligned address. |
| `ADDRESS_STORE` (5) | Attempted to write memory at an invalid or misaligned address. |
| `SYSCALL` (8) | An exception occurred while executing a `syscall` instruction. |
| `BREAKPOINT` (9) | Encountered a `break` instruction. |
| `RESERVED_INSTRUCTION` (10) | Encountered an instruction whose opcode is not recognized by the instruction set. |
| `ARITHMETIC_OVERFLOW` (12) | Arithmetic overflow occurred during addition or subtraction. Division by zero does not cause an exception! |
| `TRAP` (13) | Trapped due to a trap instruction. |

Bits 8-15 of Coprocessor 0 Cause (`$13`) are used to indicate pending interrupts.
This is used by the *Keyboard and Display Simulator* tool, for example, where bit 8 represents
a keyboard interrupt and bit 9 represents a display interrupt. For more details, see the help menu for
that tool.

## Exception Handlers

An *exception handler*, also known as a *trap handler* or *interrupt handler*, can easily
be incorporated into a MIPS program. This guide is not intended to be comprehensive but provides
the essential information for writing and using exception handlers.

There are multiple ways to include an exception handler in a MIPS program:

- Write the exception handler in the same file as the rest of the program. An example of this is
  presented below.
- Write the exception handler in a separate file, store that file in the same directory as the other
  program files, and assemble using the *Run→Assemble Folder* action.
- Write the exception handler in a separate file, store that file in any directory, then open the
  *Settings→Exception Handler* dialog, check the checkbox and browse to that file. Note that
  using this method keeps the exception handler active until the checkbox is unchecked again.

The exception handler itself should start at kernel text address `0x80000180` in the standard
memory configuration (open the *Settings→Memory Configuration* dialog to find the address
if you are using a different layout). Use the directive `.ktext 0x80000180` to achieve this.

If you use any general-purpose registers in your exception handler besides `$k0` and `$k1`
(which are reserved for this exact purpose), you should save their original values and restore them after use
to prevent the "clobbered" register values from interfering with the regular program. Note that this
includes `$at`, which is used by many extended instructions! You can save the values either by
`move`ing them into `$k0` / `$k1`, or by using the stack.

The exception handler can return control to the program using the `eret` instruction.
This will set the Program Counter to the value of Coprocessor 0 EPC (`$14`), so be sure to
increment `$14` by 4 before returning if you want to skip over the instruction that caused
the exception.

Use the `mfc0` and `mtc0` instructions to read from and write to Coprocessor 0 registers.

## Exception Handler Example

The sample MIPS program below generates a trap exception, triggering the exception handler.
After printing a message, the exception handler returns control to the instruction following
the one that triggered the exception, then the program terminates normally.

```
# Regular program code
    .text
main:
    # Generate a trap exception
    teqi    $zero, 0      # Trap unconditionally

    # Exit the program now that the exception handler has returned
    li      $v0, 10       # "Exit" system call
    syscall               # Exit the program

# Relevant data for the exception handler
    .kdata
msg:
    .asciiz "Trap generated"

# Exception handler code
    .ktext  0x80000180
    # Save the value of $at (typically good practice) using the stack
    addi    $sp, $sp, -4  # Grow the stack by one word
    sw      $at, ($sp)    # Push the value of $at to the stack
    # Save the values of $v0 and $a0 using $k0 and $k1, respectively
    move    $k0, $v0
    move    $k1, $a0

    # Print a message to console
    li      $v0, 4        # "Print String" system call
    la      $a0, msg      # Address of the string to print
    syscall               # Print the string to console

    # Restore the original values of $v0 and $a0
    move    $v0, $k0
    move    $a0, $k1
    # Restore the original value of $at
    lw      $at, ($sp)    # Pop the value of $at from the stack
    addi    $sp, $sp, 4   # Shrink the stack by one word

    # Increment EPC (Exception Program Counter) in order to skip the trap instruction
    mfc0    $k0, $14      # Coprocessor 0 register $14 has address of trapping instruction
    addi    $k0, $k0, 4   # Add 4 to point to next instruction
    mtc0    $k0, $14      # Store new address back into $14
    # Return to the regular program code
    eret                  # Exception return; jump to EPC
```
