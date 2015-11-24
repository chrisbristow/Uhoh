package com.uhoh;

import java.util.*;

public abstract class UhohBase
{
  long client_timeout = 180000;
  int max_ui_queue_size = 10;
  long max_ui_retention_time = 300000;
  long server_heartbeat_interval = 5000;
  long rest_request_timeout = 10000;
  String server_disk_log_name = "server.log";
  long server_disk_log_size = 100000000;

  // Generic logging function.
  
  String log(String s)
  {
    String log_line = new Date() + " [" + Thread.currentThread().getName() + "]: " + s;
    System.out.println(log_line);
    return(log_line);
  }
  
  // Wrapper for sleep().
  
  public void do_pause(long n)
  {
    try
    {
      Thread.sleep(n);
    }
    catch(Exception e)
    {
      log("Exception: Sleep()");
    }
  }

  // Check if time now is within the active time string.

  public boolean is_active(String active_string)
  {
    boolean ok = false;

    try
    {
      if(active_string.equals("") || active_string.equals("ANY"))
      {
        ok = true;
      }
      else
      {
        StringTokenizer s1 = new StringTokenizer(active_string, ",");

        while(s1.hasMoreTokens())
        {
          GregorianCalendar gc = new GregorianCalendar();
          String t1 = s1.nextToken();
          StringTokenizer s2 = new StringTokenizer(t1, ";");
          String valid_days = s2.nextToken();

          if(valid_days.contains(gc.get(Calendar.DAY_OF_WEEK) + ""))
          {
            String valid_hours = s2.nextToken();
            StringTokenizer s3 = new StringTokenizer(valid_hours, "-");
            int start_hour_min = Integer.parseInt(s3.nextToken().replaceAll(":", ""));
            int end_hour_min = Integer.parseInt(s3.nextToken().replaceAll(":", ""));
            int time_now = ((gc.get(Calendar.HOUR_OF_DAY) * 100) + gc.get(Calendar.MINUTE));

            if(time_now >= start_hour_min && time_now <= end_hour_min)
            {
              ok = true;
            }
          }
        }
      }
    }
    catch(Exception e)
    {
      log("Exception: Invalid active time string: " + active_string);
    }

    return(ok);
  }
}
