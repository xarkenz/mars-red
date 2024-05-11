package mars.mips.instructions.syscalls;

import javax.sound.midi.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/*
Copyright (c) 2003-2007,  Pete Sanderson and Kenneth Vollmar

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

/*
 * The ToneGenerator and Tone classes were developed by Otterbein College
 * student Tony Brock in July 2007.  They simulate MIDI output through the
 * computers soundcard using classes and methods of the javax.sound.midi
 * package.
 *
 * Max Hailperin <max@gustavus.edu> changed the interface of the
 * ToneGenerator class 2009-10-19 in order to
 * (1) provide a reliable way to wait for the completion of a
 *     synchronous tone,
 * and while he was at it,
 * (2) improve the efficiency of asynchronous tones by using a thread
 *     pool executor, and
 * (3) simplify the interface by removing all the unused versions
 *     that provided default values for various parameters
 *
 * Sean Clarke renamed ToneGenerator to MidiNotePlayer and overhauled the
 * implementation from top to bottom by eliminating the use of the sequencer,
 * as well any message-passing whatsoever, in 05/2024. The new implementation
 * makes use of the Java 1.5 concurrency features to create an event scheduler
 * that "hibernates" when it sees no activity for a while. This scheduler is
 * then used to interact directly with the MIDI synthesizer by immediately turning
 * a note on, then scheduling a task to run after the given duration to turn it off.
 */

/**
 * Used by the MIDI syscalls to play MIDI notes on demand.
 */
public class MidiNotePlayer {
    /**
     * The default pitch value for the tone: 60 / middle C.
     */
    public static final byte DEFAULT_PITCH = 60;
    /**
     * The default duration of the tone: 1000 milliseconds.
     */
    public static final int DEFAULT_DURATION = 1000;
    /**
     * The default instrument of the tone: 0 / piano.
     */
    public static final byte DEFAULT_INSTRUMENT = 0;
    /**
     * The default volume of the tone: 100 (of 127).
     */
    public static final byte DEFAULT_VOLUME = 100;
    /**
     * The default MIDI channel of the tone: 0 (channel 1).
     */
    public static final int DEFAULT_CHANNEL = 0;

    /**
     * The number of threads that will always be kept in the scheduler's thread pool, even if they are idle.
     * 1 for the idle hibernation timer, 1 for the initial note played.
     */
    private static final int CORE_THREAD_POOL_SIZE = 2;
    /**
     * The amount of time, in milliseconds, the idle timer should wait for before starting hibernation
     * once no notes are currently playing. If any notes play during this period, the idle timer is reset.
     */
    private static final int IDLE_TIMER_WAIT_MS = 10000;
    /**
     * The lock object used to synchronize all accesses to the MIDI synthesizer.
     * I don't know whether this is necessary... it doesn't seem to make a difference either way.
     */
    private static final Object SYNTHESIZER_LOCK = new Object();

    private static ScheduledExecutorService scheduler = null;
    private static ScheduledFuture<?> idleTimer = null;
    private static Synthesizer synthesizer = null;

    /**
     * Play a note with the specified pitch, duration, and instrument, and volume, returning immediately.
     *
     * @param pitch      The desired pitch in semitones - 0-127 where 60 is middle C.
     * @param duration   The desired duration in milliseconds.
     * @param instrument The desired instrument (or patch) represented by a positive byte value (0-127).  See the
     *                   <a href="http://www.midi.org/about-midi/gm/gm1sound.shtml#instrument">general
     *                   MIDI instrument patch map</a> for more instruments associated with each value.
     * @param volume     The desired volume of the initial attack of the
     *                   note (MIDI velocity) represented by a positive byte value (0-127).
     */
    public static ScheduledFuture<?> playNote(int pitch, int duration, int instrument, int volume) {
        synchronized (SYNTHESIZER_LOCK) {
            Synthesizer synthesizer = getSynthesizer();
            if (synthesizer == null) {
                // Unable to secure a MIDI synthesizer, so pretend like a note is being played to keep timing intact
                ScheduledFuture<?> future = getScheduler().schedule(() -> {}, duration, TimeUnit.MILLISECONDS);
                updateIdleTimer(duration);
                return future;
            }
            // Set up the synthesizer instrument
            synthesizer.loadInstrument(synthesizer.getDefaultSoundbank().getInstruments()[instrument]);
            synthesizer.getChannels()[DEFAULT_CHANNEL].programChange(instrument);
            // Begin playing the note with the given pitch and volume
            synthesizer.getChannels()[DEFAULT_CHANNEL].noteOn(pitch, volume);
        }

        // Schedule the note to end after the given duration has passed
        // This doesn't time it perfectly, but it's probably good enough, at least for now
        ScheduledFuture<?> future = getScheduler().schedule(() -> {
            synchronized (SYNTHESIZER_LOCK) {
                Synthesizer synthesizer = getSynthesizer();
                if (synthesizer != null) {
                    // Stop playing the note with the given pitch
                    synthesizer.getChannels()[DEFAULT_CHANNEL].noteOff(pitch);
                }
            }
        }, duration, TimeUnit.MILLISECONDS);
        // Ensure the idle timer will accommodate the newly created note
        updateIdleTimer(duration);
        return future;
    }

    private static ScheduledExecutorService getScheduler() {
        if (scheduler == null) {
            // Initialize the thread pool for the scheduler
            scheduler = Executors.newScheduledThreadPool(CORE_THREAD_POOL_SIZE);
        }
        return scheduler;
    }

    private static Synthesizer getSynthesizer() {
        if (synthesizer == null) {
            try {
                // Obtain a MIDI synthesizer for use (this takes a pretty hefty amount of time...)
                synthesizer = MidiSystem.getSynthesizer();
                synthesizer.open();
            }
            catch (MidiUnavailableException exception) {
                // Couldn't get a working synthesizer, so keep synthesizer as null
                if (synthesizer != null) {
                    synthesizer.close();
                    synthesizer = null;
                }
            }
        }
        return synthesizer;
    }

    private static void updateIdleTimer(int duration) {
        // Calculate the total delay, after which hibernate() will be called if no other notes are played
        int delay = duration + MidiNotePlayer.IDLE_TIMER_WAIT_MS;
        if (idleTimer != null) {
            if (idleTimer.getDelay(TimeUnit.MILLISECONDS) >= delay) {
                // The idle timer is scheduled to start hibernation later than this call would, so keep the later time
                return;
            }
            // We need a longer delay than the current timer is set for, so cancel the current one
            idleTimer.cancel(false);
        }
        // Set a new timer to call hibernate() after the total delay has elapsed
        idleTimer = getScheduler().schedule(MidiNotePlayer::hibernate, delay, TimeUnit.MILLISECONDS);
    }

    private static void hibernate() {
        synchronized (SYNTHESIZER_LOCK) {
            // Close the MIDI synthesizer device (sometimes a "pop" can be heard when this occurs)
            if (synthesizer != null) {
                synthesizer.close();
            }
            synthesizer = null;
        }
        // Effectively free the resources used by the scheduler and idle timer
        scheduler = null;
        idleTimer = null;
    }
}
