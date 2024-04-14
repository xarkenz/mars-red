package mars.venus;

import mars.Application;

import javax.swing.*;
import java.awt.*;

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
 * Produces MARS splash screen. Adapted from
 * <a href="https://web.archive.org/web/20060209023646/http://www.java-tips.org/content/view/1267/2/">
 * an old Java Tips page
 * </a>.
 */
public class SplashScreen extends JWindow {
    private static final int WINDOW_WIDTH = 500;
    private static final int WINDOW_HEIGHT = 300;
    private static final String IMAGE_FILENAME = "MarsAresVallis.jpg";
    private static final float IMAGE_OPACITY = 0.5f;

    private final VenusUI gui;
    /**
     * The duration to display the splash screen when {@link #showSplash()}
     * is called, in milliseconds.
     */
    private final int duration;

    /**
     * Construct the splash screen for MARS which will display for the given duration when shown.
     *
     * @param duration The duration to display the splash screen when {@link #showSplash()}
     *                 is called, in milliseconds.
     */
    public SplashScreen(VenusUI gui, int duration) {
        this.gui = gui;
        this.duration = duration;

        // Build the splash screen
        ImageBackgroundPanel contentPane = new ImageBackgroundPanel();
        this.setContentPane(contentPane);

        JLabel content = new JLabel("<html><center>"
            + "<h1>" + Application.NAME + "</h1>"
            + "<h2>MIPS Assembler and Runtime Simulator</h2>"
            + "<h3>Version " + Application.VERSION + "</h3>"
            + "<p><b>Copyright © " + Application.COPYRIGHT_YEARS + " " + Application.COPYRIGHT_HOLDERS + "</b></p>"
            + "<p><b>Modified by Sean Clarke</b></p>"
            + "</center></html>", JLabel.CENTER);
        contentPane.add(content, BorderLayout.CENTER);
    }

    /**
     * Show the splash screen for the amount of time given in the constructor,
     * then set the main GUI window to be visible.
     */
    public void showSplash() {
        // Set up window bounds
        Dimension screen = this.getToolkit().getScreenSize();
        int x = (screen.width - WINDOW_WIDTH) / 2;
        int y = (screen.height - WINDOW_HEIGHT) / 2;
        this.setBounds(x, y, WINDOW_WIDTH, WINDOW_HEIGHT);

        // Display the splash screen
        this.setVisible(true);

        // Set a timer to show the main GUI and hide the splash screen after the specified duration
        Timer splashTimer = new Timer(this.duration, event -> {
            this.gui.showWindow();
            this.setVisible(false);
            this.dispose();
        });
        splashTimer.setRepeats(false);
        splashTimer.start();
    }

    private static class ImageBackgroundPanel extends JPanel {
        private Image image;

        public ImageBackgroundPanel() {
            super(new BorderLayout());
            this.setOpaque(true);
            this.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.focusColor"), 2));

            try {
                this.image = this.getToolkit().getImage(this.getClass().getResource(Application.IMAGES_PATH + IMAGE_FILENAME));
            }
            catch (Exception exception) {
                System.err.println("Unable to load splash screen background (" + Application.IMAGES_PATH + IMAGE_FILENAME + "):\n" + exception);
                this.image = null;
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            // Don't paint if the image is not loaded
            if (this.image == null) {
                return;
            }
            // Get width and height of the source image
            int width = this.image.getWidth(this);
            int height = this.image.getHeight(this);
            if (width < 0 || height < 0) {
                return;
            }

            Graphics2D graphics2D = (Graphics2D) graphics;
            // Save the original composite so it can be restored later
            Composite composite = graphics2D.getComposite();
            // Set the opacity of the graphics context to blend the image with the default background color
            graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, IMAGE_OPACITY));

            // Calculate the area of the source image which should be shown on the window
            float aspectNeeded = (float) WINDOW_WIDTH / WINDOW_HEIGHT;
            int cropWidth = Math.min(width, Math.round(height * aspectNeeded));
            int cropHeight = Math.min(height, Math.round(width / aspectNeeded));
            int cropX = (width - cropWidth) / 2;
            int cropY = (height - cropHeight) / 2;
            // Draw the calculated area of the image to the window
            graphics2D.drawImage(this.image, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, cropX, cropY, cropX + cropWidth, cropY + cropHeight, this);

            // Restore the original composite of the graphics context so other components are drawn properly
            graphics2D.setComposite(composite);
        }
    }
}
