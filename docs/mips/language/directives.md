---
layout: default
title: Directives
permalink: /mips/language/directives
parent: Assembly Language
---

{: .no_toc }
# {{page.title}}

{: .no_toc .text-delta }
## {{site.toc_header}}

- TOC
{:toc}

## Segment Directives

{: #text }
### `.text`: Switch to Text Segment

> Syntax:
>
> **.text** \[ *address* \][^opt]

Instructions following this directive are stored in the text region of memory.

"*address*" is an optional operand which can be used to manually set the
starting address for storing instructions in the text segment. This is rarely ever
necessary for this segment; in most cases, the default behavior is sufficient.

{: #data }
### `.data`: Switch to Data Segment

> Syntax:
>
> **.data** \[ *address* \][^opt]

Values following this directive are stored in the static data region of memory.
To store values, use the [storage directives](#storage-directives).

"*address*" is an optional operand which can be used to manually set the
starting address for storing values in the data segment. This is rarely ever
necessary for this segment; in most cases, the default behavior is sufficient.

{: #ktext }
### `.ktext`: Switch to Kernel Text Segment

> Syntax:
>
> **.ktext** \[ *address* \][^opt]

Instructions following this directive are stored in the kernel text region of memory.

"*address*" is an optional operand which can be used to manually set the
starting address for storing instructions in the text segment. In MARS, this is
primarily used to create an exception handler starting at kernel text address
`0x80000180`. For more information about exception handling, see
[Exceptions, Traps, and Interrupts]({{site.url}}/mips/exceptions).

{: #kdata }
### `.kdata`: Switch to Kernel Data Segment

> Syntax:
>
> **.kdata** \[ *address* \][^opt]

Values following this directive are stored in the kernel data region of memory.
To store values, use the [storage directives](#storage-directives).

"*address*" is an optional operand which can be used to manually set the
starting address for storing values in the kernel data segment. This is rarely ever
necessary for this segment; in most cases, the default behavior is sufficient.

## Storage Directives

{: #byte }
### `.byte`: Store Byte Value(s)

> Syntax:
>
> **.byte** *operand* \[ , *operand* ... \][^opt]
>
> Where "*operand*" is one of the following:
> - *immediate*
> - *immediate* : *count*

{: .compatibility-note }
In MARS 4.5 and earlier, "*operand*" can only take the "*immediate* : *count*"
form if it is the first and only operand to the directive.

Store 8-bit byte value(s) in the current segment (which must be a data segment).
The values are stored in the order of the operands. For operands in the form
"*immediate* : *count*", the "*immediate*" value is stored *count* times sequentially.

{: #half }
### `.half`: Store Halfword Value(s)

> Syntax:
>
> **.half** *operand* \[ , *operand* ... \][^opt]
>
> Where "*operand*" is one of the following:
> - *immediate*
> - *immediate* : *count*

{: .compatibility-note }
In MARS 4.5 and earlier, "*operand*" can only take the "*immediate* : *count*"
form if it is the first and only operand to the directive.

Store 16-bit halfword value(s) in the current segment (which must be a data segment).
The values are stored in the order of the operands. For operands in the form
"*immediate* : *count*", the "*immediate*" value is stored *count* times sequentially.
By default, values are aligned to halfword boundaries.

{: #word }
### `.word`: Store Word Value(s)

> Syntax:
>
> **.word** *operand* \[ , *operand* ... \][^opt]
>
> Where "*operand*" is one of the following:
> - *immediate*
> - *label*
> - *immediate* : *count*
> - *label* : *count*

{: .compatibility-note }
In MARS 4.5 and earlier, "*operand*" can only take the "*immediate* : *count*"
or *label* : *count* form if it is the first and only operand to the directive.

Store 32-bit word value(s) in the current segment (which must be a data segment).
If a label is used as an operand, the word value stored is its address.
The values are stored in the order of the operands. For operands in the forms
"*immediate* : *count*" or "*label* : *count*", the "*immediate*" or "*label*"
value is stored *count* times sequentially. By default, values are aligned to
word boundaries.

{: #float }
### `.float`: Store Single-Precision Floating-Point Value(s)

> Syntax:
>
> **.float** *operand* \[ , *operand* ... \][^opt]
>
> Where "*operand*" is one of the following:
> - *float-literal*
> - *float-literal* : *count*

{: .compatibility-note }
In MARS 4.5 and earlier, "*operand*" can only take the "*float-literal* : *count*"
form if it is the first and only operand to the directive.

Store 32-bit single-precision floating-point value(s) in the current segment (which must
be a data segment). The values are stored in the order of the operands. For operands
in the form "*float-literal* : *count*", the "*float-literal*" value is stored *count*
times sequentially. By default, values are aligned to word boundaries.

{: #double }
### `.double`: Store Double-Precision Floating-Point Value(s)

> Syntax:
>
> **.double** *operand* \[ , *operand* ... \][^opt]
>
> Where "*operand*" is one of the following:
> - *float-literal*
> - *float-literal* : *count*

{: .compatibility-note }
In MARS 4.5 and earlier, "*operand*" can only take the "*float-literal* : *count*"
form if it is the first and only operand to the directive.

Store 64-bit double-precision floating-point value(s) in the current segment (which must
be a data segment). The values are stored in the order of the operands. For operands
in the form "*float-literal* : *count*", the "*float-literal*" value is stored *count*
times sequentially. By default, values are aligned to word (not doubleword) boundaries.

{: #ascii }
### `.ascii`: Store ASCII String(s)

> Syntax:
>
> **.ascii** *string-literal* \[ , *string-literal* ... \][^opt]

Store string(s) of 8-bit characters in the current segment (which must be a data segment).
The strings are stored in the order of the operands, but are not delimited with a null
terminator byte, and thus the length of strings stored using this directive cannot be
determined at runtime. To store null-terminated strings, use [`.asciiz`](#asciiz) instead,
or manually add a null byte (`\0`) at the end of each string.

{: #asciiz }
### `.asciiz`: Store Null-Terminated ASCII String(s)

> Syntax:
>
> **.asciiz** *string-literal* \[ , *string-literal* ... \][^opt]

Store string(s) of 8-bit characters in the current segment (which must be a data segment).
The strings are stored in the order of the operands, and each is followed by a null
terminator byte (`\0`) to form a C-style string. The null terminator byte is used by many
system calls and programs to determine the length of a string at runtime given the address
of the first character in the string. To store a string without the null terminator byte,
use [`.ascii`](#ascii) instead.

{: #space }
### `.space`: Reserve Uninitialized Space

> Syntax:
>
> **.space** *size*

Reserve a space of "*size*" bytes in the current segment (which must be a data segment).
Although the space is likely to be zeroed due to the implementation of memory in MARS,
this is not guaranteed behavior, and should not be relied on. To initialize the space
with zeroes, use a directive like [`.byte`](#byte) instead.

{: #align }
### `.align`: Align Next Storage Item

> Syntax:
>
> **.align** *power*

Ensure the next data item is stored at an address which is a multiple of
2*<sup>power</sup>*. For example, `.align 2` followed by `.space 32` will reserve 32
bytes of space aligned to a 2<sup>2</sup> byte boundary&mdash;that is, the reserved space
will start at a word-aligned address.

Since byte alignment is always guaranteed, the "*power*" value of 0 has a special meaning.
The directive `.align 0`, rather than aligning the next data item, *disables automatic
alignment for every data item* between the directive and the next
[segment directive](#segment-directives).

## Symbol Directives

{: #globl }
### `.globl`: Declare Global Symbol

> Syntax:
>
> **.globl** *label* \[ , *label* ... \][^opt]

Move one or more local symbols from the current file into the global symbol table. In
projects with multiple source files, this directive can be used to access labels defined
in another source file which is also currently being assembled.

{: #extern }
### `.extern`: Declare External Field

> Syntax:
>
> **.extern** *label*, *size*

Create a global data field of "*size*" bytes labeled as "*label*". The field will be
allocated in the global data segment, which starts at address `0x10000000` in the default
memory configuration. This directive is useful for declaring a variable which can be
accessed and/or modified in multiple source files.

{: .compatibility-note }
The updated implementation of this feature introduced in MARS Red 5.0-beta7 has not been
fully tested for compatibility with the behavior of the directive in MARS 4.5. If you
notice any issues, please [submit a report](https://github.com/xarkenz/mars-red/issues/new).

## Preprocessor Directives

{: #eqv }
### `.eqv`: Define Equivalence

> Syntax:
>
> **.eqv** *identifier*, *replacement...*

Define an [equivalence]({{site.url}}/mips/language/preprocessing#equivalences) with
the given identifier and replacement. Following this directive, all instances of
"*identifier*" in the source code will be replaced with "*replacement...*", which can
be any sequence of tokens (language elements). This directive is typically used to
define assembly-time constants.

{: #macro }
### `.macro`: Define Macro

> Syntax:
>
> **.macro** *name* \[ , *parameter* ... \][^opt]

Begin the definition of a [macro]({{site.url}}/mips/language/preprocessing#macros)
named "*name*" which accepts the given parameters. Each "*parameter*" should be a
valid macro parameter starting with `%` (or `$`, for backwards compatibility with the
older SPIM simulator). The characters `(` and `)` may be used in the parameter list
for readability.

All statements and label definitions between this directive and the next
[`.end_macro`](#end_macro) directive will be part of the macro definition, and
will not be visible to the assembler after preprocessing.

{: #end_macro }
### `.end_macro`: End Macro Definition

> Syntax:
>
> **.end_macro**

End a macro definition previously started with [`.macro`](#macro).

{: #include }
### `.include`: Include File Contents

> Syntax
>
> **.include** *path-string*

[Include]({{site.url}}/mips/language/preprocessing#includes) the contents of another
text file at the location of this directive in the source code. "*path-string*" must
be a quoted string literal specifying either a path relative to the current file,
or an absolute path (though not recommended for portability reasons).

---

[^opt]: Syntax wrapped in "\[ \]" is optional.
