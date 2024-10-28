package mars.mips.instructions.syscalls;

import mars.SimulatorException;
import mars.assembler.BasicStatement;
import mars.mips.hardware.RegisterFile;
import mars.simulator.ExceptionCause;
import mars.simulator.Simulator;
import mars.simulator.SystemIO;

/**
 * Service to seek position of file descriptor given in $a0.  $a1 specifies offset
 * and $a2 specifies whence.  New position of file descriptor is returned in $v0.
 */
public class SyscallSeek extends AbstractSyscall {
    /**
     * Build an instance of the syscall with its default service number and name.
     */
    @SuppressWarnings("unused")
    public SyscallSeek() {
        super(61, "Seek");
    }

    /**
     * Performs syscall function to seek position of file descriptor given in $a0.  $a1 specifies offset
     * and $a2 specifies whence.  New position of file descriptor is returned in $v0.
     */
    @Override
    public void simulate(BasicStatement statement) throws SimulatorException {
        int descriptor = RegisterFile.getValue(4); // $a0: file descriptor
        int offset = RegisterFile.getValue(5); // $a1: position offset
        int whenceOrdinal = RegisterFile.getValue(6); // $a2: position whence

        SystemIO.SeekWhence whence = SystemIO.SeekWhence.valueOf(whenceOrdinal);
        if (whence == null) {
            throw new SimulatorException(statement, "Invalid whence value in $a2 for " + this.getName() + " (syscall " + this.getNumber() + ")", ExceptionCause.SYSCALL_EXCEPTION);
        }

        long newPosition = Simulator.getInstance().getSystemIO().seekFile(descriptor, offset, whence);
        RegisterFile.updateRegister(2, (int) newPosition); // Put return value in $v0
    }
}
