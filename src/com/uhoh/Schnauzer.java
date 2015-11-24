// A Schnauzer is the base class for all monitorable items
// (eg. files, processes etc.)
// This is where we stash common helper methods used by
// derived items.

package com.uhoh;

import java.util.*;

public abstract class Schnauzer extends UhohBase implements Runnable
{
  String tags = "";
  EventCollector event_collector = null;
  boolean keep_running = true;
  
  // string_processor() is overridden for all items - ie. all
  // different types of FileSchnauzer, ProcessSchnauzer etc.
  // The implementation here is just made available as a fall-back
  // for debug use whilst derived classes are being developed.
  
  public void string_processor(String s)
  {
    event_collector.dispatch("SYSTEM%%" + tags + "%%" + s);
  }
  
  // Called periodically.
  
  public void timed_event_processor()
  {
    // Does nothing by default.
  }
}
