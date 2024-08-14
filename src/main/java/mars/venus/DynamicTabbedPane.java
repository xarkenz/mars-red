package mars.venus;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseEvent;

public class DynamicTabbedPane extends JTabbedPane {
    private final TabDragHandler tabDragHandler;

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
        this.tabDragHandler = new TabDragHandler(this);
        this.addMouseListener(this.tabDragHandler);
        this.addMouseMotionListener(this.tabDragHandler);
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

    public boolean hasHorizontalTabPlacement() {
        return this.getTabPlacement() == JTabbedPane.TOP || this.getTabPlacement() == JTabbedPane.BOTTOM;
    }

    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);
        if (this.tabDragHandler.getCurrentIndex() >= 0) {
            Rectangle tabBounds = this.getBoundsAt(this.tabDragHandler.getCurrentIndex());
            int newCenter = this.tabDragHandler.getMousePosition() + this.tabDragHandler.getTabCenterOffset();
            if (this.hasHorizontalTabPlacement()) {
                tabBounds.x += newCenter - (int) tabBounds.getCenterX();
            }
            else {
                tabBounds.y += newCenter - (int) tabBounds.getCenterY();
            }

            graphics.setColor(Color.RED);
            graphics.drawRect(tabBounds.x, tabBounds.y, tabBounds.width, tabBounds.height);
        }
    }

    /**
     * The tab component instantiated for each tab which adds a close button.
     */
    private class DynamicTabComponent extends Box {
        public DynamicTabComponent(String title) {
            super(BoxLayout.X_AXIS);

            // This is definitely a hack. Essentially, the title will be a custom JLabel whose content will
            // secretly update if needed whenever its text is accessed through getText().
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
            // This Unicode character actually looks very clean as a close button "icon"
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

    private static class TabDragHandler implements MouseInputListener {
        private final DynamicTabbedPane tabbedPane;
        private int currentIndex;
        /**
         * The position of the center of the tab currently being dragged relative to the cursor position.
         */
        private int tabCenterOffset;
        private int mousePosition;

        public TabDragHandler(DynamicTabbedPane tabbedPane) {
            this.tabbedPane = tabbedPane;
            this.currentIndex = -1;
        }

        public int getCurrentIndex() {
            return this.currentIndex;
        }

        public int getTabCenterOffset() {
            return this.tabCenterOffset;
        }

        public int getMousePosition() {
            return this.mousePosition;
        }

        /**
         * Invoked when the mouse button has been clicked (pressed and released) on a component.
         *
         * @param event The event to be processed.
         */
        @Override
        public void mouseClicked(MouseEvent event) {

        }

        /**
         * Invoked when a mouse button has been pressed on a component.
         *
         * @param event The event to be processed.
         */
        @Override
        public void mousePressed(MouseEvent event) {
            this.currentIndex = this.tabbedPane.indexAtLocation(event.getX(), event.getY());
            if (this.currentIndex < 0 || this.currentIndex >= this.tabbedPane.getTabCount()) {
                this.currentIndex = -1;
                return;
            }

            Rectangle tabBounds = this.tabbedPane.getBoundsAt(this.currentIndex);
            if (this.tabbedPane.hasHorizontalTabPlacement()) {
                this.mousePosition = event.getX();
                this.tabCenterOffset = (int) tabBounds.getCenterX() - this.mousePosition;
            }
            else {
                this.mousePosition = event.getY();
                this.tabCenterOffset = (int) tabBounds.getCenterY() - this.mousePosition;
            }

            this.tabbedPane.repaint();
        }

        /**
         * Invoked when a mouse button has been released on a component.
         *
         * @param event The event to be processed.
         */
        @Override
        public void mouseReleased(MouseEvent event) {
            this.currentIndex = -1;

            this.tabbedPane.repaint();
        }

        /**
         * Invoked when a mouse button is pressed on a component and then dragged. {@link MouseEvent#MOUSE_DRAGGED}
         * events will continue to be delivered to the component where the drag originated until the mouse button
         * is released (regardless of whether the mouse position is within the bounds of the component).
         * <p>
         * Due to platform-dependent Drag&amp;Drop implementations, <code>MOUSE_DRAGGED</code> events may not be
         * delivered during a native Drag&amp;Drop operation.
         *
         * @param event The event to be processed.
         */
        @Override
        public void mouseDragged(MouseEvent event) {
            if (this.currentIndex < 0 || this.currentIndex >= this.tabbedPane.getTabCount()) {
                this.currentIndex = -1;
                return;
            }

            int oldCenter;
            if (this.tabbedPane.hasHorizontalTabPlacement()) {
                this.mousePosition = event.getX();
                oldCenter = (int) this.tabbedPane.getBoundsAt(this.currentIndex).getCenterX();
            }
            else {
                this.mousePosition = event.getY();
                oldCenter = (int) this.tabbedPane.getBoundsAt(this.currentIndex).getCenterY();
            }
            int newCenter = this.mousePosition + this.tabCenterOffset;

            int direction = Integer.compare(newCenter, oldCenter);
            if (direction != 0) {
                int index = this.currentIndex;
                while (index + direction >= 0 && index + direction < this.tabbedPane.getTabCount()) {
                    int testCenter;
                    if (this.tabbedPane.hasHorizontalTabPlacement()) {
                        testCenter = (int) this.tabbedPane.getBoundsAt(index + direction).getCenterX();
                    }
                    else {
                        testCenter = (int) this.tabbedPane.getBoundsAt(index + direction).getCenterY();
                    }
                    if (newCenter * direction >= testCenter * direction) {
                        index += direction;
                    }
                    else {
                        break;
                    }
                }

                if (index != this.currentIndex) {
                    // The tab needs to be actually shifted to another spot
                    String title = this.tabbedPane.getTitleAt(this.currentIndex);
                    Icon icon = this.tabbedPane.getIconAt(this.currentIndex);
                    Component component = this.tabbedPane.getComponentAt(this.currentIndex);
                    String tip = this.tabbedPane.getToolTipTextAt(this.currentIndex);
                    this.tabbedPane.removeTabAt(this.currentIndex);
                    this.tabbedPane.insertTab(title, icon, component, tip, index);
                    this.tabbedPane.setSelectedIndex(index);
                    this.currentIndex = index;
                }

                this.tabbedPane.repaint();
            }
        }

        /**
         * Invoked when the mouse cursor has been moved onto a component but no buttons have been pushed.
         *
         * @param event The event to be processed.
         */
        @Override
        public void mouseMoved(MouseEvent event) {

        }

        /**
         * Invoked when the mouse enters a component.
         *
         * @param event The event to be processed.
         */
        @Override
        public void mouseEntered(MouseEvent event) {

        }

        /**
         * Invoked when the mouse exits a component.
         *
         * @param event The event to be processed.
         */
        @Override
        public void mouseExited(MouseEvent event) {

        }
    }
}
