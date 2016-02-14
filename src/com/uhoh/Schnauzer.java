/*
        Licence
        -------
        Copyright (c) 2015, Chris Bristow
        All rights reserved.

        Redistribution and use in source and binary forms, with or without
        modification, are permitted provided that the following conditions are met:

        1. Redistributions of source code must retain the above copyright notice, this
        list of conditions and the following disclaimer.
        2. Redistributions in binary form must reproduce the above copyright notice,
        this list of conditions and the following disclaimer in the documentation
        and/or other materials provided with the distribution.

        THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
        ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
        WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
        DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
        ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
        (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
        LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
        ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
        (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
        SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

        The views and conclusions contained in the software and documentation are those
        of the authors and should not be interpreted as representing official policies,
        either expressed or implied, of the FreeBSD Project.
*/

package com.uhoh;

/*
  A Schnauzer() is the base class for all monitorable items
  (eg. files, processes etc.)
  */

public abstract class Schnauzer extends UhohBase implements Runnable
{
  // Contains the list of tags attached to alerts raised by this schnauzer.
  String tags = "";

  // A reference to the EventCollector() where alerts are dispatched to.
  EventCollector event_collector = null;

  // Setting keep_running to false causes a Schnauzer() to complete it's
  // current work and close down.  This happens when an Uhoh Client
  // is re-configured.
  boolean keep_running = true;
  
  // The string_processor() method is overridden for all items - ie. all
  // different types of FileSchnauzer, ProcessSchnauzer etc.
  // The implementation here is just made available as a fall-back
  // for debug use whilst derived classes are being developed.
  
  public void string_processor(String s)
  {
    event_collector.dispatch("CLIENT%%" + tags + "%%" + s, "ALL");
  }
  
  // Certain schnauzers run periodic checks.  The timed_event_processor() is a method
  // which contains the logic which is run periodically.  The default implementation
  // of timed_event_processor() does nothing an is overridden by classes derived
  // from Schnauzer().
  
  public void timed_event_processor()
  {
    // Does nothing by default.
  }
}
