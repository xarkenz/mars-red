package mars.venus;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseEvent;

public class DynamicTabbedPane extends JTabbedPane {
    /**
     * Creates an empty <code>DynamicTabbedPane</code> with a default tab placement of {@link #TOP}
     * and a default tab layout policy of {@link #SCROLL_TAB_LAYOUT}.
     */
    public DynamicTabbedPane() {
        this(TOP);
    }

    /**
     * Creates an empty <code>DynamicTabbedPane</code> with the specified tab placement
     * and a default tab layout policy of {@link #SCROLL_TAB_LAYOUT}.
     *
     * @param tabPlacement The placement for the tabs relative to the content.
     *                     Must be one of {@link #TOP}, {@link #BOTTOM}, {@link #LEFT}, or {@link #RIGHT}.
     */
    public DynamicTabbedPane(int tabPlacement) {
        this(tabPlacement, SCROLL_TAB_LAYOUT);
    }

    /**
     * Creates an empty <code>DynamicTabbedPane</code> with the specified tab placement and tab layout policy.
     *
     * @param tabPlacement The placement for the tabs relative to the content.
     *                     Must be one of {@link #TOP}, {@link #BOTTOM}, {@link #LEFT}, or {@link #RIGHT}.
     * @param tabLayoutPolicy The policy for laying out tabs when all tabs will not fit on one run.
     *                        Must be either {@link #WRAP_TAB_LAYOUT} or {@link #SCROLL_TAB_LAYOUT}.
     * @throws IllegalArgumentException Thrown if tab placement or tab layout policy are not
     *         one of the above supported values.
     */
    public DynamicTabbedPane(int tabPlacement, int tabLayoutPolicy) {
        super(tabPlacement, tabLayoutPolicy);
        MouseMovementHandler handler = new MouseMovementHandler();
        this.addMouseListener(handler);
        this.addMouseMotionListener(handler);
    }

    /**
     * Inserts a new tab for the given component, at the given index,
     * represented by the given title and/or icon, either of which may
     * be <code>null</code>.
     *
     * @param title The title to be displayed on the tab.
     * @param icon The icon to be displayed on the tab.
     * @param component The component to be displayed when this tab is clicked.
     * @param tip The tooltip to be displayed for this tab.
     * @param index The position to insert this new tab
     *              (0 &le; <code>index</code> &le; {@link #getTabCount()}).
     *
     * @throws IndexOutOfBoundsException Thrown if the index is out of range
     *                                   (<code>index</code> &lt; 0 or <code>index</code> &gt; {@link #getTabCount()}).
     */
    @Override
    public void insertTab(String title, Icon icon, Component component, String tip, int index) {
        super.insertTab(title, icon, component, tip, index);
        this.setTabComponentAt(index, new DynamicTabComponent(title));
    }

    /**
     * The tab component instantiated for each tab which adds a close button.
     */
    private class DynamicTabComponent extends Box {
        public DynamicTabComponent(String title) {
            super(BoxLayout.X_AXIS);

            JLabel titleLabel = new JLabel(title) {
                @Override
                public String getText() {
                    // Grab the title from the tabbed pane and adjust the text if it has changed
                    int index = DynamicTabComponent.this.getIndex();
                    if (index >= 0) {
                        String newTitle = DynamicTabbedPane.this.getTitleAt(index);
                        if (!newTitle.equals(super.getText())) {
                            super.setText(newTitle);
                        }
                    }
                    return super.getText();
                }
            };
            JButton closeButton = new JButton("✕");
            closeButton.addActionListener(event -> {
                // Close the corresponding tab
                int index = this.getIndex();
                if (index >= 0) {
                    DynamicTabbedPane.this.removeTabAt(index);
                }
            });
            closeButton.putClientProperty("JButton.buttonType", "borderless");
            closeButton.putClientProperty("JButton.squareSize", true);

            this.add(titleLabel);
            this.add(Box.createHorizontalStrut(10));
            this.add(closeButton);
        }

        public int getIndex() {
            return DynamicTabbedPane.this.indexOfTabComponent(this);
        }
    }

    private class MouseMovementHandler implements MouseInputListener {
        private int mouseXOffset;

        /**
         * Invoked when the mouse button has been clicked (pressed
         * and released) on a component.
         *
         * @param e the event to be processed
         */
        @Override
        public void mouseClicked(MouseEvent e) {

        }

        /**
         * Invoked when a mouse button has been pressed on a component.
         *
         * @param e the event to be processed
         */
        @Override
        public void mousePressed(MouseEvent e) {

        }

        /**
         * Invoked when a mouse button has been released on a component.
         *
         * @param e the event to be processed
         */
        @Override
        public void mouseReleased(MouseEvent e) {

        }

        /**
         * Invoked when the mouse enters a component.
         *
         * @param e the event to be processed
         */
        @Override
        public void mouseEntered(MouseEvent e) {

        }

        /**
         * Invoked when the mouse exits a component.
         *
         * @param e the event to be processed
         */
        @Override
        public void mouseExited(MouseEvent e) {

        }

        /**
         * Invoked when a mouse button is pressed on a component and then
         * dragged.  {@code MOUSE_DRAGGED} events will continue to be
         * delivered to the component where the drag originated until the
         * mouse button is released (regardless of whether the mouse position
         * is within the bounds of the component).
         * <p>
         * Due to platform-dependent Drag&amp;Drop implementations,
         * {@code MOUSE_DRAGGED} events may not be delivered during a native
         * Drag&amp;Drop operation.
         *
         * @param e the event to be processed
         */
        @Override
        public void mouseDragged(MouseEvent e) {

        }

        /**
         * Invoked when the mouse cursor has been moved onto a component
         * but no buttons have been pushed.
         *
         * @param e the event to be processed
         */
        @Override
        public void mouseMoved(MouseEvent e) {

        }
    }
}
