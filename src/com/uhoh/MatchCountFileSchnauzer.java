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

import java.util.*;

public class MatchCountFileSchnauzer extends BasicMatchFileSchnauzer
{
  long match_count = 0;
  long metrics_interval = 60000;
  long next_checkpoint = 0;
  
  // Construct a MatchCountFileSchnauzer.
  
  MatchCountFileSchnauzer(String f, String a, String t, EventCollector ec, String r, long m)
  {
    super(f, a, t, ec, r, "", "");
    metrics_interval = m;
    next_checkpoint = (new Date()).getTime() + metrics_interval;
    log(" - Interval is " + m + " ms");
  }
  
  // Override string_processor() to add the regex match and count.
  
  public void string_processor(String s)
  {
    if(is_matching(s))
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
        event_collector.dispatch("CLIENT%%" + tags + "%%" + filename + ": " + match_count, "FILE");
      }

      match_count = 0;
      next_checkpoint = (new Date()).getTime() + metrics_interval;
    }
  }
}