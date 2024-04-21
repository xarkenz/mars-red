package mars;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TooManyListenersException;

/**
 * File drop handler that registers drop listeners onto a component and changes the component's
 * borders when dragging files over it.
 * <p>
 * This component comes from <a href="http://www.calebhoff.com/2018/10/mars-plus.html">MARS Plus</a>,
 * a derivation of the original MARS 4.5 created by Caleb Hoff, but the code looks similar to
 * <a href="https://github.com/tferr/Scripts/blob/dc0801d6288fb63d5ddabc7c552b86b5d74459d0/BAR/src/main/java/bar/FileDrop.java">Robert Harder's code</a>
 * which is now in the public domain.
 *
 * @author Robert Harder
 */
public class FileDrop implements DropTargetListener {
    private static final Color DEFAULT_DRAG_BORDER_COLOR = new Color(0.0F, 0.0F, 1.0F, 0.25F);

    private static Boolean supportsDnD = null;
    private static Border defaultDragBorder = null;

    private PrintStream logger;
    private Component component;
    private Border normalBorder;
    private Border dragBorder;
    private boolean isRecursive;
    private Listener listener;

    /**
     * Listener for files dropped.
     */
    public interface Listener {
        /**
         * Called when files are dropped into the component by the user.
         *
         * @param files The list of files dropped.
         */
        void filesDropped(List<File> files);
    }

    public FileDrop(Component component, Listener listener) {
        this(null, component, null, false, listener);
    }

    public FileDrop(Component component, boolean recursive, Listener listener) {
        this(null, component, null, recursive, listener);
    }

    public FileDrop(PrintStream logger, Component component, Listener listener) {
        this(logger, component, null, false, listener);
    }

    public FileDrop(PrintStream logger, Component component, boolean recursive, Listener listener) {
        this(logger, component, null, recursive, listener);
    }

    public FileDrop(Component component, Border dragBorder, Listener listener) {
        this(null, component, dragBorder, false, listener);
    }

    public FileDrop(Component component, Border dragBorder, boolean recursive, Listener listener) {
        this(null, component, dragBorder, recursive, listener);
    }

    public FileDrop(PrintStream logger, Component component, Border dragBorder, Listener listener) {
        this(logger, component, dragBorder, false, listener);
    }

    /**
     * Create a new file drop handler for a particular Swing Component.
     *
     * @param logger     Logging stream (nullable).
     * @param component  Component to register the file drop listener to.
     * @param dragBorder Border to change the component's border to on drag over.
     * @param recursive  Whether to register the file drop listener to the component's children.
     * @param listener   File drop listener that will be invoked when a file is dropped.
     */
    public FileDrop(PrintStream logger, Component component, Border dragBorder, boolean recursive, Listener listener) {
        if (!supportsDnD()) {
            this.log("drag and drop is not supported");
            return;
        }

        this.logger = logger;
        this.component = component;
        this.dragBorder = dragBorder != null ? dragBorder : getDefaultDragBorder();
        this.normalBorder = null;
        this.isRecursive = recursive;
        this.listener = listener;

        this.makeDropTarget(component);
    }

    /**
     * Called while a drag operation is ongoing, when the mouse pointer enters
     * the operable part of the drop site for the {@link DropTarget}
     * registered with this listener.
     *
     * @param event The event.
     */
    @Override
    public void dragEnter(DropTargetDragEvent event) {
        this.log("dragEnter event");

        if (this.supportsDragFlavor(event)) {
            if (this.component instanceof JComponent jComponent) {
                this.normalBorder = jComponent.getBorder();
                this.log("normal border saved");
                jComponent.setBorder(this.dragBorder);
                this.log("drag border set");
            }

            event.acceptDrag(1);
            this.log("event accepted");
        }
        else {
            event.rejectDrag();
            this.log("event rejected");
        }
    }

    /**
     * Called when a drag operation is ongoing, while the mouse pointer is still
     * over the operable part of the drop site for the {@link DropTarget}
     * registered with this listener.
     *
     * @param event The event.
     */
    @Override
    public void dragOver(DropTargetDragEvent event) {
        // Do nothing
    }

    /**
     * Called when the drag operation has terminated with a drop on
     * the operable part of the drop site for the {@link DropTarget}
     * registered with this listener.
     *
     * @param event The event.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void drop(DropTargetDropEvent event) {
        this.log("drop event");

        try {
            Transferable transferable = event.getTransferable();
            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                event.acceptDrop(DnDConstants.ACTION_COPY);
                this.log("file list accepted");
                List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                if (this.listener != null) {
                    this.listener.filesDropped(files);
                }

                event.getDropTargetContext().dropComplete(true);
                this.log("drop complete");
            }
            else {
                DataFlavor[] flavors = transferable.getTransferDataFlavors();
                boolean handled = false;

                for (DataFlavor flavor : flavors) {
                    if (flavor.isRepresentationClassReader()) {
                        event.acceptDrop(DnDConstants.ACTION_COPY);
                        this.log("reader accepted");
                        BufferedReader reader = new BufferedReader(flavor.getReaderForText(transferable));
                        if (this.listener != null) {
                            this.listener.filesDropped(this.createFileList(reader));
                        }

                        event.getDropTargetContext().dropComplete(true);
                        this.log("drop complete");
                        handled = true;
                        break;
                    }
                }

                if (!handled) {
                    this.log("not a file list or reader - abort");
                    event.rejectDrop();
                }
            }
        }
        catch (IOException | UnsupportedFlavorException exception) {
            this.log("encountered an error while handling drop event - abort");
            if (this.logger != null) {
                exception.printStackTrace(this.logger);
            }
            event.rejectDrop();
        }
        finally {
            if (this.component instanceof JComponent jComponent) {
                jComponent.setBorder(this.normalBorder);
                this.log("normal border restored");
            }
        }
    }

    /**
     * Called while a drag operation is ongoing, when the mouse pointer has
     * exited the operable part of the drop site for the
     * {@link DropTarget} registered with this listener.
     *
     * @param event The event.
     */
    @Override
    public void dragExit(DropTargetEvent event) {
        this.log("dragExit event");

        if (this.component instanceof JComponent jComponent) {
            jComponent.setBorder(this.normalBorder);
            this.log("normal border restored");
        }
    }

