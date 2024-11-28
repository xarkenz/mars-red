package mars.mips.instructions.syscalls;

import mars.simulator.SimulatorException;
import mars.assembler.BasicStatement;
import mars.mips.hardware.Processor;
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
        int descriptor = Processor.getValue(Processor.ARGUMENT_0); // $a0: file descriptor
        int offset = Processor.getValue(Processor.ARGUMENT_1); // $a1: position offset
        int whenceOrdinal = Processor.getValue(Processor.ARGUMENT_2); // $a2: position whence

        SystemIO.SeekWhence whence = SystemIO.SeekWhence.valueOf(whenceOrdinal);
        if (whence == null) {
            throw new SimulatorException(statement, "Invalid whence value in $a2 for " + this.getName() + " (syscall " + this.getNumber() + ")", ExceptionCause.SYSCALL);
        }

        long newPosition = Simulator.getInstance().getSystemIO().seekFile(descriptor, offset, whence);
        Processor.setValue(Processor.VALUE_0, (int) newPosition); // Put return value in $v0
    }
}
