package mars.simulator;

import mars.Application;
import mars.util.Binary;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

/*
Copyright (c) 2003-2013,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
*/

/**
 * Provides standard input/output services needed to simulate the MIPS syscall
 * routines.  These methods will detect whether the simulator is being
 * run from the command line or through the GUI, then do I/O to
 * {@link System#in} and {@link System#out} in the former situation, and interact with
 * the GUI in the latter.
 *
 * @author Pete Sanderson and Ken Vollmar, August 2003-2005
 */
public class SystemIO {
    /**
     * Open the file for read access. (Mutually exclusive with {@link #WRITE_ONLY_FLAG} and {@link #READ_WRITE_FLAG}.)
     * <p>
     * This constant is named differently from all the others due to it representing the absence of any flags.
     * So, to check for this state, check for equality instead of performing a bitwise AND.
     */
    public static final int READ_ONLY_FLAGS = 0x00000000;
    /**
     * Open the file for write access. (Mutually exclusive with {@link #READ_ONLY_FLAGS} and {@link #READ_WRITE_FLAG}.)
     */
    public static final int WRITE_ONLY_FLAG = 0x00000001;
    /**
     * Open the file for both reading and writing. (Mutually exclusive with {@link #READ_ONLY_FLAGS} and
     * {@link #WRITE_ONLY_FLAG}.)
     * <p>
     * This is a separate flag to preserve backwards compatibility with older versions
     * of MARS, as well as SPIM, which use the absence of any flags to represent read-only mode. Since
     * {@link #READ_ONLY_FLAGS} | {@link #WRITE_ONLY_FLAG} would equal {@link #WRITE_ONLY_FLAG}, this mode had
     * to be made distinct somehow.
     */
    public static final int READ_WRITE_FLAG = 0x00000002;
    /**
     * Start writing data at the end of the file. (Can only be used with {@link #WRITE_ONLY_FLAG}.)
     */
    public static final int APPEND_FLAG = 0x00000008;
    /**
     * Fail to open if the file already exists; in other words, prevent overwriting of an existing file.
     * (Can be used with either {@link #WRITE_ONLY_FLAG} or {@link #READ_WRITE_FLAG}.)
     */
    public static final int CREATE_NEW_FLAG = 0x00000010;

    /**
     * Enumeration of locations whence to seek from in a file. This provides context for an integer
     * <code>offset</code>, informing the system of the initial position the offset is based on.
     */
    public enum SeekWhence {
        /**
         * The file offset is set to <code>offset</code> bytes.
         */
        FROM_START(0),
        /**
         * The file offset is set to its current position plus <code>offset</code> bytes.
         */
        FROM_CURRENT(1),
        /**
         * The file offset is set to the size of the file plus <code>offset</code> bytes.
         */
        FROM_END(2);

        private final int ordinal;

        SeekWhence(int ordinal) {
            this.ordinal = ordinal;
        }

        /**
         * Get the internal integer value associated with this "whence" value.
         *
         * @return The integer ordinal.
         */
        public int getOrdinal() {
            return this.ordinal;
        }

        /**
         * Find the "whence" value, if any, corresponding to a given internal integer value.
         *
         * @param ordinal The integer ordinal to find the "whence" value for.
         * @return The "whence" value, if one was found, or <code>null</code> otherwise.
         */
        public static SeekWhence valueOf(int ordinal) {
            for (SeekWhence whence : SeekWhence.values()) {
                if (whence.getOrdinal() == ordinal) {
                    return whence;
                }
            }
            return null;
        }
    }

    /**
     * File descriptor for standard input ({@link System#in}).
     */
    public static final int STDIN_DESCRIPTOR = 0;
    /**
     * File descriptor for standard output ({@link System#out}).
     */
    public static final int STDOUT_DESCRIPTOR = 1;
    /**
     * File descriptor for standard error ({@link System#err}).
     */
    public static final int STDERR_DESCRIPTOR = 2;
    /**
     * The first file descriptor that will be allocated to the user.
     */
    public static final int FIRST_USER_DESCRIPTOR = 3;

    private static final boolean DEBUG_PRINT_HANDLES = false;

    private static BufferedReader inputReader = null;

    private Path workingDirectory = null;
    private List<FileHandle> handles;
    private int nextDescriptor;
    private String fileOperationMessage = null;

