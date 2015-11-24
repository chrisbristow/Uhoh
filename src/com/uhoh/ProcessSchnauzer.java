package com.uhoh;

import java.util.*;
import java.io.*;

public class ProcessSchnauzer extends Schnauzer
{
  String ps_command;
  HashMap<String, Object[]> ps_hash;
  
  // Start a ProcessSchnauzer thread.
  
  ProcessSchnauzer(EventCollector ec, String ps_c, HashMap<String, Object[]> ps_h)
  {
    event_collector = ec;
    ps_command = ps_c;
    ps_hash = ps_h;
  }
  
  // Read the process table, then check for listed processes, count them
  // and compare with limits - if within active times.
  
  public void run()
  {
    log("Starting a process monitor");
    
    while(keep_running)
    {
      try
      {
        String line;
        Process p = Runtime.getRuntime().exec(ps_command);
        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
        
        while((line = input.readLine()) != null)
        {
          Iterator<String> iter = ps_hash.keySet().iterator();
          
          while(iter.hasNext())
          {
            String mtch = iter.next();
            
            if(line.matches(mtch))
            {
              ps_hash.get(mtch)[4] = (Integer)ps_hash.get(mtch)[4] + 1;
            }
          }
        }
        
        input.close();
        
        Iterator<String> iter = ps_hash.keySet().iterator();
        
        while(iter.hasNext())
        {
          String mtch = iter.next();
          String active_string = (String)ps_hash.get(mtch)[1];
          String tags = (String)ps_hash.get(mtch)[0];
          
          if(is_active(active_string))
          {
            if((Integer)ps_hash.get(mtch)[4] < (Integer)ps_hash.get(mtch)[2])
            {
              event_collector.dispatch("SYSTEM%%" + tags + "%%Too few instances of " + mtch + " running", "PROCESS");// (" + ps_hash.get(mtch)[4] + " should be " + ps_hash.get(mtch)[2] + ")");
              log("Process: " + mtch + " " + ps_hash.get(mtch)[4] + " / " + ps_hash.get(mtch)[2] + " -> " + ps_hash.get(mtch)[3]);
            }
            else if((Integer)ps_hash.get(mtch)[4] > (Integer)ps_hash.get(mtch)[3])
            {
              event_collector.dispatch("SYSTEM%%" + tags + "%%Too many instances of " + mtch + " running", "PROCESS");// (" + ps_hash.get(mtch)[4] + " should be " + ps_hash.get(mtch)[3] + ")");
              log("Process: " + mtch + " " + ps_hash.get(mtch)[4] + " / " + ps_hash.get(mtch)[2] + " -> " + ps_hash.get(mtch)[3]);
            }
          }
          
          ps_hash.get(mtch)[4] = 0;
        }
      }
      catch(Exception e)
      {
        log("An exception occurred running " + ps_command);
        e.printStackTrace();
      }
      
      do_pause(60000);
    }
    
    log("Process monitor stopped");
  }
}
