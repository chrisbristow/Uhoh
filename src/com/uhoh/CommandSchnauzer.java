package com.uhoh;

import java.io.*;
import java.util.*;

public class CommandSchnauzer extends Schnauzer
{
  String command;
  String active_string;
  long interval;
  String regex = "";

  CommandSchnauzer(String c, String a, String t, EventCollector ec, long i, String r)
  {
    command = c;
    active_string = a;
    tags = t;
    event_collector = ec;
    interval = i;
    regex = r;
  }

  public void run()
  {
    log("Running " + command + " every " + interval + " ms (" + active_string + " / " + tags + ")");

    while(keep_running)
    {
      try
      {
        if(is_active(active_string))
        {
          String line;
          Process p = Runtime.getRuntime().exec(command);
          BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

          while((line = input.readLine()) != null)
          {
            //log("Cmd: " + line);

            if(line.trim().matches(regex))
            {
              string_processor(line.trim());
            }
          }

          input.close();
        }
      }
      catch(Exception e)
      {
        log("An exception occurred running " + command);
        e.printStackTrace();
      }

      do_pause(interval);
    }

    log("Running of " + command + " stopped");
  }
}
