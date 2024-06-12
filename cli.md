---
layout: default
title: Command Usage
permalink: /cli
nav_order: 3
---

# Command Usage

Command syntax, where `mars.jar` is the path to the downloaded JAR
(items surrounded by `[]` are optional):

`java -jar mars.jar [options...] <asmfiles...> [pa <args...>]`

Valid options (case-insensitive, separated by spaces):

| Option                           | Description                                                                                                                                                                                                                                                   |
|----------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `a`                              | Assemble only, do not simulate.                                                                                                                                                                                                                               |
| `ae<n>`                          | Terminate MARS with integer exit code *n* if an assemble error occurs.                                                                                                                                                                                        |
| `ascii`                          | Display memory or register contents interpreted as ASCII.                                                                                                                                                                                                     |
| `b`                              | Brief; do not display register/memory address along with contents.                                                                                                                                                                                            |
| `d`                              | Print debugging statements.                                                                                                                                                                                                                                   |
| `da` or `ad`                     | Both `a` and `d`.                                                                                                                                                                                                                                             |
| `db`                             | Enable delayed branching.                                                                                                                                                                                                                                     |
| `dec`                            | Display memory or register contents in decimal.                                                                                                                                                                                                               |
| `dump <segment> <format> <file>` | Dump memory contents to file. Supports an address range (see `<m>-<n>` below).  Current supported segments are `.text` and `.data`.  Current supported dump formats are `Binary`, `HexText`, `BinaryText`.                                                    |
| `h`                              | Display help.  Use by itself and with no filename.                                                                                                                                                                                                            |
| `hex`                            | Display memory or register contents in hexadecimal (default).                                                                                                                                                                                                 |
| `ic`                             | Display count of MIPS basic instructions 'executed'.                                                                                                                                                                                                          |
| `mc <config>`                    | Set memory configuration, where *config* is `Default` for the MARS default 32-bit address space, `CompactDataAtZero` for a 32KB address space with data segment at address 0, or `CompactTextAtZero` for a 32KB address space with text segment at address 0. |
| `me`                             | Display MARS messages to standard error instead of standard output. Can separate via redirection.                                                                                                                                                             |
| `nc`                             | Do not display copyright notice (for cleaner redirected/piped output).                                                                                                                                                                                        |
| `np` or `ne`                     | No extended instructions (pseudo-instructions) allowed.                                                                                                                                                                                                       |
| `p`                              | Project mode; assemble all files in the same directory as given file.                                                                                                                                                                                         |
| `se<n>`                          | Terminate MARS with integer exit code *n* if a simulation error occurs.                                                                                                                                                                                       |
| `sm`                             | Start execution at `main`. Execution will start at program statement globally labeled `main`.                                                                                                                                                                 |
| `smc`                            | Allow self-modifying code. If enabled, the program can write and branch to either text or data segment.                                                                                                                                                       |
| `we`                             | Assembler warnings will be considered errors.                                                                                                                                                                                                                 |
| `<n>`                            | Set the step limit, where *n* is the maximum number of steps to simulate. If 0, negative or not specified, no step limit will be applied.                                                                                                                     |
| `<reg>`                          | Display register contents after simulation, where *reg* is the number or name (e.g. `$5`, `$t3`, `$f10`) of a register.  May be repeated to specify multiple registers. The `$` is not required, except for register numbers such as `$5`.                    |
| `<m>-<n>`                        | Display memory address range from *m* (inclusive) to *n* (exclusive) after simulation, where *m* and *n* may be hex or decimal, *m* &le; *n*, and both must lie on a word boundary. May be repeated to specify multiple memory address ranges.                |
| `pa <args...>`                   | Specify program arguments separated by spaces. This option must be the last one specified since everything that follows it is interpreted as a program argument to be made available to the MIPS program at runtime.                                          |
