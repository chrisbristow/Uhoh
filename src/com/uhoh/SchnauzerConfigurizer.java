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
  The SchnauzerConfigurizer() class is used to parse a Client's configuration
  and spawn Schnauzer() threads which will perform monitoring tasks, reporting
  alerts back to the EventCollector().
 */

public class SchnauzerConfigurizer extends UhohBase
{
  ArrayList<Schnauzer> schs;

  // A SchnauzerConfigurizer() is created with the configuration details
  // and a reference to the EventCollector() passed in as arguments.

  SchnauzerConfigurizer(String in_data, EventCollector event_collector)
  {
    String file = null;
    String match_str = null;
    String active = "ANY";
    String capture = "";
    String ps_command = null;
    HashMap<String, Object[]> ps_hash = new HashMap<String, Object[]>();
    event_collector.multi_list = new ArrayList<MultiMatcher>();

    // An array of Schnauzers() created is maintained and this array is
    // used to send shut down messages to Schnauzers() if a Client's
    // configuration needs to be re-loaded.

    schs = new ArrayList<Schnauzer>();
    
    StringTokenizer st = new StringTokenizer(in_data, "%%");
    
    while(st.hasMoreTokens())
    {
      String config_line = st.nextToken();
      
      //log("Config: " + config_line);

      // The following block is a large switch table which performs the parsing
      // of the configuration and starts Schnauzer() threads as necessary.
      
      try
      {
        if(config_line.startsWith("file:"))
        {
          file = config_line.replaceFirst("^file:\\s+", "");
        }
        else if(config_line.startsWith("active:"))
        {
          active = config_line.replaceFirst("^active:\\s+", "");
        }
        else if(config_line.startsWith("match:"))
        {
          match_str = config_line.replaceFirst("^match:\\s+", "");
        }
        else if(config_line.startsWith("capture:"))
        {
          capture = config_line.replaceFirst("^capture:\\s+", "");
        }
        else if(config_line.startsWith("ps_command:"))
        {
          ps_command = config_line.replaceFirst("^ps_command:\\s+", "");
        }
        else if(config_line.startsWith("alert_all:"))
        {
          if(file != null && (match_str != null || !capture.equals("")))
          {
            HashMap<String, String> args = get_kvps(config_line);
            String message = "";

            if(args.get("message") != null)
            {
              message = config_line.replaceFirst("^\\s*alert_all:\\s+.+message=", "");
            }

            if(args.get("tags") != null)
            {
              BasicMatchFileSchnauzer sh = new BasicMatchFileSchnauzer(file, active, args.get("tags"), event_collector, match_str, message, capture);
              Thread t = new Thread(sh);
              t.start();
              schs.add(sh);
            }
          }

          file = null;
          match_str = null;
          active = "ANY";
          capture = "";
        }
        else if(config_line.startsWith("alert_count:"))
        {
          if(file != null && match_str != null)
          {
            HashMap<String, String> args = get_kvps(config_line);
            
            if(args.get("tags") != null && args.get("seconds") != null)
            {
              MetricCalculationFileSchnauzer sh = new MetricCalculationFileSchnauzer(file, active, args.get("tags"), event_collector, "", Long.parseLong(args.get("seconds")) * 1000, MetricCalcs.COUNT, match_str);
              Thread t = new Thread(sh);
              t.start();
              schs.add(sh);
            }
          }
          
          file = null;
          match_str = null;
          active = "ANY";
          capture = "";
        }
        else if(config_line.startsWith("alert_total:"))
        {
          if(file != null && !capture.equals(""))
          {
            HashMap<String, String> args = get_kvps(config_line);

            if(args.get("tags") != null && args.get("seconds") != null)
            {
              MetricCalculationFileSchnauzer sh = new MetricCalculationFileSchnauzer(file, active, args.get("tags"), event_collector, capture, Long.parseLong(args.get("seconds")) * 1000, MetricCalcs.TOTAL, null);
              Thread t = new Thread(sh);
              t.start();
              schs.add(sh);
            }
          }

          file = null;
          match_str = null;
          active = "ANY";
          capture = "";
        }
        else if(config_line.startsWith("alert_average:"))
        {
          if(file != null && !capture.equals(""))
          {
            HashMap<String, String> args = get_kvps(config_line);

            if(args.get("tags") != null && args.get("seconds") != null)
            {
              MetricCalculationFileSchnauzer sh = new MetricCalculationFileSchnauzer(file, active, args.get("tags"), event_collector, capture, Long.parseLong(args.get("seconds")) * 1000, MetricCalcs.AVERAGE, null);
              Thread t = new Thread(sh);
              t.start();
              schs.add(sh);
            }
          }

          file = null;
          match_str = null;
          active = "ANY";
          capture = "";
        }
        else if(config_line.startsWith("alert_minimum:"))
        {
          if(file != null && !capture.equals(""))
          {
            HashMap<String, String> args = get_kvps(config_line);

            if(args.get("tags") != null && args.get("seconds") != null)
            {
              MetricCalculationFileSchnauzer sh = new MetricCalculationFileSchnauzer(file, active, args.get("tags"), event_collector, capture, Long.parseLong(args.get("seconds")) * 1000, MetricCalcs.MINIMUM, null);
              Thread t = new Thread(sh);
              t.start();
              schs.add(sh);
            }
          }

          file = null;
          match_str = null;
          active = "ANY";
          capture = "";
        }
        else if(config_line.startsWith("alert_maximum:"))
        {
          if(file != null && !capture.equals(""))
          {
            HashMap<String, String> args = get_kvps(config_line);

            if(args.get("tags") != null && args.get("seconds") != null)
            {
              MetricCalculationFileSchnauzer sh = new MetricCalculationFileSchnauzer(file, active, args.get("tags"), event_collector, capture, Long.parseLong(args.get("seconds")) * 1000, MetricCalcs.MAXIMUM, null);
              Thread t = new Thread(sh);
              t.start();
              schs.add(sh);
            }
          }

          file = null;
          match_str = null;
          active = "ANY";
          capture = "";
        }
        else if(config_line.startsWith("alert_range:"))
        {
          if(file != null && match_str != null)
          {
            HashMap<String, String> args = get_kvps(config_line);
            String message = "";

            if(args.get("message") != null)
            {
              message = config_line.replaceFirst("^\\s*alert_range:\\s+.+message=", "");
            }
            
            if(args.get("tags") != null && args.get("seconds") != null)
            {
              if(args.get("minimum") != null && args.get("maximum") != null)
              {
                ThresholdFileSchnauzer sh = new ThresholdFileSchnauzer(file, active, args.get("tags"), event_collector, match_str, Long.parseLong(args.get("seconds")) * 1000, Long.parseLong(args.get("minimum")), Long.parseLong(args.get("maximum")), message);
                Thread t = new Thread(sh);
                t.start();
                schs.add(sh);
              }
              else if(args.get("minimum") != null)
              {
                ThresholdFileSchnauzer sh = new ThresholdFileSchnauzer(file, active, args.get("tags"), event_collector, match_str, Long.parseLong(args.get("seconds")) * 1000, Long.parseLong(args.get("minimum")), -1, message);
                Thread t = new Thread(sh);
                t.start();
                schs.add(sh);
              }
              else if(args.get("maximum") != null)
              {
                ThresholdFileSchnauzer sh = new ThresholdFileSchnauzer(file, active, args.get("tags"), event_collector, match_str, Long.parseLong(args.get("seconds")) * 1000, -1, Long.parseLong(args.get("maximum")), message);
                Thread t = new Thread(sh);
                t.start();
                schs.add(sh);
              }
            }
          }
          
          file = null;
          match_str = null;
          active = "ANY";
          capture = "";
        }
        else if(config_line.startsWith("alert_n:"))
        {
          if(file != null && match_str != null)
          {
            HashMap<String, String> args = get_kvps(config_line);
            String message = "";

            if(args.get("message") != null)
            {
              message = config_line.replaceFirst("^\\s*alert_n:\\s+.+message=", "");
            }
            
            if(args.get("tags") != null && args.get("seconds") != null && args.get("threshold") != null)
            {
              ThresholdFileSchnauzer sh = new ThresholdFileSchnauzer(file, active, args.get("tags"), event_collector, match_str, Long.parseLong(args.get("seconds")) * 1000, -1, Long.parseLong(args.get("threshold")), message);
              Thread t = new Thread(sh);
              t.start();
              schs.add(sh);
            }
          }
          
          file = null;
          match_str = null;
          active = "ANY";
          capture = "";
        }
        else if(config_line.startsWith("alert_inactive:"))
        {
          if(file != null && match_str != null)
          {
            HashMap<String, String> args = get_kvps(config_line);
            String message = "";

            if(args.get("message") != null)
            {
              message = config_line.replaceFirst("^\\s*alert_inactive:\\s+.+message=", "");
            }
            
            if(args.get("tags") != null && args.get("seconds") != null)
            {
              ThresholdFileSchnauzer sh = new ThresholdFileSchnauzer(file, active, args.get("tags"), event_collector, match_str, Long.parseLong(args.get("seconds")) * 1000, 1, -1, message);
              Thread t = new Thread(sh);
              t.start();
              schs.add(sh);
            }
          }
          
          file = null;
          match_str = null;
          active = "ANY";
          capture = "";
        }
        else if(config_line.startsWith("alert_disk:"))
        {
          if(file != null)
          {
            HashMap<String, String> args = get_kvps(config_line);
            
            if(args.get("tags") != null && args.get("maximum") != null)
            {
              DiskSchnauzer sh = new DiskSchnauzer(file, active, args.get("tags"), event_collector, Integer.parseInt(args.get("maximum")));
              Thread t = new Thread(sh);
              t.start();
              schs.add(sh);
            }
          }
          
          file = null;
          match_str = null;
          active = "ANY";
          capture = "";
        }
        else if(config_line.startsWith("alert_process:"))
        {
          if(match_str != null)
          {
            HashMap<String, String> args = get_kvps(config_line);
            
            if(args.get("tags") != null)
            {
              if(args.get("minimum") != null && args.get("maximum") != null)
              {
                ps_hash.put(match_str, new Object[]{ args.get("tags"), active, new Integer(args.get("minimum")), new Integer(args.get("maximum")), 0 });
                log("Checking for process: " + match_str + " (" + args.get("minimum") + " <= N <= " + args.get("maximum") + ") [" + args.get("tags") + "]");
              }
              else if(args.get("exactly") != null)
              {
                ps_hash.put(match_str, new Object[]{ args.get("tags"), active, new Integer(args.get("exactly")), new Integer(args.get("exactly")), 0 });
                log("Checking for process: " + match_str + " (N = " + args.get("exactly") + ") [" + args.get("tags") + "]");
              }
            }
          }

          file = null;
          match_str = null;
          active = "ANY";
          capture = "";
        }
        else if(config_line.startsWith("alert_cmd:"))
        {
          if(config_line.contains("command=") && match_str != null)
          {
            String cmd = config_line.replaceFirst("^\\s*alert_cmd:\\s+.+command=", "");
            HashMap<String, String> args = get_kvps(config_line);

            if(args.get("tags") != null && args.get("seconds") != null && args.get("command") != null)
            {
              log("Running: " + args.get("command") + " every " + args.get("seconds") + " second(s)");
              CommandSchnauzer sh = new CommandSchnauzer(cmd, active, args.get("tags"), event_collector, Long.parseLong(args.get("seconds")) * 1000, match_str);
              Thread t = new Thread(sh);
              t.start();
              schs.add(sh);
            }
          }

          file = null;
          match_str = null;
          active = "ANY";
          capture = "";
        }
        else if(config_line.startsWith("alert_multi:"))
        {
          HashMap<String, String> args = get_kvps(config_line);

          if(args.get("message") != null && args.get("tags") != null && args.get("seconds") != null && args.get("collect") != null)
          {
            String message = config_line.replaceFirst("^\\s*alert_multi:\\s+.+message=", "");
            log("Collecting tags: " + args.get("collect") + " over " + args.get("seconds") + " second(s) to output: " + message + " (" + args.get("tags") + ")");
            event_collector.multi_list.add(new MultiMatcher(args.get("tags"), Long.parseLong(args.get("seconds")), message, args.get("collect"), active, event_collector));
          }

          active = "ANY";
        }
      }
      catch(Exception e)
      {
        log("Exception parsing configuration: " + config_line);
        e.printStackTrace();
      }
    }

    // As there is only ever one active ProcessSchnauzer(), it is started once
    // parsing of the configuration is complete.
    
    if(ps_command != null && ps_hash.size() != 0)
    {
      ProcessSchnauzer sh = new ProcessSchnauzer(event_collector, ps_command, ps_hash);
      Thread t = new Thread(sh);
      t.start();
      schs.add(sh);
    }

    log(schs.size() + " schnauzer(s) have been started");
  }

  // The terminate_all() method is called if all Schnauzers() need to be stopped
  // if the configuration needs to be re-loaded.

  void terminate_all()
  {
    log("Stopping " + schs.size() + " schnauzer(s)");

    for(int i = 0; i < schs.size(); i ++)
    {
      schs.get(i).keep_running = false;
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
