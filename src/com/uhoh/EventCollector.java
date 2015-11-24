// Consumes all events.

package com.uhoh;

import java.util.concurrent.*;

public class EventCollector extends UhohBase implements Runnable
{
  LinkedBlockingQueue<String> q = null;
  
  // Basic EventCollector constructor.
  
  EventCollector()
  {
    q = new LinkedBlockingQueue<String>();
  }
  
  // This is the basic EventCollector loop.
  
  public void run()
  {
    log("Starting an EventCollector");
    
    while(true)
    {
      try
      {
        String event = (String)q.poll(67, TimeUnit.SECONDS);
        
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
  
  void process_event(String event)
  {
    if(event == null)
    {
      event = "";
    }

    log(event);
  }
  
  // Called by external threads to send a message to the EventCollector.
  
  public void dispatch(String event)
  {
    try
    {
      q.put(event);
    }
    catch(Exception e)
    {
      log("Exception: EventCollector put()");
      e.printStackTrace();
    }
  }
}
