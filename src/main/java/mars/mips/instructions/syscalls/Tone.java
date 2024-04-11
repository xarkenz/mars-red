package mars.mips.instructions.syscalls;

import javax.sound.midi.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

//
//  The ToneGenerator and Tone classes were developed by Otterbein College
//  student Tony Brock in July 2007.  They simulate MIDI output through the
//  computers soundcard using classes and methods of the javax.sound.midi
//  package.
//
//  Max Hailperin <max@gustavus.edu> changed the interface of the
//  ToneGenerator class 2009-10-19 in order to
//  (1) provide a reliable way to wait for the completion of a
//      synchronous tone,
//  and while he was at it,
//  (2) improve the efficiency of asynchronous tones by using a thread
//      pool executor, and
//  (3) simplify the interface by removing all the unused versions
//      that provided default values for various parameters

/**
 * Contains important variables for a MIDI Tone: pitch, duration
 * instrument (patch), and volume.  The tone can be passed to a thread
 * and will be played using MIDI.
 */
public class Tone implements Runnable {
    /**
     * Tempo of the tone is in milliseconds: 1000 beats per second.
     */
    public final static int TEMPO = 1000;
    /**
     * The default MIDI channel of the tone: 0 (channel 1).
     */
    public final static int DEFAULT_CHANNEL = 0;

    private final byte pitch;
    private final int duration;
    private final byte instrument;
    private final byte volume;

    /**
     * Instantiates a new Tone object, initializing the tone's pitch,
     * duration, instrument (patch), and volume.
     *
     * @param pitch      the pitch in semitones.  Pitch is represented by
     *                   a positive byte value - 0-127 where 60 is middle C.
     * @param duration   the duration of the tone in milliseconds.
     * @param instrument a positive byte value (0-127) which represents
     *                   the instrument (or patch) of the tone.  See the <a href=
     *                   http://www.midi.org/about-midi/gm/gm1sound.shtml#instrument>general
     *                   MIDI instrument patch map</a> for more instruments associated with
     *                   each value.
     * @param volume     a positive byte value (0-127) which represents the
     *                   volume of the initial attack of the note (MIDI velocity).  127 being
     *                   loud, and 0 being silent.
     */
    public Tone(byte pitch, int duration, byte instrument, byte volume) {
        this.pitch = pitch;
        this.duration = duration;
        this.instrument = instrument;
        this.volume = volume;
    }

    /**
     * Plays the tone.
     */
    public void run() {
        playTone();
    }

    /*
     * The following lock and the code which locks and unlocks it
     * around the opening of the Sequencer were added 2009-10-19 by
     * Max Hailperin <max@gustavus.edu> in order to work around a
     * bug in Sun's JDK which causes crashing if two threads race:
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6888117 .
     * This routinely manifested native-code crashes when tones
     * were played asynchronously, on dual-core machines with Sun's
     * JDK (but not on one core or with OpenJDK).  Even when tones
     * were played only synchronously, crashes sometimes occurred.
     * This is likely due to the fact that Thread.sleep was used
     * for synchronization, a role it cannot reliably serve.  In
     * any case, this one lock seems to make all the crashes go
     * away, and the sleeps are being eliminated (since they can
     * cause other, less severe, problems), so that case should be
     * double covered.
     */
    private static final Lock MIDI_PLAYER_LOCK = new ReentrantLock();

    private void playTone() {
        try {
            Sequencer player;
            MIDI_PLAYER_LOCK.lock();
            try {
                player = MidiSystem.getSequencer();
                player.open();
            }
            finally {
                MIDI_PLAYER_LOCK.unlock();
            }

            Sequence sequence = new Sequence(Sequence.PPQ, 1);
            player.setTempoInMPQ(TEMPO);
            Track track = sequence.createTrack();

            // Select instrument
            ShortMessage instrumentMessage = new ShortMessage();
            instrumentMessage.setMessage(ShortMessage.PROGRAM_CHANGE, DEFAULT_CHANNEL, instrument, 0);
            MidiEvent instrumentEvent = new MidiEvent(instrumentMessage, 0);
            track.add(instrumentEvent);

            ShortMessage noteOnMessage = new ShortMessage();
            noteOnMessage.setMessage(ShortMessage.NOTE_ON, DEFAULT_CHANNEL, pitch, volume);
            MidiEvent noteOnEvent = new MidiEvent(noteOnMessage, 0);
            track.add(noteOnEvent);

            ShortMessage noteOffMessage = new ShortMessage();
            noteOffMessage.setMessage(ShortMessage.NOTE_OFF, DEFAULT_CHANNEL, pitch, volume);
            MidiEvent noteOffEvent = new MidiEvent(noteOffMessage, duration);
            track.add(noteOffEvent);

            player.setSequence(sequence);

            EndOfTrackListener listener = new EndOfTrackListener();
            player.addMetaEventListener(listener);

            player.start();

            try {
                listener.awaitEndOfTrack();
            }
            catch (InterruptedException exception) {
                // Ignored
            }
            finally {
                player.close();
            }
        }
        catch (MidiUnavailableException | InvalidMidiDataException exception) {
            exception.printStackTrace();
        }
    }

    /*
     * The EndOfTrackListener was added 2009-10-19 by Max
     * Hailperin <max@gustavus.edu> so that its
     * awaitEndOfTrack method could be used as a more reliable
     * replacement for Thread.sleep.  (Given that the tone
     * might not start playing right away, the sleep could end
     * before the tone, clipping off the end of the tone.)
     */
    private static class EndOfTrackListener implements MetaEventListener {
        private boolean endedYet = false;

        @Override
        public synchronized void meta(MetaMessage message) {
            if (message.getType() == 0x2F) {
                // Meta message type 0x2F indicates "end_of_track"
                endedYet = true;
                notifyAll();
            }
        }

        public synchronized void awaitEndOfTrack() throws InterruptedException {
            while (!endedYet) {
                wait();
            }
        }
    }
}
