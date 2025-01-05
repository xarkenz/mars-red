---
layout: default
title: System Calls
permalink: /mips/syscalls
parent: MIPS Reference
---

A number of system services, mainly for input and output, are available for use by your MIPS program.
These services allow a program to interact with the console, read/write files, terminate the program,
allocate heap memory, and more.

MIPS register contents are not affected by a system call, except for result registers
as specified in the table below.

To use a syscall service in your program, follow these steps using the table below:

1. Load the service number in register `$v0`.
2. Load the argument values, if any, in the registers specified by the *Arguments* column.
3. Issue the syscall via the `syscall` instruction.
4. Retrieve the resulting values, if any, from the registers specified by the *Results* column.

For example, to print the integer value stored in `$t0` in the console:

```
    li      $v0, 1    # Service 1 prints a decimal integer
    move    $a0, $t0  # Load desired value into argument register $a0
    syscall           # Issue the syscall
```

## Available Services

Coming soon.
