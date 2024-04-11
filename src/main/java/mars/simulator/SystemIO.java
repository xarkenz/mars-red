package mars.simulator;

import mars.Application;

import java.io.*;
import java.util.ArrayList;

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
 * Provides standard i/o services needed to simulate the MIPS syscall
 * routines.  These methods will detect whether the simulator is being
 * run from the command line or through the GUI, then do I/O to
 * System.in and System.out in the former situation, and interact with
 * the GUI in the latter.
 *
 * @author Pete Sanderson and Ken Vollmar
 * @version August 2003-2005
 */
public class SystemIO {
    public static final int O_RDONLY = 0x00000000; // Open the file for read access
    public static final int O_WRONLY = 0x00000001; // Open the file for write access
    public static final int O_RDWR = 0x00000002; // Open the file for both reading and writing
    public static final int O_APPEND = 0x00000008; // Always write data to the end of the file
    public static final int O_CREAT = 0x00000200; // Create the file if it doesn't already exist
    public static final int O_EXCL = 0x00000800; // Used in conjunction with O_CREAT, fails if file already exists

    // File descriptors for standard I/O channels
    public static final int STDIN_DESCRIPTOR = 0;
    public static final int STDOUT_DESCRIPTOR = 1;
    public static final int STDERR_DESCRIPTOR = 2;
    public static final int FIRST_USER_DESCRIPTOR = 3;

    private static BufferedReader inputReader = null;

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

    private ArrayList<FileHandle> handles;
    private int nextDescriptor;
    private String fileOperationMessage = null;

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
        this.handles.add(new FileHandle("(stdin)", System.in, O_RDONLY));
        this.handles.add(new FileHandle("(stdout)", System.out, O_WRONLY));
        this.handles.add(new FileHandle("(stderr)", System.err, O_WRONLY));
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

    public FileHandle getOpenHandle(int descriptor) {
        if (0 <= descriptor && descriptor < this.handles.size()) {
            FileHandle handle = this.handles.get(descriptor);
            if (handle.isOpen()) {
                return handle;
            }
        }
        return null;
    }

    public InputStream getInputStream(int descriptor) {
        FileHandle handle = this.getOpenHandle(descriptor);
        if (handle != null) {
            return handle.getInputStream();
        }
        else {
            return null;
        }
    }

    public OutputStream getOutputStream(int descriptor) {
        FileHandle handle = this.getOpenHandle(descriptor);
        if (handle != null) {
            return handle.getOutputStream();
        }
        else {
            return null;
        }
    }

