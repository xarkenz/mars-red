package mars.venus.actions.run;

import mars.Application;
import mars.assembler.log.AssemblyError;
import mars.assembler.log.LogLevel;
import mars.assembler.log.LogMessage;
import mars.mips.hardware.Memory;
import mars.simulator.Simulator;
import mars.venus.RegistersPane;
import mars.venus.VenusUI;
import mars.venus.actions.VenusAction;
import mars.venus.editor.EditTab;
import mars.venus.editor.FileStatus;
import mars.venus.execute.ExecuteTab;
import mars.venus.execute.ProgramStatus;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/*
Copyright (c) 2003-2010,  Pete Sanderson and Kenneth Vollmar

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
 * Action for the Run -> Assemble menu item.
 */
public class RunAssembleAction extends VenusAction {
    public RunAssembleAction(VenusUI gui, Integer mnemonic, KeyStroke accel) {
        super(gui, "Assemble", VenusUI.getSVGActionIcon("assemble.svg"), "Assemble the current file", mnemonic, accel);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        EditTab editTab = this.gui.getMainPane().getEditTab();
        ExecuteTab executeTab = this.gui.getMainPane().getExecuteTab();
        RegistersPane registersPane = this.gui.getRegistersPane();

        // Require that there is a file open and that it is saved before running
        if (!editTab.saveCurrentFile()) {
            return;
        }

        this.gui.getMessagesPane().selectMessagesTab();

        // Generate the list of files to assemble
        List<String> sourceFilenames = new ArrayList<>();
        sourceFilenames.add(editTab.getCurrentEditorTab().getFile().getPath());

        // Get the path of the exception handler, if enabled
        if (this.gui.getSettings().exceptionHandlerEnabled.get()) {
            sourceFilenames.add(this.gui.getSettings().exceptionHandlerPath.get());
        }

        try {
            Simulator.getInstance().reset();
            Simulator.getInstance().getSystemIO().setWorkingDirectory(editTab.getCurrentEditorTab().getFile().getParentFile().toPath());

            this.gui.getMessagesPane().getMessages().writeOutput(this.getName() + ": assembling files.\n");

            Application.assembler.getLog().setOutput(this.gui.getMessagesPane().getMessages()::writeMessage);
            Application.assembler.assembleFilenames(sourceFilenames);

            this.gui.getMessagesPane().getMessages().writeOutput(this.getName() + ": operation completed successfully.\n");

            if (this.gui.getProgramStatus() == ProgramStatus.PAUSED) {
                this.gui.getMessagesPane().getConsole().writeOutput("\n--- program terminated by user ---\n\n");
            }

            this.gui.setProgramStatus(ProgramStatus.NOT_STARTED);

            registersPane.getProcessorTab().resetDisplay();
            registersPane.getCoprocessor1Tab().resetDisplay();
            registersPane.getCoprocessor0Tab().resetDisplay();

            executeTab.getTextSegmentWindow().setupTable();
            executeTab.getDataSegmentWindow().setupTable();
            executeTab.getDataSegmentWindow().highlightCellForAddress(Memory.getInstance().getLayout().staticRange.minAddress());
            executeTab.getDataSegmentWindow().clearHighlighting();
            executeTab.getLabelsWindow().setupTable();
            executeTab.getTextSegmentWindow().updateHighlighting();
            this.gui.getMainPane().setSelectedComponent(executeTab);
        }
        catch (AssemblyError error) {
            this.gui.getMessagesPane().getMessages().writeOutput(this.getName() + ": operation completed with errors.\n");
            this.gui.getMessagesPane().selectMessagesTab();

            // Select editor line containing first error, and corresponding error message.
            for (LogMessage message : error.getLog().getMessages()) {
                if (message.getLevel() == LogLevel.ERROR || (
                    message.getLevel() == LogLevel.WARNING && this.gui.getSettings().warningsAreErrors.get()
                )) {
                    // Bug workaround: Line selection does not work correctly for the JEditTextArea editor
                    // when the file is opened then automatically assembled (assemble-on-open setting).
                    // Automatic assemble happens in EditTabbedPane's openFile() method, by invoking
                    // this method (actionPerformed) explicitly with null argument.  Thus e!=null test.
                    // DPS 9-Aug-2010
                    if (event != null) {
                        this.gui.getMessagesPane().highlightMessageSource(message);
                    }
                    break;
                }
            }

            this.gui.setProgramStatus(ProgramStatus.NOT_ASSEMBLED);
        }
    }

    @Override
    public void update() {
        this.setEnabled(this.gui.getFileStatus() != FileStatus.NO_FILE && this.gui.getProgramStatus() != ProgramStatus.RUNNING);
    }
}
