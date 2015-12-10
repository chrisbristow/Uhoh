package com.uhoh;

import java.util.*;

public class MultiMatcher extends UhohBase
{
  long milliseconds;
  String tags;
  String message;
  HashMap<String, Long> collect;
  String active;
  EventCollector event_collector;

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
        event_collector.dispatch("CLIENT%%" + tags + "%%" + message, "FILE");
      }
    }
  }
}
