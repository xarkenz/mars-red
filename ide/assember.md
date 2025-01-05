---
layout: default
title: Assembling a Program
permalink: /ide/assembler
parent: Interface
---

There are two toolbar actions used to assemble MIPS programs: *Run → Assemble*
and *Run → Assemble Folder*. The difference between these actions is which source
files are assembled. The *Assemble* action assembles only the currently selected
file in the editor, whereas the *Assemble Folder* action assembles the currently
selected file along with all other assembly files (extension `.asm` or `.s`)
in the selected file's parent directory. This behavior is somewhat primitive, but
it is currently the only way to assemble multiple source files and "link" them
in MARS Red.

Note that *Assemble Folder* will always place the text (program code) and data
for the currently selected file *first* in memory. This can cause the
entry point of your program to depend on which file was selected when assembling.
To avoid this problem, it is recommended that multi-file projects define a
global label `main` to specify the desired entry point for the program,
which will be detected by MARS Red if the *Settings → Use "main" as program
entry point* option is enabled.

The *Settings → Exception Handler* dialog allows an optional exception handler
file to be included in all assembly operations. Technically, the file
is just added to the list of files given to the assembler, so it does not
necessarily need to contain an exception handler; this is just the recommended
use case for the feature.
