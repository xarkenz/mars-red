package mars.venus.actions.run;

import mars.*;
import mars.mips.hardware.Coprocessor0;
import mars.mips.hardware.Coprocessor1;
import mars.mips.hardware.Memory;
import mars.mips.hardware.RegisterFile;
import mars.util.FilenameFinder;
import mars.util.SystemIO;
import mars.venus.*;
import mars.venus.actions.VenusAction;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
 
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
    private static ArrayList<Program> MIPSprogramsToAssemble;
    // Threshold for adding filename to printed message of files being assembled.
    private static final int LINE_LENGTH_LIMIT = 60;

    public RunAssembleAction(VenusUI gui, String name, Icon icon, String description, Integer mnemonic, KeyStroke accel) {
        super(gui, name, icon, description, mnemonic, accel);
    }

    // These are both used by RunResetAction to re-assemble under identical conditions.
    public static ArrayList<Program> getProgramsToAssemble() {
        return MIPSprogramsToAssemble;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        String name = this.getValue(Action.NAME).toString();
        ExecutePane executePane = gui.getMainPane().getExecutePane();
        RegistersPane registersPane = gui.getRegistersPane();
        if (FileStatus.getFile() != null) {
            if (FileStatus.get() == FileStatus.EDITED) {
                gui.getEditor().save();
            }
            try {
                Globals.program = new Program();
                ArrayList<String> filesToAssemble;
                if (Globals.getSettings().assembleAllEnabled.get()) {
                    // Setting calls for multiple file assembly
                    filesToAssemble = FilenameFinder.getFilenameList(new File(FileStatus.getName()).getParent(), Globals.FILE_EXTENSIONS);
                }
                else {
                    filesToAssemble = new ArrayList<>();
                    filesToAssemble.add(FileStatus.getName());
                }
                String exceptionHandler = null;
                if (Globals.getSettings().exceptionHandlerEnabled.get() && Globals.getSettings().exceptionHandlerPath.get() != null && !Globals.getSettings().exceptionHandlerPath.get().isEmpty()) {
                    exceptionHandler = Globals.getSettings().exceptionHandlerPath.get();
                }
                MIPSprogramsToAssemble = Globals.program.prepareFilesForAssembly(filesToAssemble, FileStatus.getFile().getPath(), exceptionHandler);
                gui.getMessagesPane().writeToMessages(buildFileNameList(name + ": assembling ", MIPSprogramsToAssemble));
                // added logic to receive any warnings and output them.... DPS 11/28/06
                ErrorList warnings = Globals.program.assemble(MIPSprogramsToAssemble, Globals.getSettings().extendedAssemblerEnabled.get(), Globals.getSettings().warningsAreErrors.get());
                if (warnings.warningsOccurred()) {
                    gui.getMessagesPane().writeToMessages(warnings.generateWarningReport());
                }
                gui.getMessagesPane().writeToMessages(name + ": operation completed successfully.\n\n");
                FileStatus.setAssembled(true);
                FileStatus.set(FileStatus.RUNNABLE);
                RegisterFile.resetRegisters();
                Coprocessor1.resetRegisters();
                Coprocessor0.resetRegisters();
                executePane.getTextSegmentWindow().setupTable();
                executePane.getDataSegmentWindow().setupTable();
                executePane.getDataSegmentWindow().highlightCellForAddress(Memory.dataBaseAddress);
                executePane.getDataSegmentWindow().clearHighlighting();
                executePane.getLabelsWindow().setupTable();
                executePane.getTextSegmentWindow().setCodeHighlighting(true);
                executePane.getTextSegmentWindow().highlightStepAtPC();
                registersPane.getRegistersWindow().clearWindow();
                registersPane.getCoprocessor1Window().clearWindow();
                registersPane.getCoprocessor0Window().clearWindow();
                VenusUI.setReset(true);
                VenusUI.setStarted(false);
                gui.getMainPane().setSelectedComponent(executePane);

                // Aug. 24, 2005 Ken Vollmar
                SystemIO.resetFiles();  // Ensure that I/O "file descriptors" are initialized for a new program run
            }
            catch (ProcessingException pe) {
                String errorReport = pe.errors().generateErrorAndWarningReport();
                gui.getMessagesPane().writeToMessages(errorReport);
                gui.getMessagesPane().writeToMessages(name + ": operation completed with errors.\n\n");
                // Select editor line containing first error, and corresponding error message.
                ArrayList<ErrorMessage> errorMessages = pe.errors().getErrorMessages();
                for (ErrorMessage message : errorMessages) {
                    // No line or position may mean File Not Found (e.g. exception file). Don't try to open. DPS 3-Oct-2010
                    if (message.getLine() == 0 && message.getPosition() == 0) {
                        continue;
                    }
                    if (!message.isWarning() || Globals.getSettings().warningsAreErrors.get()) {
                        Globals.getGUI().getMessagesPane().selectErrorMessage(message.getFilename(), message.getLine(), message.getPosition());
                        // Bug workaround: Line selection does not work correctly for the JEditTextArea editor
                        // when the file is opened then automatically assembled (assemble-on-open setting).
                        // Automatic assemble happens in EditTabbedPane's openFile() method, by invoking
                        // this method (actionPerformed) explicitly with null argument.  Thus e!=null test.
                        // DPS 9-Aug-2010
                        if (event != null) {
                            Globals.getGUI().getMessagesPane().selectEditorTextLine(message.getFilename(), message.getLine(), message.getPosition());
                        }
                        break;
                    }
                }
                FileStatus.setAssembled(false);
                FileStatus.set(FileStatus.NOT_EDITED);
            }
        }
    }

    /**
     * Handy little utility for building comma-separated list of filenames
     * while not letting line length get out of hand.
     */
    private String buildFileNameList(String preamble, ArrayList<Program> programList) {
        StringBuilder result = new StringBuilder(preamble);
        int lineLength = result.length();
        for (int i = 0; i < programList.size(); i++) {
            String filename = programList.get(i).getFilename();
            result.append(filename).append((i < programList.size() - 1) ? ", " : "");
            lineLength += filename.length();
            if (lineLength > LINE_LENGTH_LIMIT) {
                result.append('\n');
                lineLength = 0;
            }
        }
        return result + ((lineLength == 0) ? "" : "\n") + '\n';
    }
}
