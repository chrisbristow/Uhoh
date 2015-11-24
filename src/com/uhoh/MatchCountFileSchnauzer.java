package com.uhoh;

import java.util.*;

public class MatchCountFileSchnauzer extends BasicMatchFileSchnauzer
{
  long match_count = 0;
  long metrics_interval = 60000;
  long next_checkpoint = 0;
  
  // Construct a MatchCountFileSchnauzer.
  
  MatchCountFileSchnauzer(String f, String a, String t, EventCollector ec, String r, long m)
  {
    super(f, a, t, ec, r, "");
    metrics_interval = m;
    next_checkpoint = (new Date()).getTime() + metrics_interval;
    log(" - Interval is " + m + " ms");
  }
  
  // Override string_processor() to add the regex match and count.
  
  public void string_processor(String s)
  {
    if(is_matching(regex))
    {
      match_count ++;
    }
  }
  
  // Output match count.
  
  public void timed_event_processor()
  {
    if((new Date()).getTime() > next_checkpoint)
    {
      if(is_active(active_string))
      {
        event_collector.dispatch("SYSTEM%%" + tags + "%%" + filename + ": " + match_count, "FILE");
      }

      match_count = 0;
      next_checkpoint = (new Date()).getTime() + metrics_interval;
    }
  }
}
