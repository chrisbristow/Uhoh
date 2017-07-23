/*
        Licence
        -------
        Copyright (c) 2015-2017, Chris Bristow
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

import org.w3c.dom.DOMImplementation;

import java.io.*;
import java.util.*;

/*
  The CommandSchnauzer() runs a command periodically and collects the command output.
 */

public class CommandSchnauzer extends Schnauzer
{
  String command;
  String active_string;
  long interval;
  String regex = "";
  String capture = "";
  long greater_than = 999999999999999L;
  long less_than = -1;
  String threshold_tags = null;

  // A CommandSchnauzer() is started with the following arguments:
  // - The command to run.
  // - During which times/days the command should run.
  // - Tags to attach to alerts raised from the command output.
  // - A reference to the EventCollector().
  // - The time interval between invocations of the command.
  // - A regex to use to match output from the command.

  CommandSchnauzer(String c, String a, String t, EventCollector ec, long i, String r, String cptr, String lt, String gt, String tt)
  {
    command = c;
    active_string = a;
    tags = t;
    event_collector = ec;
    interval = i;
    regex = r;
    capture = cptr;
    threshold_tags = tt;

    try
    {
      if(gt != null)
      {
        greater_than = Long.parseLong(gt);
        log(" - Alert if N > " + greater_than);
      }

      if(lt != null)
      {
        less_than = Long.parseLong(lt);
        log(" - Alert if N < " + less_than);
      }

      if(threshold_tags != null)
      {
        log(" - Tags for threshold breaches: " + threshold_tags);
      }
    }
    catch(Exception e)
    {
      log("Warning: Unable to parse minimum / maximum / threshold tags configuration parameters");
    }
  }

  // The run() method actually runs the command, collects the output, filters
  // using the regex and dispatches any alerts.  Note that a command can trigger
  // multiple alerts each time it runs.

  public void run()
  {
    log("Running " + command + " every " + interval + " ms (" + active_string + " / " + tags + ")");

    if(capture.length() != 0)
    {
      log(" - Capture using: " + capture);
    }

    while(keep_running)
    {
      try
      {
        if(is_active(active_string))
        {
          log("Running " + command);

          String line;
          Process p = Runtime.getRuntime().exec(command);
          BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

          while((line = input.readLine()) != null)
          {
            log("Command output: " + line);

            if(capture.length() != 0)
            {
              if(line.trim().matches(".*" + capture + ".*"))
              {
                String cap_val = translate_string(line.trim(), capture);
                string_processor(command + ": " + cap_val);

                try
                {
                  double act_cap_val = Double.parseDouble(cap_val);

                  if(act_cap_val > greater_than)
                  {
                    string_processor(command + ": Exceeded upper bound", tags, threshold_tags);
                  }

                  if(act_cap_val < less_than)
                  {
                    string_processor(command + ": Below lower bound", tags, threshold_tags);
                  }
                }
                catch(Exception e)
                {
                  log("Warning: Bounds checking has not been passed a numerical value");
                }
              }
            }
            else if(line.trim().matches(".*" + regex + ".*"))
            {
              string_processor(command + ": " + line.trim());
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

  // Override string_processor(s) from the Schnauzer() class so that alternative tags can be
  // attached for alert_cmd.

  public void string_processor(String s, String ord_tags, String alt_tags)
  {
    String the_tags = ord_tags;

    if(alt_tags != null)
    {
      the_tags = alt_tags;
    }

    event_collector.dispatch("CLIENT%%" + the_tags + "%%" + s, "ALL");
  }
}
