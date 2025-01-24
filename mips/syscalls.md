---
layout: default
title: System Calls
permalink: /mips/syscalls
parent: MIPS Reference
---

{: .no_toc }
# System Calls

{: .no_toc .text-delta }
## Table of Contents

- TOC
{:toc}

A number of system services, mainly for input and output, are available for use by your MIPS program.
Calling these services allows a program to interact with the console, read/write files, terminate the program,
allocate heap memory, and more.

## Usage

To use a system service in your program, follow these steps using the table below:

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

{: .note }
MIPS register contents are not affected by system calls unless they are specified as result registers below.

## Available Services

| Service | Number | Arguments | Results |
| :------ | :----- | :-------- | :------ |
| Print Integer | 1 | `$a0` &mdash; integer to print |  |
| Print Float | 2 | `$f12` &mdash; float to print |  |
| Print Double | 3 | `$f12` / `$f13` &mdash; double to print |  |
| Print String | 4 | `$a0` &mdash; address of null-terminated string to print |  |
| Read Integer | 5 |  | `$v0` &mdash; integer read from user input |
| Read Float | 6 |  | `$f0` &mdash; float read from user input |
| Read Double | 7 |  | `$f0` / `$f1` &mdash; double read from user input |
| Read String | 8 | `$a0` &mdash; address of input buffer <br> `$a1` &mdash; maximum number of characters to read | Follows semantics of `fgets` in UNIX. Given a specified maximum length <var>n</var>, the string can be no more than <var>n</var>&nbsp;&minus;&nbsp;1 characters long. If the string uses fewer than <var>n</var>&nbsp;&minus;&nbsp;1 characters, a newline is inserted after the last character. In either case, a null terminator byte is appended to the string. If <var>n</var>&nbsp;=&nbsp;1, user input is ignored and a null terminator byte is placed at the buffer address. If <var>n</var>&nbsp;&lt;&nbsp;1, user input is ignored and nothing is written to the buffer. |
| Sbrk <br> *(allocate heap memory)* | 9 | `$a0` &mdash; number of bytes to allocate on the heap | `$v0` &mdash; address of newly allocated memory |
| Exit <br> *(terminate program)* | 10 |  | Program execution is terminated immediately. |
| Print Character | 11 | `$a0` &mdash; character to print | Prints an ASCII character corresponding to the lowest-order byte of `$a0`. |
| Read Character | 12 |  | `$v0` &mdash; character read from user input |
| Open File | 13 | `$a0` &mdash; address of null-terminated string containing filename <br> `$a1` &mdash; flags <br> `$a2` &mdash; mode (currently ignored) | `$v0` &mdash; file descriptor (negative if an error occurred) <br> MARS implements several flag values: 0 for read-only, 1 for write-only, 2 for read-write, 8 for append, and 16 for create-new. By default, write opens will create the file if it doesn't exist; if create-new is specified, the file must not exist. It ignores mode. The returned file descriptor will be negative if the operation failed. The underlying file I/O implementation uses the [`java.nio.channels.FileChannel`] class to read and write. MARS maintains file descriptors internally and allocates them starting with 3. File descriptors 0, 1 and 2 are always open for reading from standard input, writing to standard output, and writing to standard error, respectively. |
| Read File | 14 | `$a0` &mdash; file descriptor <br> `$a1` &mdash; address of input buffer <br> `$a2` &mdash; maximum number of characters to read | `$v0` &mdash; number of characters read (zero if at the end of the file; negative if an error occurred) |
| Write File | 15 | `$a0` &mdash; file descriptor <br> `$a1` &mdash; address of output buffer <br> `$a2` &mdash; number of characters to write | `$v0` &mdash; number of characters written (negative if an error occurred) |
| Close File | 16 | `$a0` &mdash; file descriptor |  |
| Exit2 <br> *(terminate with exit code)* | 17 | `$a0` &mdash; integer exit code | Program execution is terminated immediately. If the program is run from within the MARS graphical user interface (GUI), the exit code in `$a0` is displayed in the message console. |
| Time <br> *(get system time)* | 30 |  | `$a0` &mdash; low order 32 bits of system time <br> `$a1` &mdash; high order 32 bits of system time <br> System time is obtained from `java.util.Date.getTime()` as the number of milliseconds since 1 January 1970. |
| MIDI Out | 31 | `$a0` &mdash; pitch (0-127) <br> `$a1` &mdash; duration in milliseconds <br> `$a2` &mdash; instrument (0-127) <br> `$a3` &mdash; volume (0-127) | Begins playing tone and returns immediately. <br> *See section below for details.* |
| Sleep | 32 | `$a0` &mdash; duration to sleep in milliseconds | Pauses execution for (approximately) the specified duration. This timing will not be precise, as the Java implementation will add some overhead. |
| MIDI Out Synchronous | 33 | `$a0` &mdash; pitch (0-127) <br> `$a1` &mdash; duration in milliseconds <br> `$a2` &mdash; instrument (0-127) <br> `$a3` &mdash; volume (0-127) | Plays tone and returns upon tone completion. <br> *See section below for details.* |
| Print Integer Hexadecimal | 34 | `$a0` &mdash; integer to print | Displayed value is 8 hexadecimal digits, left-padding with zeroes if necessary. |
| Print Integer Binary | 35 | `$a0` &mdash; integer to print | Displayed value is 32 bits, left-padding with zeroes if necessary. |
| Print Integer Unsigned | 36 | `$a0` &mdash; integer to print | Displayed as unsigned decimal value. |
| Random Seed[^random] | 40 | `$a0` &mdash; integer ID for pseudorandom number generator <br> `$a1` &mdash; seed for corresponding pseudorandom number generator | Sets the seed of the corresponding pseudorandom number generator. |
| Random Integer[^random] | 41 | `$a0` &mdash; integer ID for pseudorandom number generator | `$a0` &mdash; the next pseudorandom, uniformly distributed integer value from this random number generator's sequence |
| Random Integer Range[^random] | 42 | `$a0` &mdash; integer ID for pseudorandom number generator <br> `$a1` &mdash; upper bound on the range of returned values (exclusive) | `$a0` &mdash; the next pseudorandom, uniformly distributed integer value in the range 0 &le; `$a0` &lt; `$a1` from this random number generator's sequence |
| Random Float[^random] | 43 | `$a0` &mdash; integer ID for pseudorandom number generator | `$f0` &mdash; the next pseudorandom, uniformly distributed float value in the range 0.0 &le; `$f0` &lt; 1.0 from this random number generator's sequence |
| Random Double[^random] | 44 | `$a0` &mdash; integer ID for pseudorandom number generator | `$f0` / `$f1` &mdash; the next pseudorandom, uniformly distributed double value in the range 0.0 &le; `$f0` &lt; 1.0 from this random number generator's sequence |
| Confirm Dialog | 50 | `$a0` &mdash; address of null-terminated string message for the user | `$a0` &mdash; value corresponding to the option chosen: <br> 0: Yes <br> 1: No <br> 2: Cancel |
| Input Dialog Integer | 51 | `$a0` &mdash; address of null-terminated string message for the user | `$a0` &mdash; integer read from user input <br> `$a1` &mdash; status value: <br> 0: OK was chosen, no issues <br> &minus;1: OK was chosen, but input was invalid <br> &minus;2: Cancel was chosen <br> &minus;3: OK was chosen, but input was blank |
| Input Dialog Float | 52 | `$a0` &mdash; address of null-terminated string message for the user | `$f0` &mdash; float read from user input <br> `$a1` &mdash; status value: <br> 0: OK was chosen, no issues <br> &minus;1: OK was chosen, but input was invalid <br> &minus;2: Cancel was chosen <br> &minus;3: OK was chosen, but input was blank |
| Input Dialog Double | 53 | `$a0` &mdash; address of null-terminated string message for the user | `$f0` / `$f1` &mdash; double read from user input <br> `$a1` &mdash; status value: <br> 0: OK was chosen, no issues <br> &minus;1: OK was chosen, but input was invalid <br> &minus;2: Cancel was chosen <br> &minus;3: OK was chosen, but input was blank |
| Input Dialog String | 54 | `$a0` &mdash; address of null-terminated string message for the user <br> `$a1` &mdash; address of input buffer <br> `$a2` &mdash; maximum number of characters to read | `$a1` &mdash; status value: <br> 0: OK was chosen, no issues; buffer contains the input string <br> &minus;2: Cancel was chosen; no change to buffer <br> &minus;3: OK was chosen, but input was blank; no change to buffer <br> &minus;4: OK was chosen, but character limit was exceeded; buffer contains the trimmed input ending with a null terminator <br> Behaves in the same manner as the Read String service (8) above. |
| Message Dialog | 55 | `$a0` &mdash; address of null-terminated string message for the user <br> `$a0` &mdash; the type of message to be displayed (differentiated by icon): <br> 0: error message <br> 1: informational message <br> 2: warning message <br> 3: question message <br> other: plain message (no icon) |  |
| Message Dialog Integer | 56 | `$a0` &mdash; address of null-terminated string message for the user <br> `$a1` &mdash; integer value to append to the message |  |
| Message Dialog Float | 57 | `$a0` &mdash; address of null-terminated string message for the user <br> `$f12` &mdash; float value to append to the message |  |
| Message Dialog Double | 58 | `$a0` &mdash; address of null-terminated string message for the user <br> `$f12` / `$f13` &mdash; double value to append to the message |  |
| Message Dialog String | 59 | `$a0` &mdash; address of null-terminated string message for the user <br> `$a1` &mdash; address of null-terminated string to append to the message |  |
| Print Last I/O Message | 60 |  | The most recent file operation message is printed to the message console if MARS is ran with a GUI, or the terminal if ran from the command line. |
| Seek File | 61 | `$a0` &mdash; file descriptor <br> `$a1` &mdash; offset to add to position specified by whence <br> `$a2` &mdash; whence to add the offset to (can be 0 for beginning of file, 1 for current position, or 2 for end of file) | `$v0` &mdash; new position in file (-1 if an error occurred) |
| Launch Tool <br> *coming in 5.0-beta8* | 62 | `$a0` &mdash; address of null-terminated string tool identifier | The MARS tool corresponding to the identifier is launched in the GUI, unless it is already open. To find the identifier for a tool, look for square brackets in the title bar of the tool window. |
| Notify Tool <br> *coming in 5.0-beta8* | 63 | `$a0` &mdash; address of null-terminated string tool identifier <br> `$a1` &mdash; notify key | The MARS tool corresponding to the identifier is notified of an event; this is often used for tool configuration and state changes. Refer to the help menu for a specific tool for guidance on how it uses this service. |
| Query Tool <br> *coming in 5.0-beta8* | 64 | `$a0` &mdash; address of null-terminated string tool identifier <br> `$a1` &mdash; query key | The MARS tool corresponding to the identifier is queried for information. Refer to the help menu for a specific tool for guidance on how it uses this service. |

