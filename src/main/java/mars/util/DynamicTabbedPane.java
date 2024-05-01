package mars.util;

import javax.swing.*;
import java.awt.*;

public class DynamicTabbedPane extends JTabbedPane {
    /**
     * Creates an empty <code>DynamicTabbedPane</code> with a default tab placement of {@link #TOP}
     * and a default tab layout policy of {@link #SCROLL_TAB_LAYOUT}.
     */
    public DynamicTabbedPane() {
        this(TOP, SCROLL_TAB_LAYOUT);
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
}
