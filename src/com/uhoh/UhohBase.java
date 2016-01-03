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

public abstract class UhohBase
{
  long client_timeout = 180000;
  long server_heartbeat_interval = 5000;
  long rest_request_timeout = 10000;
  String server_disk_log_name = "server.log";
  long server_disk_log_size = 100000000;
  enum MetricCalcs { TOTAL, AVERAGE, MAXIMUM, MINIMUM };

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
