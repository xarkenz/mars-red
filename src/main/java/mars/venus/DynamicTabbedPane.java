package mars.venus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * An extension of {@link JTabbedPane} which makes tabs closable and able to be rearranged by the user.
 */
public class DynamicTabbedPane extends JTabbedPane {
    private final TabDragHandler tabDragHandler;
    private Color tabDragBackground;

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
        this.updateTabDragBackground();
    }

    /**
     * Insert a new tab for the given component, at the given index, represented by the given title and/or icon,
     * either of which may be <code>null</code>.
     *
     * @param title The title to be displayed on the tab.
     * @param icon The icon to be displayed on the tab.
     * @param component The component to be displayed when this tab is clicked.
     * @param tip The tooltip to be displayed for this tab.
     * @param index The position to insert this new tab (0 &le; <code>index</code> &le; {@link #getTabCount()}).
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
     * Determine which axis is relevant for tab positioning based on tab placement. For horizontal placement
     * (i.e. top or bottom) the X axis is relevant, and for vertical placement (i.e. left or right)
     * the Y axis is relevant.
     *
     * @return <code>true</code> if {@link #getTabPlacement()} returns {@link #TOP} or {@link #BOTTOM}, or
     *         <code>false</code> if it returns {@link #LEFT} or {@link #RIGHT}.
     */
    public boolean hasHorizontalTabPlacement() {
        return this.getTabPlacement() == JTabbedPane.TOP || this.getTabPlacement() == JTabbedPane.BOTTOM;
    }

    /**
     * Paint the tabbed pane, then edit the result if needed to provide visual feedback for tab reordering.
     *
     * @param graphics The graphics context in which to paint.
     */
    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);
        if (this.tabDragHandler.getCurrentIndex() >= 0) {
            Rectangle tabBounds = this.getBoundsAt(this.tabDragHandler.getCurrentIndex());

            Rectangle bgClip = new Rectangle(tabBounds);
            if (this.hasHorizontalTabPlacement()) {
                int tabOffset = this.tabDragHandler.getDraggedTabPosition() - tabBounds.x;
                graphics.copyArea(tabBounds.x, tabBounds.y, tabBounds.width, tabBounds.height, tabOffset, 0);

                bgClip.width = Math.min(Math.abs(tabOffset), tabBounds.width);
                if (tabOffset < 0 && bgClip.width < tabBounds.width) {
                    bgClip.x += tabBounds.width + tabOffset;
                }
            }
            else {
                int tabOffset = this.tabDragHandler.getDraggedTabPosition() - tabBounds.y;
                graphics.copyArea(tabBounds.x, tabBounds.y, tabBounds.width, tabBounds.height, 0, tabOffset);

                bgClip.height = Math.min(Math.abs(tabOffset), tabBounds.height);
                if (tabOffset < 0 && bgClip.height < tabBounds.height) {
                    bgClip.y += tabBounds.height + tabOffset;
                }
            }

            if (bgClip.width > 0 && bgClip.height > 0) {
                Graphics bgGraphics = graphics.create(bgClip.x, bgClip.y, bgClip.width, bgClip.height);

                bgGraphics.setColor(this.tabDragBackground);
                bgGraphics.fillRect(0, 0, tabBounds.width, tabBounds.height);
            }
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();
        this.updateTabDragBackground();
    }

    private void updateTabDragBackground() {
        this.tabDragBackground = UIManager.getColor("TabbedPane.focusColor");
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

    private static class TabDragHandler extends MouseAdapter {
        /**
         * The tabbed pane this handler is attached to.
         */
        private final DynamicTabbedPane tabbedPane;
        /**
         * The index of the tab currently being dragged by the user, or -1 if no tabs are being dragged.
         */
        private int currentIndex;
        /**
         * The position of the left/top edge of the tab currently being dragged relative to the mouse position.
         */
        private int tabEdgeOffset;
        /**
         * The last recorded mouse position (only updated while the user is holding the mouse down).
         */
        private int mousePosition;

        /**
         * Create a new <code>TabDragHandler</code> for the given tabbed pane. Does not automatically attach
         * as a mouse listener.
         *
         * @param tabbedPane The tabbed pane this handler is attached to.
         */
        public TabDragHandler(DynamicTabbedPane tabbedPane) {
            this.tabbedPane = tabbedPane;
            this.currentIndex = -1;
        }

        /**
         * Get the index of the tab currently being dragged by the user.
         *
         * @return The tab index, or -1 if no tabs are currently being dragged.
         */
        public int getCurrentIndex() {
            return this.currentIndex;
        }

        /**
         * Get either the X coordinate of the left edge or the Y coordinate of the top edge for the visual
         * positioning of the tab currently being dragged. For horizontal tab placements (top or bottom),
         * the X coordinate is used. For vertical tab placements (left or right), the Y coordinate is used.
         * This method should only be called if {@link #getCurrentIndex()} returns a value &ge; 0.
         *
         * @return The integer position of the current tab for painting purposes.
         */
        public int getDraggedTabPosition() {
            return this.mousePosition + this.tabEdgeOffset;
        }

        /**
         * Invoked when the user starts dragging on the tabbed pane component.
         *
         * @param event The event to be processed.
         */
        @Override
        public void mousePressed(MouseEvent event) {
            // Only proceed if the user actually pressed on a tab
            this.currentIndex = this.tabbedPane.indexAtLocation(event.getX(), event.getY());
            if (this.currentIndex < 0 || this.currentIndex >= this.tabbedPane.getTabCount()) {
                this.currentIndex = -1;
                return;
            }

            if (this.tabbedPane.hasHorizontalTabPlacement()) {
                this.mousePosition = event.getX();
                this.tabEdgeOffset = this.tabbedPane.getBoundsAt(this.currentIndex).x - this.mousePosition;
            }
            else {
                this.mousePosition = event.getY();
                this.tabEdgeOffset = this.tabbedPane.getBoundsAt(this.currentIndex).y - this.mousePosition;
            }

            this.tabbedPane.repaint();
        }

        /**
         * Invoked when the user stops dragging on the tabbed pane component.
         *
         * @param event The event to be processed.
         */
        @Override
        public void mouseReleased(MouseEvent event) {
            // Indicate dragging has stopped
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
            // Sanity check
            if (this.currentIndex < 0 || this.currentIndex >= this.tabbedPane.getTabCount()) {
                this.currentIndex = -1;
                return;
            }

            int oldTabEdge;
            if (this.tabbedPane.hasHorizontalTabPlacement()) {
                this.mousePosition = event.getX();
                oldTabEdge = this.tabbedPane.getBoundsAt(this.currentIndex).x;
            }
            else {
                this.mousePosition = event.getY();
                oldTabEdge = this.tabbedPane.getBoundsAt(this.currentIndex).y;
            }
            int newTabEdge = this.getDraggedTabPosition();

            // Integer.compare comes in handy here; essentially, `direction` indicates relative drag direction:
            // 0 if newTabEdge == oldTabEdge (tab has not moved)
            // +1 if newTabEdge > oldTabEdge (mouse is dragging tab right or down depending on tab placement)
            // -1 if newTabEdge < oldTabEdge (mouse is dragging tab left or up depending on tab placement)
            int direction = Integer.compare(newTabEdge, oldTabEdge);

            if (direction == 0) {
                // Repaint just in case
                this.tabbedPane.repaint();
                return;
            }

            // Iteratively shift the current tab past other tabs if it has passed any
            boolean tabHasMoved = false;
            int testIndex = this.currentIndex + direction;
            while (testIndex >= 0 && testIndex < this.tabbedPane.getTabCount()) {
                int testTabSize;
                if (this.tabbedPane.hasHorizontalTabPlacement()) {
                    testTabSize = this.tabbedPane.getBoundsAt(testIndex).width;
                    oldTabEdge = this.tabbedPane.getBoundsAt(this.currentIndex).x;
                }
                else {
                    testTabSize = this.tabbedPane.getBoundsAt(testIndex).height;
                    oldTabEdge = this.tabbedPane.getBoundsAt(this.currentIndex).y;
                }

                // The current tab "passes" the tab being tested if it has been dragged over half the width of the
                // tab being tested; or, in other words, it is closer to the prospective spot than the current spot.
                int tabAbsOffset = direction * (newTabEdge - oldTabEdge);
                if (tabAbsOffset > testTabSize - tabAbsOffset) {
                    // Swap the tab being dragged with the tab it is passing, being careful not to break focus.
                    // The tab being passed has to be removed and reinserted rather than the current tab because
                    // otherwise focus would be broken, causing flicker on some platforms.
                    // I wish there was a better way to do this...
                    String title = this.tabbedPane.getTitleAt(testIndex);
                    Icon icon = this.tabbedPane.getIconAt(testIndex);
                    Component component = this.tabbedPane.getComponentAt(testIndex);
                    String tip = this.tabbedPane.getToolTipTextAt(testIndex);
                    this.tabbedPane.removeTabAt(testIndex);
                    this.tabbedPane.insertTab(title, icon, component, tip, this.currentIndex);

                    this.currentIndex = testIndex;
                    testIndex += direction;
                    tabHasMoved = true;
                }
                else {
                    break;
                }
            }

            // If the tab has actually changed positions, we want to make sure change listeners know
            if (tabHasMoved) {
                this.tabbedPane.fireStateChanged();
            }

            this.tabbedPane.repaint();
        }
    }
}
