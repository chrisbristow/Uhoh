package com.uhoh;

import java.util.*;

// Parse the config file and start Schnauzers.

public class SchnauzerConfigurizer extends UhohBase
{
  ArrayList<Schnauzer> schs;

  SchnauzerConfigurizer(String in_data, EventCollector event_collector)
  {
    String file = null;
    String match_str = null;
    String active = "ANY";
    String ps_command = null;
    HashMap<String, Object[]> ps_hash = new HashMap<String, Object[]>();

    schs = new ArrayList<Schnauzer>();
    
    StringTokenizer st = new StringTokenizer(in_data, "%%");
    
    while(st.hasMoreTokens())
    {
      String config_line = st.nextToken();
      
      //log("Config: " + config_line);
      
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
        else if(config_line.startsWith("ps_command:"))
        {
          ps_command = config_line.replaceFirst("^ps_command:\\s+", "");
        }
        else if(config_line.startsWith("alert_all:"))
        {
          if(file != null && match_str != null)
          {
            HashMap<String, String> args = get_kvps(config_line);

            if(args.get("tags") != null)
            {
              BasicMatchFileSchnauzer sh = new BasicMatchFileSchnauzer(file, active, args.get("tags"), event_collector, match_str);
              Thread t = new Thread(sh);
              t.start();
              schs.add(sh);
            }
          }

          file = null;
          match_str = null;
          active = "ANY";
        }
        else if(config_line.startsWith("alert_count:"))
        {
          if(file != null && match_str != null)
          {
            HashMap<String, String> args = get_kvps(config_line);
            
            if(args.get("tags") != null && args.get("seconds") != null)
            {
              MatchCountFileSchnauzer sh = new MatchCountFileSchnauzer(file, active, args.get("tags"), event_collector, match_str, Long.parseLong(args.get("seconds")) * 1000);
              Thread t = new Thread(sh);
              t.start();
              schs.add(sh);
            }
          }
          
          file = null;
          match_str = null;
          active = "ANY";
        }
        else if(config_line.startsWith("alert_range:"))
        {
          if(file != null && match_str != null)
          {
            HashMap<String, String> args = get_kvps(config_line);
            
            if(args.get("tags") != null && args.get("seconds") != null)
            {
              if(args.get("minimum") != null && args.get("maximum") != null)
              {
                ThresholdFileSchnauzer sh = new ThresholdFileSchnauzer(file, active, args.get("tags"), event_collector, match_str, Long.parseLong(args.get("seconds")) * 1000, Long.parseLong(args.get("minimum")), Long.parseLong(args.get("maximum")));
                Thread t = new Thread(sh);
                t.start();
                schs.add(sh);
              }
              else if(args.get("minimum") != null)
              {
                ThresholdFileSchnauzer sh = new ThresholdFileSchnauzer(file, active, args.get("tags"), event_collector, match_str, Long.parseLong(args.get("seconds")) * 1000, Long.parseLong(args.get("minimum")), -1);
                Thread t = new Thread(sh);
                t.start();
                schs.add(sh);
              }
              else if(args.get("maximum") != null)
              {
                ThresholdFileSchnauzer sh = new ThresholdFileSchnauzer(file, active, args.get("tags"), event_collector, match_str, Long.parseLong(args.get("seconds")) * 1000, -1, Long.parseLong(args.get("maximum")));
                Thread t = new Thread(sh);
                t.start();
                schs.add(sh);
              }
            }
          }
          
          file = null;
          match_str = null;
          active = "ANY";
        }
        else if(config_line.startsWith("alert_n:"))
        {
          if(file != null && match_str != null)
          {
            HashMap<String, String> args = get_kvps(config_line);
            
            if(args.get("tags") != null && args.get("seconds") != null && args.get("threshold") != null)
            {
              ThresholdFileSchnauzer sh = new ThresholdFileSchnauzer(file, active, args.get("tags"), event_collector, match_str, Long.parseLong(args.get("seconds")) * 1000, -1, Long.parseLong(args.get("threshold")));
              Thread t = new Thread(sh);
              t.start();
              schs.add(sh);
            }
          }
          
          file = null;
          match_str = null;
          active = "ANY";
        }
        else if(config_line.startsWith("alert_inactive:"))
        {
          if(file != null && match_str != null)
          {
            HashMap<String, String> args = get_kvps(config_line);
            
            if(args.get("tags") != null && args.get("seconds") != null)
            {
              ThresholdFileSchnauzer sh = new ThresholdFileSchnauzer(file, active, args.get("tags"), event_collector, match_str, Long.parseLong(args.get("seconds")) * 1000, 1, -1);
              Thread t = new Thread(sh);
              t.start();
              schs.add(sh);
            }
          }
          
          file = null;
          match_str = null;
          active = "ANY";
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
              }
              else if(args.get("exactly") != null)
              {
                ps_hash.put(match_str, new Object[]{ args.get("tags"), active, new Integer(args.get("exactly")), new Integer(args.get("exactly")), 0 });
              }
            }
          }
          
          match_str = null;
          active = "ANY";
        }
        else if(config_line.startsWith("alert_cmd:"))
        {
          if(config_line.contains("command=") && match_str != null)
          {
            String cmd = config_line.replaceFirst("^\\s*alert_cmd:\\s+.+command=", "");
            HashMap<String, String> args = get_kvps(config_line);

            if(args.get("tags") != null && args.get("seconds") != null && args.get("command") != null)
            {
              CommandSchnauzer sh = new CommandSchnauzer(cmd, active, args.get("tags"), event_collector, Long.parseLong(args.get("seconds")) * 1000, match_str);
              Thread t = new Thread(sh);
              t.start();
              schs.add(sh);
            }
          }

          match_str = null;
          active = "ANY";
        }
      }
      catch(Exception e)
      {
        log("Exception parsing configuration: " + config_line);
        e.printStackTrace();
      }
    }
    
    if(ps_command != null && ps_hash.size() != 0)
    {
      ProcessSchnauzer sh = new ProcessSchnauzer(event_collector, ps_command, ps_hash);
      Thread t = new Thread(sh);
      t.start();
      schs.add(sh);
    }

    log(schs.size() + " schnauzer(s) have been started");
  }

  // Called to stop all Schnauzers (a "reset").

  void terminate_all()
  {
    log("Stopping " + schs.size() + " schnauzer(s)");

    for(int i = 0; i < schs.size(); i ++)
    {
      schs.get(i).keep_running = false;
    }
  }
  
  // Get key=value pairs from a config line.
  
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
