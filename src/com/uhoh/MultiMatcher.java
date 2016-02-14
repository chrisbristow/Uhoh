/*
        Licence
        -------
        Copyright (c) 2016, Chris Bristow
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

/*
  A MultiMatcher() object contains a configuration for an alert which is triggered when
  one or more other alerts are raised.
 */

public class MultiMatcher extends UhohBase
{
  long milliseconds;
  String tags;
  String message;
  HashMap<String, Long> collect;
  String active;
  EventCollector event_collector;

  // To create a MultiMatcher() object, supply:
  // - The list of tags attached to this alert.
  // - The time window (in seconds) within which all qualifying alerts must be
  //   seen in order to trigger this alert.
  // - The message to appear in the multi alert, if triggered.
  // - The time periods that this multi alert can be active within.
  // - The reference to the EventCollector() where alerts will be dispatched
  //   to if triggered.

  MultiMatcher(String t, long s, String m, String c, String a, EventCollector ec)
  {
    tags = t;
    milliseconds = s * 1000L;
    message = m;
    active = a;
    collect = new HashMap<String, Long>();
    event_collector = ec;

    for(String tg : c.split(","))
    {
      collect.put(tg, 0L);
    }
  }

  // The check_for_complete() method is called to check whether any alerts contain tags
  // which would trigger a multi alert.  "complete" is set to true if all tags configured
  // have been seen.

  void check_for_complete(String in_tags)
  {
    boolean complete = true;
    boolean matched = false;

    for(String tg : in_tags.split(","))
    {
      if(collect.containsKey(tg))
      {
        collect.put(tg, System.currentTimeMillis());
        matched = true;
      }
    }

    if(matched)
    {
      Iterator iter = collect.keySet().iterator();

      while(iter.hasNext())
      {
        long min_time = System.currentTimeMillis() - milliseconds;

        if(collect.get(iter.next()) < min_time)
        {
          complete = false;
        }
      }

      if(complete && is_active(active))
      {
        event_collector.dispatch("CLIENT%%" + tags + "%%" + message, "ALL");
      }
    }
  }
}
