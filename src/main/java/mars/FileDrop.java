package mars;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.TooManyListenersException;

/**
 * File drop handler that registers drop listeners onto a component and changes the component's
 * borders when dragging files over it.
 * <br>
 * This component comes from <a href="http://www.calebhoff.com/2018/10/mars-plus.html">MARS Plus</a>, a derivation of the original MARS 4.5 created by
 * Caleb Hoff, but the code looks similar to <a href="https://github.com/tferr/Scripts/blob/dc0801d6288fb63d5ddabc7c552b86b5d74459d0/BAR/src/main/java/bar/FileDrop.java">Robert Harder's code</a> which is now in public domain.
 *
 * @author Robert Harder
 */
public class FileDrop {
    private transient Border normalBorder;
    private transient DropTargetListener dropListener;
    private static Boolean supportsDnD;
    private static final Color defaultBorderColor = new Color(0.0F, 0.0F, 1.0F, 0.25F);
    private static final String ZERO_CHAR_STRING = "\u0000";

    public FileDrop(Component editorComponent, FileDrop.Listener listener) {
        this(null, editorComponent, BorderFactory.createMatteBorder(2, 2, 2, 2, defaultBorderColor), true, listener);
    }

    public FileDrop(Component editorComponent, boolean recursive, FileDrop.Listener listener) {
        this(null, editorComponent, BorderFactory.createMatteBorder(2, 2, 2, 2, defaultBorderColor), recursive, listener);
    }

    public FileDrop(PrintStream out, Component editorComponent, FileDrop.Listener listener) {
        this(out, editorComponent, BorderFactory.createMatteBorder(2, 2, 2, 2, defaultBorderColor), false, listener);
    }

    public FileDrop(PrintStream out, Component editorComponent, boolean recursive, FileDrop.Listener listener) {
        this(out, editorComponent, BorderFactory.createMatteBorder(2, 2, 2, 2, defaultBorderColor), recursive, listener);
    }

    public FileDrop(Component editorComponent, Border dragBorder, FileDrop.Listener listener) {
        this(null, editorComponent, dragBorder, false, listener);
    }

    public FileDrop(Component editorComponent, Border dragBorder, boolean recursive, FileDrop.Listener listener) {
        this(null, editorComponent, dragBorder, recursive, listener);
    }

    public FileDrop(PrintStream out, Component editorComponent, Border dragBorder, FileDrop.Listener listener) {
        this(out, editorComponent, dragBorder, false, listener);
    }

