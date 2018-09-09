/*
        Licence
        -------
        Copyright (c) 2015-2018, Chris Bristow
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

import java.io.*;
import java.util.*;

/*
  UhohBase is a class which is used as a base class for (almost) all other
  classes within the Uhoh system.  It contains global settings and various methods
  which are used throughout Uhoh.
 */

public abstract class UhohBase
{
  // Time (in milliseconds) after which a Client is considered to be dead and
  // the server will log a "lost client" alert.
  long client_timeout = 180000;

  // Time (in milliseconds) after which the Server stops logging "lost client"
  // alerts for Clients considered to be dead.
  long client_remove_time = 300000;

  // Time (in milliseconds) between broadcasts from the Server which indicate
  // to Clients how to contact the Server.
  long server_heartbeat_interval = 5000;

  // Time (in milliseconds) that an incoming REST request to the Server is
  // permitted to take before the Server returns an error.  REST requests
  // are only used to serve the browser user interface.
  long rest_request_timeout = 10000;

  // The rolling log file for the Server or Client.
  String rolling_disk_log_name = "client.log";

  // The maximum size of the Client/Server's rolling log file before it is
  // rolled.
  long rolling_disk_log_size = 1000000;

  // Prefix for log messages.
  String logging_pfx = "C";

  // Used internally to define which type of metric should be captured.
  enum MetricCalcs { TOTAL, AVERAGE, MAXIMUM, MINIMUM, COUNT, THRESHOLD };

  // Used as a file handle for rolling log files.
  FileOutputStream logmgr = null;

  // The log method is used throughout Uhoh to log messages to STDOUT.  It
  // prefixes any message logged with the date/time and name of thread that
  // called log(). It also calls the rolling_log() method to write all
  // log messages to rolling disk files.
  
  String log(String s)
  {
    String log_line = new Date() + " [" + logging_pfx + "] [" + Thread.currentThread().getName() + "]: " + s;
    System.out.println(log_line);
    rolling_log(log_line);
    return(log_line);
  }
  
  // The do_pause() method simply calls Thread.sleep() but catches
  // any exception thrown.
  
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

  // The is_active() method is provided with a string as it's own argument.
  // The string is examined to see if it indicates that the current day/time
  // sits within the definition contained in the string.  If so, true is returned,
  // if not, false.
  //
  // The string is in the format:
  // - A comma-separated list of <days>;<hour/min_range> items.
  // - <days> are days of the week - numbered 1-7 where 1 = Sunday, 2 = Monday etc.
  // - <hour/min_range> is written as start hour/min - end hour/min in the format HH:MM-HH:MM.
  //
  // For example:
  // 23456;09:00-17:30,17;10:00-16:00
  // - Returns true if the current time is within:
  //   09:00 - 17:30 Monday - Friday (days 2, 3, 4, 5 & 6).
  //   or 10:00 - 16:00 Saturday - Sunday (days 1 & 7).
  //
  //  If the string is set to the value "ANY" then true is always returned.

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

  // The rolling_log() method manages the writing of log information to disk.
  // Two files are kept - the current log file and one previous log file.

  void rolling_log(String s)
  {
    try
    {
      if(logmgr == null)
      {
        logmgr = new FileOutputStream(rolling_disk_log_name, true);
      }

      logmgr.write((s + "\n").getBytes());

      if(logmgr.getChannel().size() > rolling_disk_log_size)
      {
        logmgr.close();
        logmgr = null;
        (new File(rolling_disk_log_name)).renameTo(new File(rolling_disk_log_name + ".1"));
        (new File(rolling_disk_log_name)).createNewFile();
      }
    }
    catch(Exception e)
    {
      System.out.println("Exception logging to disk:");
      e.printStackTrace();
    }
  }

  // The get_kvps() method is used to extract key / value pairs
  // from the configuration.

  HashMap<String, String> get_kvps(String conf)
  {
    HashMap<String, String> h = new HashMap<String, String>();
    String[] s1 = conf.split("\\s+");

    for(int i = 0; i < s1.length; i ++)
    {
      String[] s2 = s1[i].split("=");

      if(s2.length > 1)
      {
        h.put(s2[0], s2[1]);
      }
    }

    return(h);
  }
}
