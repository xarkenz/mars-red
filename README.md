<div align="center">
  <h1>MARS Red</h1>
  <img style="width: 60%" src="./docs/ide/ide-windows.png" alt="The MARS Red IDE"><br>
</div>

---------

MARS Red is a modern derivative of [MARS](https://dpetersanderson.github.io/)
(MIPS Assembler and Runtime Simulator), an education-oriented lightweight IDE
for programming in the MIPS assembly language.

The MARS Red IDE provides editing and assembling capabilities, but its real
strength lies in its support for interactive debugging.

- The program can be executed in slow motion or one instruction at a time.
  In addition, the program can be stepped backwards to undo instructions.
- Registers and data in memory are shown and can be modified by hand
  while the program is running.
- Execution breakpoints can easily be added or removed after assembling.

## Installation

Check out the [releases](https://github.com/xarkenz/mars-red/releases) page for the most recent stable version release.

To compile and run the latest build, ensure Maven is installed and use the following commands:

```bash
# Build the .jar file
mvn clean package

# Run the .jar file
java -jar *-jar-with-dependencies.jar
```

## Documentation

The latest documentation can be found [here](https://xarkenz.github.io/mars-red/).