    /**
     * Called if the user has modified the current drop gesture.
     *
     * @param event The event.
     */
    @Override
    public void dropActionChanged(DropTargetDragEvent event) {
        this.log("dropActionChanged event");

        if (this.supportsDragFlavor(event)) {
            event.acceptDrag(DnDConstants.ACTION_COPY);
            this.log("event accepted");
        }
        else {
            event.rejectDrag();
            this.log("event rejected");
        }
    }

    /**
     * Determine whether the JVM/AWT package supports drag and grop functionality.
     *
     * @return true if DnD is supported, false otherwise.
     */
    private static boolean supportsDnD() {
        if (FileDrop.supportsDnD == null) {
            // We only need to check the support once
            try {
                // Attempt to load a class related to drag and drop functionality
                // If that fails, the catch clause will be executed instead
                Class.forName("java.awt.dnd.DnDConstants");
                FileDrop.supportsDnD = Boolean.TRUE;
            }
            catch (Exception exception) {
                FileDrop.supportsDnD = Boolean.FALSE;
            }
        }

        return FileDrop.supportsDnD;
    }

    /**
     * Get the border to replace the target component's border with when files are dragged over it,
     * unless a custom border is provided in the constructor.
     *
     * @return The default dragging border.
     */
    private static Border getDefaultDragBorder() {
        if (FileDrop.defaultDragBorder == null) {
            Color color = UIManager.getColor("FileDrop.borderColor");
            if (color == null) {
                color = DEFAULT_DRAG_BORDER_COLOR;
            }
            FileDrop.defaultDragBorder = BorderFactory.createMatteBorder(2, 2, 2, 2, color);
        }

        return FileDrop.defaultDragBorder;
    }

    /**
     * Create a file list from lines from a reader. Used if the filesystem provides names of files
     * when dropping instead of actual {@link File} objects.
     *
     * @param reader Buffered reader provided by the filesystem.
     * @return The created file list.
     */
    private List<File> createFileList(BufferedReader reader) {
        List<File> files = new ArrayList<>();

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("\0")) {
                    continue;
                }

                try {
                    File file = new File(new URI(line));
                    files.add(file);
                }
                catch (Exception exception) {
                    this.log("error with '" + line + "': " + exception.getMessage());
                }
            }

            return files;
        }
        catch (IOException exception) {
            this.log("encountered an error while creating file array:");
            if (this.logger != null) {
                exception.printStackTrace(this.logger);
            }
            return files;
        }
    }

    /**
     * Register the file drop handler's provided listener with the provided component.
     *
     * @param component Component to attach the listener to.
     */
    private void makeDropTarget(Component component) {
        DropTarget dropTarget = new DropTarget();

        try {
            dropTarget.addDropTargetListener(this);
        }
        catch (TooManyListenersException exception) {
            this.log("too many listeners attached, drop will fail:");
            if (this.logger != null) {
                exception.printStackTrace(this.logger);
            }
        }

        component.addHierarchyListener(event -> {
            this.log("hierarchy changed");
            Component parent = component.getParent();
            if (parent == null) {
                component.setDropTarget(null);
                this.log("drop target cleared from component");
            }
            else {
                new DropTarget(component, this);
                this.log("drop target added to component");
            }
        });
        if (component.getParent() != null) {
            new DropTarget(component, this);
        }

        if (this.isRecursive && component instanceof Container container) {
            for (Component child : container.getComponents()) {
                this.makeDropTarget(child);
            }
        }
    }

    /**
     * Determine whether the file system returns supported formats for dropping files.
     *
     * @param event Event called on dragging an object over the component.
     * @return true if the dragging format is supported, false otherwise.
     */
    private boolean supportsDragFlavor(DropTargetDragEvent event) {
        DataFlavor[] flavors = event.getCurrentDataFlavors();

        if (this.logger != null) {
            if (flavors.length == 0) {
                this.log("no data flavors");
            }

            for (DataFlavor flavor : flavors) {
                this.log(flavor.toString());
            }
        }

        return Arrays.stream(flavors)
            .anyMatch(flavor -> flavor.equals(DataFlavor.javaFileListFlavor) || flavor.isRepresentationClassReader());
    }

    /**
     * Internal logger print method. If the logger is null, does nothing.
     *
     * @param message Message to print.
     */
    private void log(String message) {
        if (this.logger != null) {
            this.logger.println(FileDrop.class.getSimpleName() + ": " + message);
        }
    }
}