[^random]:
    Pseudorandom number generation in services 40 - 44 is implemented with [`java.util.Random`].
    Each generator identified by the value of `$a0` is a unique `Random` instance.
    Arbitrary IDs can be used; any time a new ID is used, a new generator is instantiated for that ID.
    The generators are seeded with the current time by default, so use the Set Seed service (40) if a deterministic outcome is desired.

## File I/O Example

The sample MIPS program below will open a new file for writing, write text to it from a memory buffer,
then close it. The file will be created in the same directory as the assembly file.

```
    .data
fout: # Filename for output
    .asciiz "testout.txt"
buffer:
    .asciiz "The quick brown fox jumps over the lazy dog."

    .text
main:
    # Create and open a file for writing
    li      $v0, 13      # "Open File" system call
    la      $a0, fout    # Filename to open
    li      $a1, 1       # Opening flags (1 = write-only)
    syscall              # Open the file (file descriptor returned in $v0)
    move    $s6, $v0     # Save the file descriptor so $v0 can be reused

    # Write to the file
    li      $v0, 15      # "Write File" system call
    move    $a0, $s6     # File descriptor
    la      $a1, buffer  # Address of buffer whose content to write
    li      $a2, 44      # Buffer size
    syscall              # Write the buffer content to the file

    # Close the file
    li      $v0, 16      # "Close File" system call
    move    $a0, $s6     # File descriptor
    syscall              # Close the file

    # Exit the program
    li      $v0, 10      # "Exit" system call
    syscall              # Exit the program
```

