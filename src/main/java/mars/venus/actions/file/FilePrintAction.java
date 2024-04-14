package mars.venus.actions.file;

import mars.venus.editor.FileEditorTab;
import mars.util.HardcopyWriter;
import mars.venus.actions.VenusAction;
import mars.venus.VenusUI;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/*
Copyright (c) 2003-2006,  Pete Sanderson and Kenneth Vollmar

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
 * Action for the File -> Print menu item
 */
public class FilePrintAction extends VenusAction {
    public FilePrintAction(VenusUI gui, String name, Icon icon, String description, Integer mnemonic, KeyStroke accel) {
        super(gui, name, icon, description, mnemonic, accel);
    }

    /**
     * Uses the {@link HardcopyWriter} class developed by David Flanagan for the book
     * "Java Examples in a Nutshell".  It will do basic printing of multi-page
     * text documents.  It displays a print dialog but does not act on any
     * changes the user may have specified there, such as number of copies.
     *
     * @param event component triggering this call
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        FileEditorTab fileEditorTab = gui.getMainPane().getCurrentEditorTab();
        if (fileEditorTab == null) {
            return;
        }
        int fontSize = 10; // fixed at 10 point
        double margin = 0.5; // all margins (left,right,top,bottom) fixed at 0.5"
        HardcopyWriter output;
        try {
            output = new HardcopyWriter(gui, fileEditorTab.getFilename(), fontSize, margin, margin, margin, margin);
        }
        catch (HardcopyWriter.PrintCanceledException pce) {
            return;
        }
        BufferedReader input = new BufferedReader(new StringReader(fileEditorTab.getSource()));
        int lineNumberDigits = Integer.toString(fileEditorTab.getSourceLineCount()).length();
        int lineNumber = 0;
        try {
            String line = input.readLine();
            while (line != null) {
                StringBuilder formattedLine = new StringBuilder();
                if (fileEditorTab.showingLineNumbers()) {
                    lineNumber++;
                    formattedLine.append(lineNumber).append(": ");
                    while (formattedLine.length() < lineNumberDigits) {
                        formattedLine.append(' ');
                    }
                }
                formattedLine.append(line).append('\n');
                output.write(formattedLine.toString(), 0, formattedLine.length());
                line = input.readLine();
            }
            input.close();
            output.close();
        }
        catch (IOException ioe) {
            // No action
        }
    }
}
