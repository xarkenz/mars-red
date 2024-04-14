package mars.venus;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/////////////////////////////  CREDIT  /////////////////////////////////////
// http://forums.sun.com/thread.jspa?threadID=499183&messageID=2505646
// bsampieri, 4 March 2004
// Java Developer Forum, Useful Code of the Day: Button Fires Events While Held
// Adopted/adapted by DPS 20 July 2008
//
// This is NOT one of the MARS buttons!  It is a subclass of JButton that can
// be used to create buttons that fire events after being held down for a 
// specified period of time and at a specified rate.

/**
 * <code>RepeatButton</code> is a <code>JButton</code> which contains a timer
 * for firing events while the button is held down.  There is a default
 * initial delay of 300ms before the first event is fired and a 60ms delay
 * between subsequent events.  When the user holds the button down and moves
 * the mouse out from over the button, the timer stops, but if the user moves
 * the mouse back over the button without having released the mouse button,
 * the timer starts up again at the same delay rate.  If the enabled state is
 * changed while the timer is active, it will be stopped.
 * <p>
 * NOTE:  The normal button behavior is that the action event is fired after
 * the button is released.  It may be important to know then that this is
 * still the case.  So in effect, listeners will get 1 more event then what
 * the internal timer fires.  It's not a "bug", per se, just something to be
 * aware of.  There seems to be no way to suppress the final event from
 * firing anyway, except to process all ActionListeners internally.  But
 * realistically, it probably doesn't matter.
 */
public class RepeatButton extends JButton implements ActionListener, MouseListener {
    /**
     * The pressed state for this button.
     */
    private boolean pressed = false;
    /**
     * Flag to indicate that the button should fire events when held.
     * If false, the button is effectively a plain old JButton, but
     * there may be times when this feature might wish to be disabled.
     */
    private boolean repeatEnabled = true;
    /**
     * The hold-down timer for this button.
     */
    private Timer timer = null;
    /**
     * The initial delay for this button.  Hold-down time before first
     * timer firing.  In milliseconds.
     */
    private int initialDelay = 300;
    /**
     * The interval between timer firings for this button once the delay
     * period is past.  In milliseconds.
     */
    private int interval = 60;
    /**
     * Holder of the modifiers used when the mouse pressed the button.
     * This is used for subsequently fired action events.  This may change
     * after mouse pressed if the user moves the mouse out, releases a key
     * and then moves the mouse back in.
     */
    private int modifiers = 0;

    /**
     * Creates a button with no set text or icon.
     */
    public RepeatButton() {
        super();
        init();
    }

    /**
     * Creates a button where properties are taken from the Action supplied.
     *
     * @param action the button action
     */
    public RepeatButton(Action action) {
        super(action);
        init();
    }

    /**
     * Creates a button with an icon.
     *
     * @param icon the button icon
     */
    public RepeatButton(Icon icon) {
        super(icon);
        init();
    }

    /**
     * Creates a button with text.
     *
     * @param text the button text
     */
    public RepeatButton(String text) {
        super(text);
        init();
    }

    /**
     * Creates a button with initial text and an icon.
     *
     * @param text the button text
     * @param icon the button icon
     */
    public RepeatButton(String text, Icon icon) {
        super(text, icon);
        init();
    }

    /**
     * Initializes the button.
     */
    private void init() {
        this.addMouseListener(this);
        // Initialize timers for button holding
        this.timer = new Timer(this.interval, this);
        this.timer.setRepeats(true);
    }

    /**
     * Gets the interval time between repetitions after the initial delay.
     *
     * @return the interval time in milliseconds
     */
    public int getInterval() {
        return this.interval;
    }

    /**
     * Set the interval time between repetitions after the initial delay.
     *
     * @param interval the interval time in milliseconds
     */
    public void setInterval(int interval) {
        this.interval = interval;
    }

    /**
     * Gets the initial delay time before repetitions begin.
     *
     * @return the initial delay time in milliseconds
     */
    public int getInitialDelay() {
        return this.initialDelay;
    }

    /**
     * Sets the initial delay time before repetitions begin.
     *
     * @param delay the initial delay time in milliseconds
     */
    public void setInitialDelay(int delay) {
        this.initialDelay = delay;
    }

    /**
     * Checks if the button should fire events when held.  If false, the
     * button is effectively a plain old JButton, but there may be times
     * when this feature might wish to be disabled.
     *
     * @return if true, the button should fire events when held
     */
    public boolean isRepeatEnabled() {
        return this.repeatEnabled;
    }

    /**
     * Sets if the button should fire events when held.  If false, the
     * button is effectively a plain old JButton, but there may be times
     * when this feature might wish to be disabled.  If false, it will
     * also stop the timer if it's running.
     *
     * @param enable if true, the button should fire events when held
     */
    public void setRepeatEnabled(boolean enable) {
        if (!enable) {
            this.pressed = false;
            if (timer.isRunning()) {
                timer.stop();
            }
        }
        this.repeatEnabled = enable;
    }

    /**
     * Sets the enabled state of this button.  Overridden to stop the timer
     * if it's running.
     *
     * @param enable if true, enables the button
     */
    @Override
    public void setEnabled(boolean enable) {
        if (enable != super.isEnabled()) {
            this.pressed = false;
            if (timer.isRunning()) {
                timer.stop();
            }
        }
        super.setEnabled(enable);
    }

    /**
     * Handle action events. OVERRIDE THIS IN SUBCLASS!
     *
     * @param event the action event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        // Process events from this component only
        if (event.getSource() == this.timer) {
            super.fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, super.getActionCommand(), this.modifiers));
        }
    }

    /**
     * Handle mouse clicked events.
     *
     * @param event the mouse event
     */
    @Override
    public void mouseClicked(MouseEvent event) {
        // Process events from this component only
        if (event.getSource() == this) {
            this.pressed = false;
            if (this.timer.isRunning()) {
                this.timer.stop();
            }
        }
    }

    /**
     * Handle mouse pressed events.
     *
     * @param event the mouse event
     */
    @Override
    public void mousePressed(MouseEvent event) {
        // Process events from this component only
        if (event.getSource() == this && this.isEnabled() && this.isRepeatEnabled()) {
            this.pressed = true;
            if (!this.timer.isRunning()) {
                this.modifiers = event.getModifiersEx();
                this.timer.setInitialDelay(this.initialDelay);
                this.timer.start();
            }
        }
    }

    /**
     * Handle mouse released events.
     *
     * @param event the mouse event
     */
    @Override
    public void mouseReleased(MouseEvent event) {
        // Process events from this component only
        if (event.getSource() == this) {
            this.pressed = false;
            if (this.timer.isRunning()) {
                this.timer.stop();
            }
        }
    }

    /**
     * Handle mouse entered events.
     *
     * @param event the mouse event
     */
    @Override
    public void mouseEntered(MouseEvent event) {
        // Process events from this component only
        if (event.getSource() == this && this.isEnabled() && this.isRepeatEnabled()) {
            if (this.pressed && !this.timer.isRunning()) {
                this.modifiers = event.getModifiersEx();
                this.timer.setInitialDelay(this.interval);
                this.timer.start();
            }
        }
    }

    /**
     * Handle mouse exited events.
     *
     * @param event the mouse event
     */
    @Override
    public void mouseExited(MouseEvent event) {
        // Process events from this component only
        if (event.getSource() == this) {
            if (this.timer.isRunning()) {
                this.timer.stop();
            }
        }
    }
}