## MIDI Output Services (31, 32, 33)

*The MIDI system services were originally developed and documented by Otterbein student Tony Brock in July 2007.*

These system services are unique to MARS, and provide a means of producing sound. MIDI output is simulated by
your system sound card, and the functionality is provided by the [`javax.sound.midi`] package.

Service 31 will generate the tone then immediately return. Service 33 will generate the tone then
sleep for the tone's duration before returning. Thus it essentially combines services 31 and 32.

This service requires four parameters for each tone:

{: .no_toc }
### Pitch (`$a0`)

Accepts a byte value (0 - 127) that denotes a pitch as it would be represented in MIDI.
Each number is one semitone / half-step in the chromatic scale.
0 represents a very low C and 127 represents a very high G, where a standard 88 key piano begins at 9 (A) and ends at 108 (C).
If the parameter value is outside this range, it applies a default value 60 which is the same as middle C on a piano.
From middle C, all other pitches in the octave are as follows:

- 60: C or B&#9839;
- 61: C&#9839; or D&#9837;
- 62: D
- 63: D&#9839; or E&#9837;
- 64: E or F&#9837;
- 65: F or E&#9839;
- 66: F&#9839; or G&#9837;
- 67: G
- 68: G&#9839; or A&#9837;
- 69: A
- 70: A&#9839; or B&#9837;
- 71: B or C&#9837;
- 72: C or B&#9839;

