package mars.mips.instructions.syscalls;

import mars.assembler.BasicStatement;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.Processor;
import mars.simulator.ExceptionCause;
import mars.simulator.SimulatorException;
import mars.tools.MarsTool;
import mars.venus.ToolManager;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

/**
 * Service to open a MARS tool in the GUI as if the menu action was clicked.
 */
public class SyscallQueryTool extends AbstractSyscall {
    /**
     * Build an instance of the syscall with its default service number and name.
     */
    @SuppressWarnings("unused")
    public SyscallQueryTool() {
        super(64, "QueryTool");
    }

    /**
     * System call to display a message to user.
     */
    @Override
    public void simulate(BasicStatement statement) throws SimulatorException, InterruptedException {
        this.requireGUI(statement);

        try {
            // Read a null-terminated string from memory
            String identifier = Memory.getInstance().fetchNullTerminatedString(Processor.getValue(Processor.ARGUMENT_0));
            MarsTool tool = ToolManager.getTool(identifier);
            if (tool == null) {
                throw new SimulatorException(
                    statement,
                    "could not find a tool matching identifier '" + identifier + "'",
                    ExceptionCause.SYSCALL
                );
            }

            int key = Processor.getValue(Processor.ARGUMENT_1);

            SwingUtilities.invokeAndWait(() -> tool.handleQuery(key));
        }
        catch (AddressErrorException exception) {
            throw new SimulatorException(statement, exception);
        }
        catch (InvocationTargetException exception) {
            throw new SimulatorException(
                statement,
                "exception while querying tool: " + exception.getCause().getMessage(),
                ExceptionCause.SYSCALL
            );
        }
    }
}