    /**
     * Open a file for either reading or writing. Note that read/write combined flag is NOT
     * IMPLEMENTED.  Also note that file permission modes are also NOT IMPLEMENTED.
     *
     * @param filename The path of the file to open.
     * @param flags    0 for read, 1 for write, 9 for append
     * @return File descriptor for the file opened, or -1 if an error occurred.
     * @author Ken Vollmar
     */
    public int openFile(String filename, int flags) {
        Closeable stream;
        if (flags == O_RDONLY) {
            // Open for reading only
            try {
                // Attempt to set up input stream from file on disk
                stream = new FileInputStream(filename);
            }
            catch (FileNotFoundException exception) {
                fileOperationMessage = "File \"" + filename + "\" not found; unable to open for reading.";
                return -1;
            }
        }
        else if ((flags & O_WRONLY) != 0) {
            // Open for writing only
            try {
                // Attempt to set up output stream to file on disk
                stream = new FileOutputStream(filename, ((flags & O_APPEND) != 0));
            }
            catch (FileNotFoundException exception) {
                fileOperationMessage = "File \"" + filename + "\" not found; unable to open for writing.";
                return -1;
            }
        }
        else {
            fileOperationMessage = "Invalid flags for opening file \"" + filename + "\": " + flags + ".";
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
        handle.open(filename, stream, flags);

        fileOperationMessage = "Successfully opened file \"" + filename + "\".";
        return descriptor;
    }

    /**
     * Close the file with specified file descriptor. Any errors are ignored.
     */
    public void closeFile(int descriptor) {
        // Can't close STDIN, STDOUT, STDERR, or invalid descriptor
        if (FIRST_USER_DESCRIPTOR <= descriptor || descriptor < this.handles.size()) {
            this.handles.get(descriptor).close(this.nextDescriptor);
            this.nextDescriptor = descriptor;
        }
    }

    /**
     * Write bytes to file.
     *
     * @param descriptor      file descriptor
     * @param buffer          byte array containing characters to write
     * @param lengthRequested number of bytes to write
     * @return number of bytes written, or -1 on error
     */
    public int writeToFile(int descriptor, byte[] buffer, int lengthRequested) {
        /////////////// DPS 8-Jan-2013  ////////////////////////////////////////////////////
        // Write to STDOUT or STDERR file descriptor while using IDE - write to Messages pane.
        if ((descriptor == STDOUT_DESCRIPTOR || descriptor == STDERR_DESCRIPTOR) && Application.getGUI() != null) {
            String data = new String(buffer);
            Application.getGUI().getMessagesPane().writeToConsole(data);
            return data.length();
        }
        ///////////////////////////////////////////////////////////////////////////////////
        // When running in command mode, code below works for either regular file or STDOUT/STDERR.

        // Retrieve the output stream corresponding to the descriptor
        OutputStream outputStream = this.getOutputStream(descriptor);
        // Ensure the file descriptor has been opened for writing
        if (outputStream == null) {
            this.fileOperationMessage = "File with descriptor " + descriptor + " cannot be written to";
            return -1;
        }
        try {
            // Oct. 9 2005 Ken Vollmar
            // Observation: made a call to outputStream.write(buffer, 0, lengthRequested)
            //     with buffer containing 6(ten) 32-bit-words <---> 24(ten) bytes, where the
            //     words are MIPS integers with values such that many of the bytes are ZEROES.
            //     The effect is apparently that the write stops after encountering a zero-valued
            //     byte. (The method write does not return a value and so this can't be verified
            //     by the return value.)
            // Writes up to lengthRequested bytes of data to this output stream from an array of bytes.
            // outputStream.write(buffer, 0, lengthRequested); // write is a void method -- no verification value returned

            // Oct. 9 2005 Ken Vollmar  Force the write statement to write exactly
            // the number of bytes requested, even though those bytes include many ZERO values.
            for (int index = 0; index < lengthRequested; index++) {
                outputStream.write(buffer[index]);
            }
            outputStream.flush(); // DPS 7-Jan-2013
        }
        catch (IOException exception) {
            fileOperationMessage = "IO Exception on write of file with descriptor " + descriptor;
            return -1;
        }
        catch (IndexOutOfBoundsException exception) {
            fileOperationMessage = "IndexOutOfBoundsException on write of file with descriptor" + descriptor;
            return -1;
        }

        return lengthRequested;
    }

    /**
     * Read bytes from file.
     *
     * @param descriptor      file descriptor
     * @param buffer          byte array to contain bytes read
     * @param lengthRequested number of bytes to read
     * @return number of bytes read, 0 on EOF, or -1 on error
     */
    public int readFromFile(int descriptor, byte[] buffer, int lengthRequested) {
        /////////////// DPS 8-Jan-2013  //////////////////////////////////////////////////
        // Read from STDIN file descriptor while using IDE - get input from Messages pane.
        if (descriptor == STDIN_DESCRIPTOR && Application.getGUI() != null) {
            String input = Application.getGUI().getMessagesPane().getInputString(lengthRequested);
            byte[] bytesRead = input.getBytes();
            for (int i = 0; i < buffer.length; i++) {
                buffer[i] = (i < bytesRead.length) ? bytesRead[i] : 0;
            }
            return Math.min(buffer.length, bytesRead.length);
        }
        ////////////////////////////////////////////////////////////////////////////////////
        // When running in command mode, code below works for either regular file or STDIN.

        // Retrieve the input stream corresponding to the descriptor
        InputStream inputStream = this.getInputStream(descriptor);
        if (inputStream == null) {
            // Check the existence of the "read" descriptor
            fileOperationMessage = "File descriptor " + descriptor + " is not open for reading";
            return -1;
        }
        try {
            // Reads up to lengthRequested bytes of data from this Input stream into an array of bytes.
            int lengthRead = inputStream.read(buffer, 0, lengthRequested);
            // This method will return -1 upon EOF, but our spec says that negative
            // value represents an error, so we return 0 for EOF.  DPS 10-July-2008.
            return lengthRead == -1 ? 0 : lengthRead;
        }
        catch (IOException e) {
            fileOperationMessage = "IOException on read of file with descriptor " + descriptor;
            return -1;
        }
        catch (IndexOutOfBoundsException e) {
            fileOperationMessage = "IndexOutOfBoundsException on read of file with descriptor " + descriptor;
            return -1;
        }
    }

    public static class FileHandle {
        private String name;
        private Closeable stream;
        private int flagsOrNextDescriptor;

        public FileHandle(int nextDescriptor) {
            this.name = null;
            this.stream = null;
            this.flagsOrNextDescriptor = nextDescriptor;
        }

        public FileHandle(String name, Closeable stream, int flags) {
            this.open(name, stream, flags);
        }

        public void open(String name, Closeable stream, int flags) {
            this.name = name;
            this.stream = stream;
            this.flagsOrNextDescriptor = flags;
        }

        public void close(int nextDescriptor) {
            if (this.stream != null) {
                try {
                    this.stream.close();
                }
                catch (IOException exception) {
                    // Ignore
                }
                this.stream = null;
            }
            this.flagsOrNextDescriptor = nextDescriptor;
        }

        public boolean isOpen() {
            return this.stream != null;
        }

        public String getName() {
            return this.name;
        }

        public int getFlags() {
            return this.flagsOrNextDescriptor;
        }

        public int getNextDescriptor() {
            return this.flagsOrNextDescriptor;
        }

        public InputStream getInputStream() {
            if (this.stream instanceof InputStream inputStream) {
                return inputStream;
            }
            else {
                return null;
            }
        }

        public OutputStream getOutputStream() {
            if (this.stream instanceof OutputStream outputStream) {
                return outputStream;
            }
            else {
                return null;
            }
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
    public int readInteger() throws NumberFormatException {
        String input = "";
        if (Application.getGUI() == null) {
            try {
                input = getInputReader().readLine();
            }
            catch (IOException ignored) {
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
        return Integer.parseInt(input.trim());
    }

    /**
     * Implements syscall to read a float value.
     *
     * @return Float value entered by user.
     * @throws NumberFormatException Thrown if invalid input is entered.
     * @author Ken Vollmar Feb 14 2005
     */
    public float readFloat() throws NumberFormatException {
        String input = "";
        if (Application.getGUI() == null) {
            try {
                input = getInputReader().readLine();
            }
            catch (IOException ignored) {
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
        return Float.parseFloat(input.trim());
    }

    /**
     * Implements syscall to read a double value.
     *
     * @return Double value entered by user.
     * @throws NumberFormatException Thrown if invalid input is entered.
     * @author DPS 1 Aug 2005, based on Ken Vollmar's {@link #readFloat()}
     */
    public double readDouble() throws NumberFormatException {
        String input = "";
        if (Application.getGUI() == null) {
            try {
                input = getInputReader().readLine();
            }
            catch (IOException ignored) {
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
        return Double.parseDouble(input.trim());
    }

    /**
     * Implements syscall to read a string value.
     *
     * @param maxLength The maximum string length.
     * @return String value entered by user, truncated to maximum length if necessary.
     */
    public String readString(int maxLength) {
        String input = "";
        if (Application.getGUI() == null) {
            try {
                input = getInputReader().readLine();
            }
            catch (IOException ignored) {
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

        if (input.length() > maxLength) {
            // Modified DPS 13-July-2011.  Originally: return input.substring(0, maxLength);
            return (maxLength <= 0) ? "" : input.substring(0, maxLength);
        }
        else {
            return input;
        }
    }

    /**
     * Implements syscall to read a char value.
     *
     * @return Integer value with leas byte corresponding to user input.
     * @throws IndexOutOfBoundsException Thrown if invalid input is entered.
     */
    public int readChar() throws IndexOutOfBoundsException {
        String input = "";
        if (Application.getGUI() == null) {
            try {
                input = getInputReader().readLine();
            }
            catch (IOException ignored) {
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
        // The whole try-catch is not really necessary in this case since I'm
        // just propagating the runtime exception (the default behavior), but
        // I want to make it explicit.  The client needs to catch it.
        try {
            return input.charAt(0); // first character input
        }
        catch (IndexOutOfBoundsException exception) {
            // no chars present
            throw exception; // was: returnValue = 0;
        }
    }
}

