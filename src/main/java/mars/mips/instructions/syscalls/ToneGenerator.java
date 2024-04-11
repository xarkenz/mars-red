package mars.mips.instructions.syscalls;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
 * Creates a Tone object and passes it to a thread to "play" it using MIDI.
 */
public class ToneGenerator {
    /**
     * The default pitch value for the tone: 60 / middle C.
     */
    public final static byte DEFAULT_PITCH = 60;
    /**
     * The default duration of the tone: 1000 milliseconds.
     */
    public final static int DEFAULT_DURATION = 1000;
    /**
     * The default instrument of the tone: 0 / piano.
     */
    public final static byte DEFAULT_INSTRUMENT = 0;
    /**
     * The default volume of the tone: 100 (of 127).
     */
    public final static byte DEFAULT_VOLUME = 100;

    private static final Executor THREAD_POOL = Executors.newCachedThreadPool();

    /**
     * Produces a Tone with the specified pitch, duration, and instrument,
     * and volume.
     *
     * @param pitch      the desired pitch in semitones - 0-127 where 60 is
     *                   middle C.
     * @param duration   the desired duration in milliseconds.
     * @param instrument the desired instrument (or patch) represented
     *                   by a positive byte value (0-127).  See the <a href=
     *                   http://www.midi.org/about-midi/gm/gm1sound.shtml#instrument>general
     *                   MIDI instrument patch map</a> for more instruments associated with
     *                   each value.
     * @param volume     the desired volume of the initial attack of the
     *                   Tone (MIDI velocity) represented by a positive byte value (0-127).
     */
    public void generateTone(byte pitch, int duration, byte instrument, byte volume) {
        Runnable tone = new Tone(pitch, duration, instrument, volume);
        THREAD_POOL.execute(tone);
    }

    /**
     * Produces a Tone with the specified pitch, duration, and instrument,
     * and volume, waiting for it to finish playing.
     *
     * @param pitch      the desired pitch in semitones - 0-127 where 60 is
     *                   middle C.
     * @param duration   the desired duration in milliseconds.
     * @param instrument the desired instrument (or patch) represented
     *                   by a positive byte value (0-127).  See the <a href=
     *                   http://www.midi.org/about-midi/gm/gm1sound.shtml#instrument>general
     *                   MIDI instrument patch map</a> for more instruments associated with
     *                   each value.
     * @param volume     the desired volume of the initial attack of the
     *                   Tone (MIDI velocity) represented by a positive byte value (0-127).
     */
    public void generateToneSynchronously(byte pitch, int duration, byte instrument, byte volume) {
        Runnable tone = new Tone(pitch, duration, instrument, volume);
        tone.run();
    }
}