    /**
     * Private method to simply return the BufferedReader used for
     * keyboard input, redirected input, or piped input.
     * These are all equivalent in the eyes of the program because they are
     * transparent to it.  Lazy instantiation.
     *
     * @author DPS 28 Feb 2008
     */
    private static BufferedReader getInputReader() {
        if (inputReader == null) {
            inputReader = new BufferedReader(new InputStreamReader(System.in));
        }
        return inputReader;
    }

    /**
     * Set up I/O functionality.
     */
    public SystemIO() {
        this.initHandles();
    }

    /**
     * Close any open files, and reinitialize standard I/O just in case.
     */
    public void resetFiles() {
        for (int descriptor = FIRST_USER_DESCRIPTOR; descriptor < this.handles.size(); descriptor++) {
            this.closeFile(descriptor);
        }
        this.initHandles();
    }

    private void initHandles() {
        this.handles = new ArrayList<>(3);
        this.handles.add(new FileHandle("stdin", Channels.newChannel(System.in), READ_ONLY_FLAGS));
        this.handles.add(new FileHandle("stdout", Channels.newChannel(System.out), WRITE_ONLY_FLAG));
        this.handles.add(new FileHandle("stderr", Channels.newChannel(System.err), WRITE_ONLY_FLAG));
        System.out.flush();
        System.err.flush();
        this.nextDescriptor = this.handles.size();
    }

    /**
     * Get the working directory which is used to calculate relative paths when using {@link #openFile(Path, int)}.
     *
     * @return The path representing the working directory, or <code>null</code> if the default working directory
     *         is in use (which is determined by the <code>user.dir</code> system property).
     */
    public Path getWorkingDirectory() {
        return this.workingDirectory;
    }

