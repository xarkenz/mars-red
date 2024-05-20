package mars.mips.instructions.syscalls;

import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.hardware.RegisterFile;
import mars.simulator.Simulator;
import mars.simulator.SystemIO;

/**
 * Service to seek position of file descriptor given in $a0.  $a1 specifies offset
 * and $a2 specifies whence.  New position of file descriptor is returned in $v0.
 */
public class SyscallSeek extends AbstractSyscall {
    /**
     * Build an instance of the Seek file position syscall.  Default service number
     * is 61 and name is "Seek".
     */
    public SyscallSeek() {
        super(61, "Seek");
    }

    /**
     * Performs syscall function to seek position of file descriptor given in $a0.  $a1 specifies offset
     * and $a2 specifies whence.  New position of file descriptor is returned in $v0.
     */
    @Override
    public void simulate(ProgramStatement statement) throws ProcessingException {
        int descriptor = RegisterFile.getValue(4); // $a0: file descriptor
        int offset = RegisterFile.getValue(5); // $a1: position offset
        int whence = RegisterFile.getValue(6); // $a2: position whence

        SystemIO.SeekWhence[] values = SystemIO.SeekWhence.values();
        if (whence < 0 || whence >= values.length) {
            throw new ProcessingException(statement, "Whence value in $a2 must be between 0 and " + (values.length-1) + " for " + getName() + " (syscall " + getNumber() + ")");
        }
        SystemIO.SeekWhence whenceValue = values[whence];

        long newPosition = Simulator.getInstance().getSystemIO().seekFile(descriptor, offset, whenceValue);
        RegisterFile.updateRegister(2, (int) newPosition); // Put return value in $v0
    }
}
