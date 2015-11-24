package com.uhoh;

import java.util.Date;

public class ThresholdFileSchnauzer extends MatchCountFileSchnauzer
{
  long greater_than = -1;
  long less_than = -1;
  
  //Construct a ThresholdFileSchnauzer.
  
  ThresholdFileSchnauzer(String f, String a, String t, EventCollector ec, String r, long m, long lt, long gt, String msg)
  {
    super(f, a, t, ec, r, m);
    greater_than = gt;
    less_than = lt;
    message = msg;
    
    if(lt != -1)
    {
      log(" - Alert if N < " + lt);
    }
    
    if(gt != -1)
    {
      log(" - Alert if N > " + gt);
    }

    if(message.length() != 0)
    {
      log(" - Message: " + message);
    }
  }
  
  // Output match count if count falls outside of the given
  // range.  Exclude range settings of -1.
  
  public void timed_event_processor()
  {
    if((new Date()).getTime() > next_checkpoint)
    {
      if(is_active(active_string))
      {
        if((less_than != -1 && match_count < less_than) || (greater_than != -1 && match_count > greater_than))
        {
          if(message.length() == 0)
          {
            event_collector.dispatch("SYSTEM%%" + tags + "%%" + filename + ": " + match_count, "FILE");
          }
          else
          {
            event_collector.dispatch("SYSTEM%%" + tags + "%%" + filename + ": " + message, "FILE");
          }
        }
      }
      
      match_count = 0;
      next_checkpoint = (new Date()).getTime() + metrics_interval;
    }
  }
}