    /**
     * Set the working directory which is used to calculate relative paths when using {@link #openFile(Path, int)}.
     *
     * @param workingDirectory The path representing the working directory, or <code>null</code> to use the
     *                         <code>user.dir</code> system property (which is the default behavior).
     */
    public void setWorkingDirectory(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    /**
     * Get the string message indicating the result of the previous file operation.
     *
     * @return The operation message.
     */
    public String getFileOperationMessage() {
        return this.fileOperationMessage;
    }

    /**
     * Retrieve the file handle corresponding to the given file descriptor.
     *
     * @param descriptor The file descriptor for the desired handle.
     * @return The handle, if it exists and is currently in use, or null otherwise.
     */
    public FileHandle getOpenHandle(int descriptor) {
        if (0 <= descriptor && descriptor < this.handles.size()) {
            FileHandle handle = this.handles.get(descriptor);
            if (handle.isOpen()) {
                return handle;
            }
        }
        return null;
    }

    /**
     * Retrieve the byte channel corresponding to the given file descriptor.
     *
     * @param descriptor The file descriptor for the desired byte channel.
     * @return The byte channel, if the descriptor corresponds to a valid file handle, or null otherwise.
     */
    public Channel getChannel(int descriptor) {
        FileHandle handle = this.getOpenHandle(descriptor);
        return (handle != null) ? handle.getChannel() : null;
    }

    /**
     * Retrieve the readable byte channel corresponding to the given file descriptor.
     *
     * @param descriptor The file descriptor for the desired readable byte channel.
     * @return The readable byte channel, if the descriptor corresponds to a valid readable
     *         file handle, or null otherwise.
     */
    public ReadableByteChannel getReadableChannel(int descriptor) {
        Channel channel = this.getChannel(descriptor);
        return (channel instanceof ReadableByteChannel readChannel) ? readChannel : null;
    }

    /**
     * Retrieve the writable byte channel corresponding to the given file descriptor.
     *
     * @param descriptor The file descriptor for the desired writable byte channel.
     * @return The writable byte channel, if the descriptor corresponds to a valid writable
     *         file handle, or null otherwise.
     */
    public WritableByteChannel getWritableChannel(int descriptor) {
        Channel channel = this.getChannel(descriptor);
        return (channel instanceof WritableByteChannel writeChannel) ? writeChannel : null;
    }

    /**
     * Open a file for either reading or writing. Also note that file permission
     * modes are also NOT IMPLEMENTED.
     *
     * @param filename The path of the file to open.
     * @param flags    One of {@link #READ_ONLY_FLAGS}, {@link #WRITE_ONLY_FLAG}, {@link #READ_WRITE_FLAG},
     *                 {@link #WRITE_ONLY_FLAG} | {@link #APPEND_FLAG}, or any of the aforementioned
     *                 combined with {@link #CREATE_NEW_FLAG}.
     * @return File descriptor for the file opened, or -1 if an error occurred.
     * @author Ken Vollmar
     */
    public int openFile(Path filename, int flags) {
        FileChannel channel;
        Set<OpenOption> options = new HashSet<>();
        String mode;

        if (this.workingDirectory != null) {
            filename = this.workingDirectory.resolve(filename);
        }

        boolean readOnly = (flags == READ_ONLY_FLAGS);
        boolean writeOnly = ((flags & WRITE_ONLY_FLAG) != 0);
        boolean readWrite = ((flags & READ_WRITE_FLAG) != 0);
        boolean append = ((flags & APPEND_FLAG) != 0);
        boolean createNew = ((flags & CREATE_NEW_FLAG) != 0);

        if (readOnly) {
            // Open for reading only
            options.add(StandardOpenOption.READ);
            mode = "reading";
        }
        else if (writeOnly || readWrite) {
            // Open for at least writing
            options.add(StandardOpenOption.WRITE);
            if (readWrite) {
                // Both writing and reading
                options.add(StandardOpenOption.READ);
                mode = "reading and writing";
            }
            else {
                // Write-only, check for append
                if (append) {
                    options.add(StandardOpenOption.APPEND);
                    mode = "appending";
                }
                else {
                    mode = "writing";
                }
            }
            if (createNew) {
                options.add(StandardOpenOption.CREATE_NEW);
                mode += " (requiring new file)";
            }
            else {
                options.add(StandardOpenOption.CREATE);
            }
        }
        else {
            this.fileOperationMessage = "Invalid flags for opening file \"" + filename + "\": " + Binary.intToHexString(flags);
            return -1;
        }

        try {
            channel = FileChannel.open(filename, options);
        }
        catch (IOException exception) {
            this.fileOperationMessage = "File \"" + filename + "\" unable to open for " + mode;
            return -1;
        }

        // Dynamically create another file handle if needed
        if (this.nextDescriptor >= this.handles.size()) {
            this.handles.add(new FileHandle(this.handles.size() + 1));
        }

        // Open the file handle corresponding to the next descriptor
        int descriptor = this.nextDescriptor;
        FileHandle handle = this.handles.get(descriptor);
        this.nextDescriptor = handle.getNextDescriptor();
        handle.open(filename.toString(), channel, flags);

        this.printHandlesForDebug();

        this.fileOperationMessage = "Successfully opened file \"" + filename + "\" with descriptor " + descriptor;
        return descriptor;
    }

    /**
     * Close the file with specified file descriptor. Sets the file operation message accordingly.
     *
     * @param descriptor The descriptor of the file to close.
     */
    public void closeFile(int descriptor) {
        // Do an explicit bounds check to provide more descriptive error messages
        if (FIRST_USER_DESCRIPTOR <= descriptor && descriptor < this.handles.size()) {
            FileHandle handle = this.getOpenHandle(descriptor);
            if (handle != null) {
                String name = handle.getName();
                handle.close(this.nextDescriptor);
                this.nextDescriptor = descriptor;

                this.printHandlesForDebug();

                this.fileOperationMessage = "Successfully closed file with descriptor " + descriptor + " (" + name + ")";
            }
            else {
                this.fileOperationMessage = "Failed to close file with descriptor " + descriptor + ", as it was already closed";
            }
        }
        else if (descriptor == STDIN_DESCRIPTOR || descriptor == STDOUT_DESCRIPTOR || descriptor == STDERR_DESCRIPTOR) {
            this.fileOperationMessage = "File with descriptor " + descriptor + " (" + this.handles.get(descriptor).getName() + ") cannot be closed";
        }
        else {
            this.fileOperationMessage = "File descriptor " + descriptor + " is not in use and cannot be closed";
        }
    }

    /**
     * Print debug information to console about the handle list and next descriptor. Only
     * prints if {@link #DEBUG_PRINT_HANDLES} is <code>true</code>.
     */
    private void printHandlesForDebug() {
        if (DEBUG_PRINT_HANDLES) {
            System.out.println("Handles: ");
            for (int i = 0; i < this.handles.size(); i++) {
                System.out.println(" " + i + ": " + this.handles.get(i));
            }
            System.out.println("Next desc: " + this.nextDescriptor);
        }
    }

    /**
     * Write bytes to file.
     *
     * @param descriptor Target file descriptor.
     * @param buffer     Byte buffer containing characters to write. All characters from the current position
     *                   to the limit will be written.
     * @return Number of bytes written, or -1 on error.
     */
    public int writeToFile(int descriptor, ByteBuffer buffer) {
        // DPS 8-Jan-2013
        // Write to STDOUT or STDERR file descriptor while using IDE - write to console.
        if ((descriptor == STDOUT_DESCRIPTOR || descriptor == STDERR_DESCRIPTOR) && Application.getGUI() != null) {
            // Sean Clarke (05/2024): originally used buffer.array() but was getting UnsupportedOperationException
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            Application.getGUI().getMessagesPane().writeToConsole(new String(data));

            this.fileOperationMessage = "Successfully wrote " + data.length + " bytes to file with descriptor " + descriptor;
            return data.length;
        }
        // When running in command mode, code below works for either regular file or STDOUT/STDERR.

        // Retrieve the writable channel corresponding to the descriptor
        WritableByteChannel channel = this.getWritableChannel(descriptor);
        // Ensure the file descriptor has been opened for writing
        if (channel == null) {
            this.fileOperationMessage = "File with descriptor " + descriptor + " is not open for writing";
            return -1;
        }
        try {
            // From testing, this does not stop writing on zero-bytes.
            int numBytes = channel.write(buffer);
            this.fileOperationMessage = "Successfully wrote " + numBytes + " bytes to file with descriptor " + descriptor;
            return numBytes;
        }
        catch (NonWritableChannelException exception) {
            this.fileOperationMessage = "File with descriptor " + descriptor + " is not open for writing";
            return -1;
        }
        catch (IOException exception) {
            this.fileOperationMessage = "Failed to write to file with descriptor " + descriptor;
            return -1;
        }
    }

    /**
     * Read sequential bytes from a file into a buffer.
     *
     * @param descriptor Target file descriptor.
     * @param buffer     Byte buffer to contain bytes read. The remaining capacity is the maximum number of characters.
     * @return Number of bytes read, 0 on EOF, or -1 on error.
     */
    public int readFromFile(int descriptor, ByteBuffer buffer) throws InterruptedException {
        // DPS 8-Jan-2013
        // Read from STDIN file descriptor while using IDE - get input from console.
        if (descriptor == STDIN_DESCRIPTOR && Application.getGUI() != null) {
            String input = Application.getGUI().getMessagesPane().getInputString(buffer.remaining());
            byte[] bytesRead = input.getBytes();
            int numBytes = Math.min(buffer.remaining(), bytesRead.length);
            buffer.put(bytesRead, 0, numBytes);

            this.fileOperationMessage = "Successfully read " + numBytes + " bytes from file with descriptor " + descriptor;
            return numBytes;
        }
        // When running in command mode, code below works for either regular file or STDIN.

        // Retrieve the readable channel corresponding to the descriptor
        ReadableByteChannel channel = this.getReadableChannel(descriptor);
        // Ensure the file descriptor has been opened for reading
        if (channel == null) {
            this.fileOperationMessage = "File with descriptor " + descriptor + " is not open for reading";
            return -1;
        }
        try {
            // Read up to buffer.remaining() bytes of data from this input stream into an array of bytes.
            int numBytes = channel.read(buffer);
            // The method above will return -1 upon EOF, but our spec says that negative
            // value represents an error, so we return 0 for EOF.  DPS 10-July-2008.
            if (numBytes == -1) {
                numBytes = 0;
            }
            this.fileOperationMessage = "Successfully read " + numBytes + " bytes from file with descriptor " + descriptor;
            return numBytes;
        }
        catch (NonReadableChannelException exception) {
            this.fileOperationMessage = "File with descriptor " + descriptor + " is not open for reading";
            return -1;
        }
        catch (IOException exception) {
            this.fileOperationMessage = "Failed to read from file with descriptor " + descriptor;
            return -1;
        }
    }

    /**
     * Change the current position where read/write operations will be done within a file.
     *
     * @param descriptor Target file descriptor.
     * @param offset     Offset from position specified by <code>whence</code>. May be negative if applicable.
     * @param whence     Directive indicating where in the file to start the offset from.
     * @return New position in the file, or -1 if an error occurred.
     */
    public long seekFile(int descriptor, long offset, SeekWhence whence) {
        Channel channel = this.getChannel(descriptor);
        if (channel == null) {
            this.fileOperationMessage = "File descriptor " + descriptor + " is not open and cannot be seeked";
            return -1;
        }
        if (!(channel instanceof SeekableByteChannel seekableChannel)) {
            this.fileOperationMessage = "File with descriptor " + descriptor + " does not support seeking";
            return -1;
        }

        try {
            long whencePosition = switch (whence) {
                case FROM_START -> 0;
                case FROM_CURRENT -> seekableChannel.position();
                case FROM_END -> seekableChannel.size();
            };
            long newPosition = whencePosition + offset;

            try {
                seekableChannel.position(newPosition);
            }
            catch (IllegalArgumentException exception) {
                this.fileOperationMessage = "Cannot seek from position " + whencePosition + " to " + newPosition + " in file with descriptor " + descriptor;
                return -1;
            }

            this.fileOperationMessage = "Successfully seeked to position " + newPosition + " in file with descriptor " + descriptor;
            return newPosition;
        }
        catch (IOException exception) {
            this.fileOperationMessage = "Failed to seek in file with descriptor " + descriptor;
            return -1;
        }
    }

    /**
     * Virtual representation of a file for the file-related syscalls to use.
     * An instance of this class can be in one of two states: <b>open</b> or <b>closed</b>.
     * <p>
     * In the <b>open</b> state, the instance contains a currently active input/output stream,
     * as well as the name of the stream and which flags it was opened with.
     * <p>
     * In the <b>closed</b> state, the instance instead stores one integer for the index of the
     * next free file descriptor in the list of handles. This allows the list of handles to
     * always know the next closed file descriptor it can open.
     *
     * @author Sean Clarke 04/2024
     */
    public static class FileHandle {
        private String name;
        private Channel channel;
        private int flagsOrNextDescriptor;

        /**
         * Create a new <b>closed</b> handle with the previous head of the closed handle list.
         *
         * @param nextDescriptor The descriptor of the next closed handle following this one.
         */
        public FileHandle(int nextDescriptor) {
            this.name = null;
            this.channel = null;
            this.flagsOrNextDescriptor = nextDescriptor;
        }

        /**
         * Create a new <b>open</b> handle with the given byte channel information.
         *
         * @param name    The name of the byte channel.
         * @param channel The corresponding byte channel.
         * @param flags   The opening flags for this byte channel.
         */
        public FileHandle(String name, Channel channel, int flags) {
            this.open(name, channel, flags);
        }

        /**
         * Open this handle with the given byte channel information.
         * <p>
         * Note: this does not attempt to close an existing byte channel. Ensure {@link #close(int)} is called
         * beforehand if this handle is already open.
         *
         * @param name    The name of the byte channel.
         * @param channel The corresponding byte channel.
         * @param flags   The opening flags for this byte channel.
         */
        public void open(String name, Channel channel, int flags) {
            this.name = name;
            this.channel = channel;
            this.flagsOrNextDescriptor = flags;
        }

        /**
         * Close this handle with the previous head of the closed handle list.
         *
         * @param nextDescriptor The descriptor of the next closed handle following this one.
         */
        public void close(int nextDescriptor) {
            if (this.channel != null) {
                try {
                    this.channel.close();
                }
                catch (IOException exception) {
                    // Ignore
                }
                this.channel = null;
            }
            this.flagsOrNextDescriptor = nextDescriptor;
        }

        /**
         * Determine whether this handle is <b>open</b> or <b>closed</b>.
         *
         * @return <code>true</code> if this handle is <b>open</b>, or <code>false</code> otherwise.
         */
        public boolean isOpen() {
            return this.channel != null;
        }

        /**
         * Get the name of the byte channel used by this handle.
         * <p>
         * Note: only call this method if this handle is <b>open</b>.
         *
         * @return The name of the byte channel.
         */
        public String getName() {
            Objects.requireNonNull(this.channel);
            return this.name;
        }

        /**
         * Get the flags the byte channel was opened with.
         * <p>
         * Note: only call this method if this handle is <b>open</b>.
         *
         * @return The integer flags.
         */
        public int getFlags() {
            Objects.requireNonNull(this.channel);
            return this.flagsOrNextDescriptor;
        }

        /**
         * Get the descriptor of the next closed handle following this one.
         * <p>
         * Note: only call this method if this handle is <b>closed</b>.
         *
         * @return The descriptor of the next handle.
         */
        public int getNextDescriptor() {
            return this.flagsOrNextDescriptor;
        }

        public Channel getChannel() {
            return this.channel;
        }

        @Override
        public String toString() {
            return "Handle " + this.name + " : " + this.channel + " : Flags / next desc : " + this.flagsOrNextDescriptor;
        }
    }

    /**
     * Implements syscall to print a string.
     *
     * @param string The string to print.
     */
    public void printString(String string) {
        if (Application.getGUI() == null) {
            System.out.print(string);
        }
        else {
            Application.getGUI().getMessagesPane().writeToConsole(string);
        }
    }

    /**
     * Implements syscall to read an integer value.
     *
     * @return Integer value entered by user.
     * @throws NumberFormatException Thrown if invalid input is entered.
     */
    public int readInteger() throws NumberFormatException, InterruptedException {
        String input = "";
        if (Application.getGUI() == null) {
            try {
                input = getInputReader().readLine();
            }
            catch (IOException exception) {
                // Leave the input as an empty string
            }
        }
        else {
            if (Application.getSettings().popupSyscallInput.get()) {
                input = Application.getGUI().getMessagesPane().getInputString("Enter an integer value");
            }
            else {
                input = Application.getGUI().getMessagesPane().getInputString(-1);
            }
        }

        // Client is responsible for catching NumberFormatException
        return Integer.parseInt(input.strip());
    }

    /**
     * Implements syscall to read a float value.
     *
     * @return Float value entered by user.
     * @throws NumberFormatException Thrown if invalid input is entered.
     * @author Ken Vollmar Feb 14 2005
     */
    public float readFloat() throws NumberFormatException, InterruptedException {
        String input = "";
        if (Application.getGUI() == null) {
            try {
                input = getInputReader().readLine();
            }
            catch (IOException exception) {
                // Leave the input as an empty string
            }
        }
        else {
            if (Application.getSettings().popupSyscallInput.get()) {
                input = Application.getGUI().getMessagesPane().getInputString("Enter a float value");
            }
            else {
                input = Application.getGUI().getMessagesPane().getInputString(-1);
            }
        }
        return Float.parseFloat(input.strip());
    }

    /**
     * Implements syscall to read a double value.
     *
     * @return Double value entered by user.
     * @throws NumberFormatException Thrown if invalid input is entered.
     * @author DPS 1 Aug 2005, based on Ken Vollmar's {@link #readFloat()}
     */
    public double readDouble() throws NumberFormatException, InterruptedException {
        String input = "";
        if (Application.getGUI() == null) {
            try {
                input = getInputReader().readLine();
            }
            catch (IOException exception) {
                // Leave the input as an empty string
            }
        }
        else {
            if (Application.getSettings().popupSyscallInput.get()) {
                input = Application.getGUI().getMessagesPane().getInputString("Enter a double value");
            }
            else {
                input = Application.getGUI().getMessagesPane().getInputString(-1);
            }
        }
        return Double.parseDouble(input.strip());
    }

    /**
     * Implements syscall to read a string value.
     *
     * @param maxLength The maximum string length.
     * @return String value entered by user, truncated to maximum length if necessary.
     */
    public String readString(int maxLength) throws InterruptedException {
        String input = "";
        if (Application.getGUI() == null) {
            try {
                input = getInputReader().readLine();
            }
            catch (IOException exception) {
                // Leave the input as an empty string
            }
        }
        else {
            if (Application.getSettings().popupSyscallInput.get()) {
                input = Application.getGUI().getMessagesPane().getInputString("Enter a string (maximum " + maxLength + " characters)");
            }
            else {
                input = Application.getGUI().getMessagesPane().getInputString(maxLength);
                if (input.endsWith("\n")) {
                    input = input.substring(0, input.length() - 1);
                }
            }
        }

        // Trim the output if a maximum length is set
        if (maxLength > 0 && input.length() > maxLength) {
            return input.substring(0, maxLength);
        }
        else {
            return input;
        }
    }

    /**
     * Implements syscall to read a char value.
     *
     * @return Integer value with least significant byte corresponding to user input.
     * @throws IndexOutOfBoundsException Thrown if invalid input is entered.
     */
    public int readChar() throws IndexOutOfBoundsException, InterruptedException {
        String input = "";
        if (Application.getGUI() == null) {
            try {
                input = getInputReader().readLine();
            }
            catch (IOException exception) {
                // Leave the input as an empty string
            }
        }
        else {
            if (Application.getSettings().popupSyscallInput.get()) {
                input = Application.getGUI().getMessagesPane().getInputString("Enter a character value");
            }
            else {
                input = Application.getGUI().getMessagesPane().getInputString(1);
            }
        }
        // Throws index-out-of-bounds exception!
        return input.charAt(0); // first character input
    }
}

