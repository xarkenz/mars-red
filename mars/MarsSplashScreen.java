package mars;

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
public class MarsSplashScreen extends JWindow {
    /**
     * The duration to display the splash screen when {@link #showSplash()}
     * is called, in milliseconds.
     */
    private final int duration;

    /**
     * Construct a splash screen for MARS which will display for the given duration when shown.
     *
     * @param duration The duration to display the splash screen when {@link #showSplash()}
     *                 is called, in milliseconds.
     */
    public MarsSplashScreen(int duration) {
        this.duration = duration;
    }

    /**
     * A simple little method to show a title screen in the center
     * of the screen for the amount of time given in the constructor
     */
    public void showSplash() {
        ImageBackgroundPanel content = new ImageBackgroundPanel();
        this.setContentPane(content);

        // Set the window's bounds, centering the window
        // Wee bit of a hack.  I've hardcoded the image dimensions of
        // MarsSurfacePathfinder.jpg, because obtaining them via
        // getHeight() and getWidth() is not trivial -- it is possible
        // that at the time of the call the image has not completed
        // loading so the Image object doesn't know how big it is.
        // So observers are involved -- see the API.
        int width = 390;
        int height = 215;
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screen = tk.getScreenSize();
        int x = (screen.width - width) / 2;
        int y = (screen.height - height) / 2;
        setBounds(x, y, width, height);

        // Build the splash screen
        JLabel title = new JLabel("MARS: Mips Assembler and Runtime Simulator", JLabel.CENTER);
        JLabel copyright1 = new JLabel("<html><br><br>Version " + Globals.APPLICATION_VERSION + " Copyright (c) " + Globals.COPYRIGHT_YEARS + "</html>", JLabel.CENTER);
        JLabel copyright2 = new JLabel("<html><br><br>" + Globals.COPYRIGHT_HOLDERS + "</html>", JLabel.CENTER);
        title.setFont(new Font("Sans-Serif", Font.BOLD, 16));
        title.setForeground(Color.black);
        copyright1.setFont(new Font("Sans-Serif", Font.BOLD, 14));
        copyright2.setFont(new Font("Sans-Serif", Font.BOLD, 14));
        copyright1.setForeground(Color.white);
        copyright2.setForeground(Color.white);

        content.add(title, BorderLayout.NORTH);
        content.add(copyright1, BorderLayout.CENTER);
        content.add(copyright2, BorderLayout.SOUTH);

        // Display it
        setVisible(true);
        // Wait a little while, maybe while loading resources
        try {
            Thread.sleep(duration);
        }
        catch (Exception ignored) {
        }
        setVisible(false);
    }

    private static class ImageBackgroundPanel extends JPanel {
        static final String IMAGE_FILENAME = "MarsSurfacePathfinder.jpg";

        Image image;

        ImageBackgroundPanel() {
            try {
                image = new ImageIcon(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(Globals.IMAGES_PATH + IMAGE_FILENAME))).getImage();
            }
            catch (Exception e) {
                System.out.println(e); // handled in paintComponent()
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                g.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), this);
            }
        }
    }
}
