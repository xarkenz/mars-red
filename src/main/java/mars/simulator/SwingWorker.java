package mars.simulator;

import javax.swing.*;

/*
 * This file downloaded from the Sun Microsystems URL given below.
 *
 * I will subclass it to create worker thread for running
 * MIPS simulated execution.
 */

/**
 * This is the 3rd version of SwingWorker (also known as
 * SwingWorker 3), an abstract class that you subclass to
 * perform GUI-related work in a dedicated thread.  For
 * instructions on and examples of using this class, see
 * <a href="http://java.sun.com/docs/books/tutorial/uiswing/misc/threads.html">the Swing documentation</a>.
 * Note that the API changed slightly in the 3rd version:
 * You must now invoke {@link #start()} on the SwingWorker after
 * creating it.
 */
public abstract class SwingWorker {
    private Object value;

    /**
     * Class to maintain reference to current worker thread
     * under separate synchronization control.
     */
    private static class ThreadVar {
        private Thread thread;

        ThreadVar(Thread thread) {
            this.thread = thread;
        }

        synchronized Thread get() {
            return thread;
        }

        synchronized void clear() {
            thread = null;
        }
    }

    private final ThreadVar threadVar;

    /**
     * Get the value produced by the worker thread, or null if it
     * hasn't been constructed yet.
     */
    protected synchronized Object getValue() {
        return value;
    }

    /**
     * Set the value produced by worker thread.
     */
    private synchronized void setValue(Object value) {
        this.value = value;
    }

    /**
     * Compute the value to be returned by the <code>get</code> method.
     */
    public abstract Object construct();

    /**
     * Called on the event dispatching thread (not on the worker thread)
     * after the <code>construct</code> method has returned.
     */
    public void finished() {
    }

    /**
     * A new method that interrupts the worker thread.  Call this method
     * to force the worker to stop what it's doing.
     */
    public void interrupt() {
        Thread thread = threadVar.get();
        if (thread != null) {
            thread.interrupt();
        }
        threadVar.clear();
    }

    /**
     * Return the value created by the <code>construct</code> method.
     * Returns null if either the constructing thread or the current
     * thread was interrupted before a value was produced.
     *
     * @return the value created by the <code>construct</code> method
     */
    public Object get() {
        while (true) {
            Thread thread = threadVar.get();
            if (thread == null) {
                return getValue();
            }
            try {
                thread.join();
            }
            catch (InterruptedException exception) {
                Thread.currentThread().interrupt(); // propagate
                return null;
            }
        }
    }

    /**
     * Start a thread that will call the {@link #construct()} method
     * and then exit.
     *
     * @param useSwing Set true if MARS is running from GUI, false otherwise.
     */
    public SwingWorker(final boolean useSwing) {
        final Runnable doFinished = this::finished;

        Runnable doConstruct = new Runnable() {
            @Override
            public void run() {
                try {
                    setValue(construct());
                }
                finally {
                    threadVar.clear();
                }

                if (useSwing) {
                    SwingUtilities.invokeLater(doFinished);
                }
                else {
                    doFinished.run();
                }
            }
        };

        // Thread that represents executing MIPS program...
        Thread thread = new Thread(doConstruct, "MIPS");

        threadVar = new ThreadVar(thread);
    }

    /**
     * Start the worker thread.
     */
    public void start() {
        Thread thread = threadVar.get();
        if (thread != null) {
            thread.start();
        }
    }
}
