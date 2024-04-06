package mars.mips.instructions.syscalls;

import javax.sound.midi.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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

public class EndOfTrackListener implements MetaEventListener {
    private boolean endedYet = false;

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
