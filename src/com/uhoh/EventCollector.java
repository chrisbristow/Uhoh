// Consumes all events.

package com.uhoh;

import java.util.concurrent.*;

public class EventCollector extends UhohBase implements Runnable
{
  LinkedBlockingQueue<Object[]> q = null;
  
  // Basic EventCollector constructor.
  
  EventCollector()
  {
    q = new LinkedBlockingQueue<Object[]>();
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
