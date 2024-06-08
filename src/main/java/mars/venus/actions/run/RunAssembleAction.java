package mars.venus.actions.run;

import mars.*;
import mars.mips.hardware.*;
import mars.simulator.Simulator;
import mars.util.FilenameFinder;
import mars.venus.*;
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
 * Action for the Run -> Assemble menu item (and toolbar icon)
 */
public class RunAssembleAction extends VenusAction {
    /**
     * Threshold for adding filename to printed message of files being assembled.
     */
    private static final int LINE_LENGTH_LIMIT = 60;

    private static List<Program> programsToAssemble;

    public RunAssembleAction(VenusUI gui, String name, Icon icon, String description, Integer mnemonic, KeyStroke accel) {
        super(gui, name, icon, description, mnemonic, accel);
    }

    /**
     * This is used by {@link RunResetAction} to re-assemble under identical conditions.
     */
    public static List<Program> getProgramsToAssemble() {
        return programsToAssemble;
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

        // Generate the list of files to assemble
        String leadPathname = editTab.getCurrentEditorTab().getFile().getPath();
        List<String> pathnames;
        if (Application.getSettings().assembleAllEnabled.get()) {
            pathnames = FilenameFinder.findFilenames(editTab.getCurrentEditorTab().getFile().getParent(), Application.FILE_EXTENSIONS);
        }
        else {
            pathnames = List.of(leadPathname);
        }

        // Get the path of the exception handler, if enabled
        String exceptionHandler = null;
        if (Application.getSettings().exceptionHandlerEnabled.get()) {
            exceptionHandler = Application.getSettings().exceptionHandlerPath.get();
        }

        try {
            Simulator.getInstance().reset();

            Application.program = new Program();
            programsToAssemble = Application.program.prepareFilesForAssembly(pathnames, leadPathname, exceptionHandler);
            this.gui.getMessagesPane().writeToMessages(this.buildFileNameList(this.getName() + ": assembling ", programsToAssemble));

            // Added logic to receive any warnings and output them.  DPS 11/28/06
            ErrorList warnings = Application.program.assemble(programsToAssemble, Application.getSettings().extendedAssemblerEnabled.get(), Application.getSettings().warningsAreErrors.get());

            if (warnings.warningsOccurred()) {
                this.gui.getMessagesPane().writeToMessages(warnings.generateWarningReport());
                this.gui.getMessagesPane().selectMessagesTab();
            }
            else {
                this.gui.getMessagesPane().writeToMessages(this.getName() + ": operation completed successfully.\n");
            }
            if (this.gui.getProgramStatus() == ProgramStatus.PAUSED) {
                this.gui.getMessagesPane().writeToConsole("\n--- program terminated by user ---\n\n");
            }

            this.gui.setProgramStatus(ProgramStatus.NOT_STARTED);

            executeTab.getTextSegmentWindow().setupTable();
            executeTab.getDataSegmentWindow().setupTable();
            executeTab.getDataSegmentWindow().highlightCellForAddress(Memory.getInstance().getAddress(MemoryConfigurations.STATIC_LOW));
            executeTab.getDataSegmentWindow().clearHighlighting();
            executeTab.getLabelsWindow().setupTable();
            executeTab.getTextSegmentWindow().setCodeHighlighting(true);
            executeTab.getTextSegmentWindow().highlightStepAtPC();
            this.gui.getMainPane().setSelectedComponent(executeTab);

            registersPane.getRegistersWindow().clearWindow();
            registersPane.getCoprocessor1Window().clearWindow();
            registersPane.getCoprocessor0Window().clearWindow();
        }
        catch (ProcessingException exception) {
            String errorReport = exception.getErrors().generateErrorAndWarningReport();
            this.gui.getMessagesPane().writeToMessages(errorReport);
            this.gui.getMessagesPane().writeToMessages(getName() + ": operation completed with errors.\n");
            this.gui.getMessagesPane().selectMessagesTab();

            // Select editor line containing first error, and corresponding error message.
            ArrayList<ErrorMessage> errorMessages = exception.getErrors().getErrorMessages();
            for (ErrorMessage message : errorMessages) {
                // No line or position may mean File Not Found (e.g. exception file). Don't try to open. DPS 3-Oct-2010
                if (message.getLine() == 0 && message.getPosition() == 0) {
                    continue;
                }
                if (!message.isWarning() || Application.getSettings().warningsAreErrors.get()) {
                    this.gui.getMessagesPane().selectErrorMessage(message.getFilename(), message.getLine(), message.getPosition());
                    // Bug workaround: Line selection does not work correctly for the JEditTextArea editor
                    // when the file is opened then automatically assembled (assemble-on-open setting).
                    // Automatic assemble happens in EditTabbedPane's openFile() method, by invoking
                    // this method (actionPerformed) explicitly with null argument.  Thus e!=null test.
                    // DPS 9-Aug-2010
                    if (event != null) {
                        this.gui.getMessagesPane().selectEditorTextLine(message.getFilename(), message.getLine(), message.getPosition());
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

    /**
     * Handy little utility for building comma-separated list of filenames
     * while not letting line length get out of hand.
     */
    private String buildFileNameList(String preamble, List<Program> programList) {
        StringBuilder result = new StringBuilder(preamble);
        int lineLength = result.length();
        for (int i = 0; i < programList.size(); i++) {
            String filename = programList.get(i).getFilename();
            result.append(filename);
            if (i < programList.size() - 1) {
                result.append(", ");
            }
            lineLength += filename.length();
            if (lineLength > LINE_LENGTH_LIMIT) {
                result.append('\n');
                lineLength = 0;
            }
        }
        if (lineLength > 0) {
            result.append('\n');
        }
        return result.toString();
    }
}