    /**
     * Create a new file drop handler for a particular Swing Component.
     *
     * @param out logging stream (nullable)
     * @param editorComponent component to register the file drop listener to
     * @param dragBorder border to change the component's border to on drag over
     * @param recursive whether to register the file drop listener to the component's children
     * @param listener file listener that the handler calls
     */
    public FileDrop(final PrintStream out, final Component editorComponent, final Border dragBorder, boolean recursive, final FileDrop.Listener listener) {
        if (supportsDnD()) {
            this.dropListener = new DropTargetListener() {
                @Override
                public void dragEnter(DropTargetDragEvent evt) {
                    FileDrop.log(out, "FileDrop: dragEnter event.");
                    if (FileDrop.this.isDragOk(out, evt)) {
                        if (editorComponent instanceof JComponent jc) {
                            FileDrop.this.normalBorder = jc.getBorder();
                            FileDrop.log(out, "FileDrop: normal border saved.");
                            jc.setBorder(dragBorder);
                            FileDrop.log(out, "FileDrop: drag border set.");
                        }

                        evt.acceptDrag(1);
                        FileDrop.log(out, "FileDrop: event accepted.");
                    } else {
                        evt.rejectDrag();
                        FileDrop.log(out, "FileDrop: event rejected.");
                    }
                }

                @Override
                public void dragOver(DropTargetDragEvent evt) {
                }

                @Override
                @SuppressWarnings("unchecked")
                public void drop(DropTargetDropEvent evt) {
                    FileDrop.log(out, "FileDrop: drop event.");

                    try {
                        Transferable tr = evt.getTransferable();
                        if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                            evt.acceptDrop(1);
                            FileDrop.log(out, "FileDrop: file list accepted.");
                            List<File> fileList = (List<File>) tr.getTransferData(DataFlavor.javaFileListFlavor);
                            if (listener != null) {
                                listener.filesDropped(fileList.toArray(new File[0]));
                            }

                            evt.getDropTargetContext().dropComplete(true);
                            FileDrop.log(out, "FileDrop: drop complete.");
                        } else {
                            DataFlavor[] flavors = tr.getTransferDataFlavors();
                            boolean handled = false;

                            for (DataFlavor flavor : flavors) {
                                if (flavor.isRepresentationClassReader()) {
                                    evt.acceptDrop(1);
                                    FileDrop.log(out, "FileDrop: reader accepted.");
                                    Reader reader = flavor.getReaderForText(tr);
                                    BufferedReader br = new BufferedReader(reader);
                                    if (listener != null) {
                                        listener.filesDropped(FileDrop.createFileArray(out, br));
                                    }

                                    evt.getDropTargetContext().dropComplete(true);
                                    FileDrop.log(out, "FileDrop: drop complete.");
                                    handled = true;
                                    break;
                                }
                            }

                            if (!handled) {
                                FileDrop.log(out, "FileDrop: not a file list or reader - abort.");
                                evt.rejectDrop();
                            }
                        }
                    } catch (IOException var13) {
                        FileDrop.log(out, "FileDrop: IOException - abort:");
                        var13.printStackTrace(out);
                        evt.rejectDrop();
                    } catch (UnsupportedFlavorException var14) {
                        FileDrop.log(out, "FileDrop: UnsupportedFlavorException - abort:");
                        var14.printStackTrace(out);
                        evt.rejectDrop();
                    } finally {
                        if (editorComponent instanceof JComponent jc) {
                            jc.setBorder(FileDrop.this.normalBorder);
                            FileDrop.log(out, "FileDrop: normal border restored.");
                        }
                    }
                }

                @Override
                public void dragExit(DropTargetEvent evt) {
                    FileDrop.log(out, "FileDrop: dragExit event.");
                    if (editorComponent instanceof JComponent jc) {
                        jc.setBorder(FileDrop.this.normalBorder);
                        FileDrop.log(out, "FileDrop: normal border restored.");
                    }
                }

                @Override
                public void dropActionChanged(DropTargetDragEvent evt) {
                    FileDrop.log(out, "FileDrop: dropActionChanged event.");
                    if (FileDrop.this.isDragOk(out, evt)) {
                        evt.acceptDrag(1);
                        FileDrop.log(out, "FileDrop: event accepted.");
                    } else {
                        evt.rejectDrag();
                        FileDrop.log(out, "FileDrop: event rejected.");
                    }
                }
            };
            this.makeDropTarget(out, editorComponent, recursive);
        } else {
            log(out, "FileDrop: Drag and drop is not supported with this JVM");
        }
    }

    /**
     * Determines whether the JVM/AWT package supports Drag and Drop.
     *
     * @return true if DnD is supported, false otherwise
     */
    @SuppressWarnings("unused")
    private static boolean supportsDnD() {
        if (supportsDnD == null) {

            try {
                Class<?> arbitraryDndClass = Class.forName("java.awt.dnd.DnDConstants");
                supportsDnD = Boolean.TRUE;
            } catch (Exception var2) {
                supportsDnD = Boolean.FALSE;
            }
        }

        return supportsDnD;
    }

    /**
     * Creates a file array from lines from a reader. Used if the filesystem provides names of files
     * when dropping instead of actual File objects.
     *
     * @param out logging stream (nullable)
     * @param bReader buffered reader provided by the filesystem
     * @return the created File array
     */
    private static File[] createFileArray(PrintStream out, BufferedReader bReader) {
        try {
            List<File> list = new ArrayList<>();
            String line;

            while ((line = bReader.readLine()) != null) {
                try {
                    if (!ZERO_CHAR_STRING.equals(line)) {
                        File file = new File(new URI(line));
                        list.add(file);
                    }
                } catch (Exception var5) {
                    log(out, "Error with " + line + ": " + var5.getMessage());
                }
            }

            return list.toArray(new File[0]);
        } catch (IOException var6) {
            log(out, "FileDrop: IOException");
            return new File[0];
        }
    }

    /**
     * Registers the file drop handler's provided listener with the provided component.
     *
     * @param out logging stream (nullable)
     * @param editorComponent component to attach the listener to
     * @param recursive whether to register the listener to the component's children
     */
    private void makeDropTarget(final PrintStream out, final Component editorComponent, boolean recursive) {
        DropTarget dt = new DropTarget();

        try {
            dt.addDropTargetListener(this.dropListener);
        } catch (TooManyListenersException var8) {
            var8.printStackTrace(out);
            log(out, "FileDrop: Drop will not work due to previous error. Do you have another listener attached?");
        }

        editorComponent.addHierarchyListener(evt -> {
            FileDrop.log(out, "FileDrop: Hierarchy changed.");
            Component parent = editorComponent.getParent();
            if (parent == null) {
                editorComponent.setDropTarget(null);
                FileDrop.log(out, "FileDrop: Drop target cleared from component.");
            } else {
                new DropTarget(editorComponent, FileDrop.this.dropListener);
                FileDrop.log(out, "FileDrop: Drop target added to component.");
            }
        });
        if (editorComponent.getParent() != null) {
            new DropTarget(editorComponent, this.dropListener);
        }

        if (recursive && editorComponent instanceof Container cont) {
            Component[] comps = cont.getComponents();

            for (Component comp : comps) {
                this.makeDropTarget(out, comp, recursive);
            }
        }
    }

    /**
     * Determines whether the file system returns supported formats for dropping files.
     *
     * @param out logging stream (nullable)
     * @param evt event called on dragging an object over the component
     * @return true if the dragging format is supported, false otherwise.
     */
    private boolean isDragOk(PrintStream out, DropTargetDragEvent evt) {
        boolean ok = false;
        DataFlavor[] flavors = evt.getCurrentDataFlavors();

        for (DataFlavor flavor : flavors) {
            if (flavor.equals(DataFlavor.javaFileListFlavor) || flavor.isRepresentationClassReader()) {
                ok = true;
                break;
            }
        }

        if (out != null) {
            if (flavors.length == 0) {
                log(out, "FileDrop: no data flavors.");
            }

            for (DataFlavor flavor : flavors) {
                log(out, flavor.toString());
            }
        }

        return ok;
    }

    /**
     * Internal logger print method. If the logger is null, does nothing.
     *
     * @param out logging stream (nullable)
     * @param message message to print
     */
    private static void log(PrintStream out, String message) {
        if (out != null) {
            out.println(message);
        }
    }

    /**
     * Listener for files dropped.
     */
    public interface Listener {
        /**
         * Called when files are dropped into the component.
         *
         * @param files the list of Files dropped
         */
        void filesDropped(File[] files);
    }
}
