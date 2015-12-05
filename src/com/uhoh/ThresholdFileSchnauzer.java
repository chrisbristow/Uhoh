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
            event_collector.dispatch("CLIENT%%" + tags + "%%" + filename + ": " + match_count, "FILE");
          }
          else
          {
            event_collector.dispatch("CLIENT%%" + tags + "%%" + filename + ": " + message, "FILE");
          }
        }
      }
      
      match_count = 0;
      next_checkpoint = (new Date()).getTime() + metrics_interval;
    }
  }
}
