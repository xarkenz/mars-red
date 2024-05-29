package mars.simulator;

import mars.Application;

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
     * Open the file for read access.
     */
    public static final int O_RDONLY = 0x00000000;
    /**
     * Open the file for write access.
     */
    public static final int O_WRONLY = 0x00000001;
    /**
     * Open the file for both reading and writing.
     */
    public static final int O_RDWR = 0x00000002;
    /**
     * Start writing data at the end of the file.
     */
    public static final int O_APPEND = 0x00000008;
    /**
     * Fail write if the file already exists.
     */
    public static final int O_EXCL = 0x00000010;

    public enum SeekWhence {
        /**
         * The file offset is set to <code>offset</code> bytes.
         */
        SEEK_SET,
        /**
         * The file offset is set to its current position plus <code>offset</code> bytes.
         */
        SEEK_CUR,
        /**
         * The file offset is set to the size of the file plus <code>offset</code> bytes.
         */
        SEEK_END
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
     * Set up I/O functionality for the given simulator.
     *
     * @param simulator The simulator this instance will be bound to.
     */
    public SystemIO(Simulator simulator) {
        this.initHandles();
        simulator.addThreadListener(new SimulatorListener() {
            @Override
            public void simulatorFinished(SimulatorFinishEvent event) {
                SystemIO.this.resetFiles();
            }
        });
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
        this.handles.add(new FileHandle("(stdin)", Channels.newChannel(System.in), O_RDONLY));
        this.handles.add(new FileHandle("(stdout)", Channels.newChannel(System.out), O_WRONLY));
        this.handles.add(new FileHandle("(stderr)", Channels.newChannel(System.err), O_WRONLY));
        System.out.flush();
        System.err.flush();
        this.nextDescriptor = this.handles.size();
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
     * @param flags    One of {@link #O_RDONLY}, {@link #O_WRONLY}, {@link #O_WRONLY} | {@link #O_APPEND}, {@link #O_RDWR}.
     * @return File descriptor for the file opened, or -1 if an error occurred.
     * @author Ken Vollmar
     */
    public int openFile(Path filename, int flags) {
        FileChannel channel;
        Set<OpenOption> options = new HashSet<>();
        String mode;

        boolean readOnly = (flags == O_RDONLY),
                writeOnly = ((flags & O_WRONLY) != 0),
                readWrite = ((flags & O_RDWR) != 0),
                append = ((flags & O_APPEND) != 0),
                createNew = ((flags & O_EXCL) != 0);

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
            fileOperationMessage = "Invalid flags for opening file \"" + filename + "\": " + flags + ".";
            return -1;
        }

        try {
            channel = FileChannel.open(filename, options);
        }
        catch (IOException exception) {
            fileOperationMessage = "File \"" + filename + "\" unable to open for " + mode + ".";
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

        printHandlesForDebug();

        fileOperationMessage = "Successfully opened file \"" + filename + "\".";
        return descriptor;
    }

    /**
     * Close the file with specified file descriptor. Any errors are ignored.
     */
    public void closeFile(int descriptor) {
        // Can't close STDIN, STDOUT, STDERR, or invalid descriptor
        if (FIRST_USER_DESCRIPTOR <= descriptor && descriptor < this.handles.size()) {
            this.handles.get(descriptor).close(this.nextDescriptor);
            this.nextDescriptor = descriptor;

            printHandlesForDebug();

            fileOperationMessage = "Successfully closed file descriptor \"" + descriptor + "\".";
        }
        else {
            fileOperationMessage = "Couldn't close file descriptor \"" + descriptor + "\".";
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
     * @param descriptor      Target file descriptor.
     * @param buffer          Byte buffer containing characters to write.
     * @return Number of bytes written, or -1 on error.
     */
    public int writeToFile(int descriptor, ByteBuffer buffer) {
        /////////////// DPS 8-Jan-2013  ////////////////////////////////////////////////////
        // Write to STDOUT or STDERR file descriptor while using IDE - write to Messages pane.
        if ((descriptor == STDOUT_DESCRIPTOR || descriptor == STDERR_DESCRIPTOR) && Application.getGUI() != null) {
            // Sean Clarke (05/2024): originally used buffer.array() but was getting UnsupportedOperationException
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            Application.getGUI().getMessagesPane().writeToConsole(new String(data));
            return data.length;
        }
        ///////////////////////////////////////////////////////////////////////////////////
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
            return channel.write(buffer);
        }
        catch (NonWritableChannelException exception) {
            this.fileOperationMessage = "File with descriptor " + descriptor + " is not open for writing";
            return -1;
        }
        catch (IOException exception) {
            this.fileOperationMessage = "IO Exception on write of file with descriptor " + descriptor;
            return -1;
        }
        catch (IndexOutOfBoundsException exception) {
            this.fileOperationMessage = "IndexOutOfBoundsException on write of file with descriptor" + descriptor;
            return -1;
        }
    }

    /**
     * Read bytes from file.
     *
     * @param descriptor      Target file descriptor.
     * @param buffer          Byte buffer to contain bytes read. Capacity should be maximum number of bytes to read.
     * @return Number of bytes read, 0 on EOF, or -1 on error.
     */
    public int readFromFile(int descriptor, ByteBuffer buffer) throws InterruptedException {
        /////////////// DPS 8-Jan-2013  //////////////////////////////////////////////////
        // Read from STDIN file descriptor while using IDE - get input from Messages pane.
        if (descriptor == STDIN_DESCRIPTOR && Application.getGUI() != null) {
            String input = Application.getGUI().getMessagesPane().getInputString(buffer.capacity());
            byte[] bytesRead = input.getBytes();
            for (int index = 0; index < buffer.remaining(); index++) {
                buffer.put(index, (index < bytesRead.length) ? bytesRead[index] : 0);
            }
            return Math.min(buffer.remaining(), bytesRead.length);
        }
        ////////////////////////////////////////////////////////////////////////////////////
        // When running in command mode, code below works for either regular file or STDIN.

        // Retrieve the readable channel corresponding to the descriptor
        ReadableByteChannel channel = this.getReadableChannel(descriptor);
        // Ensure the file descriptor has been opened for reading
        if (channel == null) {
            fileOperationMessage = "File descriptor " + descriptor + " is not open for reading";
            return -1;
        }
        try {
            // Reads up to buffer.remaining() bytes of data from this input stream into an array of bytes.
            int lengthRead = channel.read(buffer);
            // This method will return -1 upon EOF, but our spec says that negative
            // value represents an error, so we return 0 for EOF.  DPS 10-July-2008.
            return lengthRead == -1 ? 0 : lengthRead;
        }
        catch (NonReadableChannelException exception) {
            fileOperationMessage = "File descriptor " + descriptor + " is not open for reading";
            return -1;
        }
        catch (IOException exception) {
            fileOperationMessage = "IOException on read of file with descriptor " + descriptor;
            return -1;
        }
        catch (IndexOutOfBoundsException exception) {
            fileOperationMessage = "IndexOutOfBoundsException on read of file with descriptor " + descriptor;
            return -1;
        }
    }

    /**
     * Seek position in file.
     *
     * @param descriptor    Target file descriptor.
     * @param offset        Offset from position specified by whence.
     * @param whence        Directive indicating where in the file to start the offset from.
     * @return New position in file, or -1 on error.
     */
    public long seekFile(int descriptor, long offset, SeekWhence whence) {
        Channel channel = this.getChannel(descriptor);
        if (!(channel instanceof SeekableByteChannel seekableChannel)) {
            fileOperationMessage = "File descriptor " + descriptor + " cannot be seeked";
            return -1;
        }
        try {
            long curPosition = seekableChannel.position();
            long newPosition = switch (whence) {
                case SEEK_SET -> offset;
                case SEEK_CUR -> curPosition + offset;
                case SEEK_END -> seekableChannel.size() + offset;
            };
            seekableChannel.position(newPosition);
            return newPosition;
        }
        catch (IllegalArgumentException exception) {
            fileOperationMessage = "New position cannot be negative for file descriptor " + descriptor;
            return -1;
        }
        catch (IOException exception) {
            fileOperationMessage = "IOException on seeking file with descriptor " + descriptor;
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

