---
layout: default
title: Processors
permalink: /ide/processors
parent: Interface
---

The MIPS processor and coprocessors are displayed on the right-hand side at all times,
even when you are not running a program. While writing your program,
this serves as a useful reference for register names and their conventional uses
(hover mouse over the register name to see tool tips). There are three tabs:

- **Processor:** The primary MIPS processor. Displayed here is the register file
  containing all general-purpose registers (GPRs) from `$zero` / `$0` to `$ra` / `$31`,
  as well as the special `HI` / `LO` registers and the current value of `PC`
  (the Program Counter, which is the address of the next instruction to be
  fetched from memory).
- **Coprocessor 1:** The MIPS floating-point coprocessor, also known as the
  float processing unit (FPU). Displayed here is the register file containing all
  floating-point registers (FPRs) from `$f0` to `$f31`, as well as a checkbox for each
  of the eight FPU condition flags. (On a real MIPS FPU, these condition flags would
  instead be eight bits of the FCSR, or float control / status register, which also
  manages rounding modes and how floating-point exceptions are handled. MARS Red
  currently does not simulate the rest of this register, though.)
- **Coprocessor 0:** The MIPS system control coprocessor, which normally handles most
  kernel-related operations such as memory management and exception / trap handling.
  Only a small subset of this functionality is simulated by MARS Red, so most of the
  registers in the register file are left blank and should not be accessed by any
  program. When a simulated program receives a MIPS exception / trap / interrupt,
  this tab will be selected, as the registers are updated with basic information
  about the event that occurred.

Most of the registers listed in these tabs can be modified manually by double-clicking
the desired cell in the *Value* column. Coprocessor 1 supports manual entry for both
*Single* and *Double*, and interprets decimal values as their corresponding
floating-point representations.
