package mars.mips.instructions.syscalls;

import mars.ProcessingException;
import mars.ProgramStatement;
import mars.simulator.Simulator;
import mars.simulator.SystemIO;

/**
 * Service to print the last file operation message.
 */
public class SyscallPrintIOMessage extends AbstractSyscall {
    /**
     * Build an instance of the syscall with its default service number and name.
     */
    @SuppressWarnings("unused")
    public SyscallPrintIOMessage() {
        super(60, "PrintIOMessage");
    }

    /**
     * Performs syscall function to print the most recent file operation message, as defined
     * in {@link SystemIO#getFileOperationMessage()}, followed by a newline.
     */
    @Override
    public void simulate(ProgramStatement statement) throws ProcessingException {
        String fileOperationMessage = Simulator.getInstance().getSystemIO().getFileOperationMessage();
        if (fileOperationMessage == null) {
            return;
        }

        Simulator.getInstance().getSystemIO().printString(fileOperationMessage + '\n');
    }
}
