// Consumes all events.

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

import java.util.concurrent.*;
import java.util.*;

public class EventCollector extends UhohBase implements Runnable
{
  LinkedBlockingQueue<Object[]> q = null;
  ArrayList<MultiMatcher> multi_list = null;
  
  // Basic EventCollector constructor.
  
  EventCollector()
  {
    q = new LinkedBlockingQueue<Object[]>();
    multi_list = new ArrayList<MultiMatcher>();
  }
  
  // This is the basic EventCollector loop.
  
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
        log("Exception: EventCollector take()");
        e.printStackTrace();
      }
    }
  }
  
  // Override this method to customise processing of events.
  
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
  
  // Called by external threads to send a message to the EventCollector.
  
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