To produce these pitches in other octaves, add or subtract multiples of 12.

{: .no_toc }
### Duration (`$a1`)

Accepts an integer value that is the length of the tone in milliseconds.
If the parameter value is negative, it applies a default value of one second (1000 milliseconds).

{: .no_toc }
### Instrument (`$a2`)

Accepts a byte value (0 - 127) that denotes the General MIDI "patch" used to play the tone.
If the parameter is outside this range, a default value of 0 (*Acoustic Grand Piano*) is used.
General MIDI standardizes the number associated with each possible instrument
(often referred to as the *program change* number), but it does
not specify how exactly the tone will sound. This is determined by the synthesizer
that is producing the sound. Thus, a *Tuba* (patch 58) on one computer
may sound different than that same patch on another computer.
The 128 available patches are divided into instrument families of 8:

|:--------------------|:---------------------|
| 0&nbsp;-&nbsp;7     | Piano                |
| 8&nbsp;-&nbsp;15    | Chromatic Percussion |
| 16&nbsp;-&nbsp;23   | Organ                |
| 24&nbsp;-&nbsp;31   | Guitar               |
| 32&nbsp;-&nbsp;39   | Bass                 |
| 40&nbsp;-&nbsp;47   | Strings              |
| 48&nbsp;-&nbsp;55   | Ensemble             |
| 56&nbsp;-&nbsp;63   | Brass                |
| 64&nbsp;-&nbsp;71   | Reed                 |
| 72&nbsp;-&nbsp;79   | Pipe                 |
| 80&nbsp;-&nbsp;87   | Synth Lead           |
| 88&nbsp;-&nbsp;95   | Synth Pad            |
| 96&nbsp;-&nbsp;103  | Synth Effects        |
| 104&nbsp;-&nbsp;111 | Ethnic               |
| 112&nbsp;-&nbsp;119 | Percussion           |
| 120&nbsp;-&nbsp;127 | Sound Effects        |

Note that outside of Java, General MIDI usually refers to patches 1-128.
When referring to a list of General MIDI patches, the range must be adjusted by 1 to play the correct patch.
For a full list of General MIDI instruments, see
[this page](https://www.cs.cmu.edu/~music/cmsip/readings/GMSpecs_Patches.htm).
(Or just do an internet search. The links on the official www.midi.org website appear to be broken.)
The General MIDI channel 10 percussion key map is not relevant to the
current implementation because it always defaults to MIDI channel 1.

{: .no_toc }
### Volume (`$a3`)

Accepts a byte value (0 - 127) where 0 is silent and 127 is the loudest.
This value denotes MIDI "velocity", which refers to the initial attack of the tone.
If the parameter value is outside this range, a default value of 100 is used.
MIDI velocity measures how hard a *note on* (or *note off*)
message is played, perhaps on a MIDI controller like a keyboard. Most
MIDI synthesizers will translate this into volume on a logarithmic scale
in which the difference in amplitude decreases as the velocity value increases.
Note that velocity value on more sophisticated synthesizers can also
affect the timbre of the tone (as most instruments sound different when
they are played louder or softer).

[`java.nio.channels.FileChannel`]: {{site.java_se_docs}}/java.base/java/nio/channels/Channel.html
[`java.util.Random`]: {{site.java_se_docs}}/java.base/java/util/Random.html
[`javax.sound.midi`]: {{site.java_se_docs}}/java.desktop/javax/sound/midi/package-summary.html
