/*
        Licence
        -------
        Copyright (c) 2015-2017, Chris Bristow
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

import java.util.concurrent.*;
import java.util.*;

/*
  This abstract class is used to derive classes used within the Client by the Client's
  main thread.  It defines the main LinkedBlockingQueue used within the Client
  as well as methods to dispatch to and and read from that queue.
 */

public abstract class EventCollector extends UhohBase
{
  // A LinkedBlockingQueue() is created where all alerts are dispatched to.
  LinkedBlockingQueue<Object[]> q = null;

  // A list of MultiMatcher() objects is declared.  MultiMatcher() objects are used
  // to process derived alerts - ie. alerts that are triggered if certain combinations
  // of other alerts occur with a specific time window.
  ArrayList<MultiMatcher> multi_list = null;
  
  // The constructor creates the shared LinkedBlockingQueue() for capturing all alerts.
  
  EventCollector()
  {
    q = new LinkedBlockingQueue<Object[]>();
    multi_list = new ArrayList<MultiMatcher>();
  }
  
  // This run() method for EventCollector() polls the LinkedBlockingQueue()
  // and then calls the process_event() method for each alert received.  The
  // process_event() method is overridden in order to implement specific
  // alert processing actions.
  
  public void run()
  {
    log("Starting an EventCollector");
    
    while(true)
    {
      try
      {
        Object[] event = (Object[])q.poll(67, TimeUnit.SECONDS);
        
        process_event(event);
      }
      catch(Exception e)
      {
        log("Exception: EventCollector poll()");
        e.printStackTrace();
      }
    }
  }
  
  // The default implementation of the process_event() method, below, simply
  // writes event details to STDOUT.  This is usually overridden to implement
  // more advanced alert processing logic.
  
  void process_event(Object[] event)
  {
    if(event == null)
    {
      log("");
    }
    else
    {
      log((String)event[0]);
    }
  }
  
  // When external threads need to log an event, they use a reference to an EventCollector()
  // (or more often a reference to an object derived from EventCollector()) and call that object's
  // dispatch() method.  The dispatch() method simply places the event into the EventCollector()
  // LinkedBlockingQueue().
  
  public void dispatch(String event, String type)
  {
    try
    {
      q.put(new Object[]{ event, type });
    }
    catch(Exception e)
    {
      log("Exception: EventCollector put()");
      e.printStackTrace();
    }
  }
}
