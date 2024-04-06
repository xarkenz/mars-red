package mars.util;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * An icon whose image is in the Scalable Vector Graphics format.
 * Since it is a vector image, this icon can be resized, despite the {@link Icon}
 * interface being defined to represent a "small <i>fixed size</i> picture."
 */
public class SVGIcon implements Icon {
    // This is a trick to account for SVG elements seemingly aligning to integer boundaries.
    // When set to 1, elements can appear out of alignment with each other for this reason.
    // The higher the number, the larger the internal image the transcoder generates, which is then
    // sampled with antialiasing when painted.
    private static final int SAMPLES_PER_PIXEL = 2;

    private final SVGToBufferedImageTranscoder transcoder;
    private final TranscoderInput transcoderInput;
    // I considered storing the dimensions solely in the transcoder hints, but their hints are stored
    // as floats, and this is more convenient for the getter methods anyway.
    private int width;
    private int height;
    private boolean needsTranscode;

    /**
     * Create a new {@code SVGIcon} from a URI and initial dimensions.
     *
     * @param url    The URL for the icon image.
     * @param width  The initial width of the icon.
     * @param height The initial height of the icon.
     */
    public SVGIcon(URL url, int width, int height) {
        this.transcoder = new SVGToBufferedImageTranscoder();
        this.transcoderInput = new TranscoderInput(url.toString());
        // This method call will generate the initial image for the icon
        this.setIconDimensions(width, height);
    }

    /**
     * Draw the icon at the specified location.
     *
     * @param component A {@code Component} to get properties useful for painting (not used).
     * @param gfx       The graphics context.
     * @param leftX     The X coordinate of the icon's top-left corner.
     * @param topY      The Y coordinate of the icon's top-left corner.
     */
    @Override
    public void paintIcon(Component component, Graphics gfx, int leftX, int topY) {
        AffineTransform transform = ((Graphics2D) gfx).getTransform();
        // Regenerate the image
        if (this.needsTranscode) {
            // Set the target width and height for the transcoder, converting from user space to device space
            // so the image still appears sharp on high-DPI screens
            this.transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, (float) (this.width * transform.getScaleX() * SAMPLES_PER_PIXEL));
            this.transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, (float) (this.height * transform.getScaleY() * SAMPLES_PER_PIXEL));
            // Perform the transcode operation
            try {
                this.transcoder.transcode(transcoderInput);
                this.needsTranscode = false;
            }
            catch (TranscoderException exception) {
                // TODO: Probably should handle this case differently
                // For now, the needsTranscode flag will remain true in case the transcode works next time
                exception.printStackTrace();
            }
        }
        // Simply setting the transform to the identity matrix seems to work for preventing scaling here
        // (The scaling is instead done by changing the target dimensions for the transcoder above)
        transform.setToIdentity();
        gfx.drawImage(this.transcoder.getOutput(), leftX, topY, this.width, this.height, null);
    }

    /**
     * Get the icon's width.
     *
     * @return An int specifying the width of the icon.
     */
    @Override
    public int getIconWidth() {
        return this.width;
    }

    /**
     * Get the icon's height.
     *
     * @return An int specifying the height of the icon.
     */
    @Override
    public int getIconHeight() {
        return this.height;
    }

    /**
     * Set the icon's image dimensions. Note that this reloads the image.
     *
     * @param width  The new width of the icon.
     * @param height The new height of the icon.
     */
    public void setIconDimensions(int width, int height) {
        this.width = width;
        this.height = height;
        // Since the width and height may have changed, regenerate the image upon next paint
        this.needsTranscode = true;
    }

    /**
     * A concrete implementation of the Apache Batik {@link ImageTranscoder} which transcodes
     * an SVG image to the raw {@link BufferedImage} instead of a file format. Since {@code BufferedImage}
     * is used internally anyway, this effectively just cuts out the step involving {@link TranscoderOutput}.
     * <p>
     * This method of transcoding SVG images is derived from @Devon_C_Miller's answer
     * to <a href="https://stackoverflow.com/questions/2495501/swing-batik-create-an-imageicon-from-an-svg-file">
     * this StackOverflow question</a>. Thanks Devon!
     */
    private static class SVGToBufferedImageTranscoder extends ImageTranscoder {
        private BufferedImage output = null;

        /**
         * Transcodes the specified input without creating any output.
         * After this operation, the resulting {@link BufferedImage} can be retrieved via {@link #getOutput()}.
         *
         * @param input The input to transcode.
         * @throws TranscoderException Thrown if an error occurred while transcoding.
         */
        public void transcode(TranscoderInput input) throws TranscoderException {
            this.transcode(input, null);
        }

        /**
         * Get the image generated by the most recent call to {@link #transcode(TranscoderInput)}.
         *
         * @return The image, or {@code null} if no previous transcode operation has been done.
         */
        public BufferedImage getOutput() {
            return this.output;
        }

        /**
         * Create a new image with the specified dimensions. There is probably no reason to call this
         * directly; it is primarily used by the superclass to begin the transcode operation.
         * @see BufferedImage#BufferedImage(int, int, int)
         *
         * @param width  The width of the image in pixels.
         * @param height The height of the image in pixels.
         * @return A blank ARGB {@code BufferedImage} with the specified width and height.
         */
        @Override
        public BufferedImage createImage(int width, int height) {
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }

        /**
         * <b>Do not use this method.</b> Calling it has no effect.
         * To get the {@link BufferedImage} from this transcoder, use {@link #getOutput()} instead.
         */
        @Override
        public void writeImage(BufferedImage image, TranscoderOutput output) {
            // This method will be called by the superclass, but we will ignore the output passed in,
            // instead updating our own output field (how rebellious of us)
            this.output = image;
        }
    }
}
