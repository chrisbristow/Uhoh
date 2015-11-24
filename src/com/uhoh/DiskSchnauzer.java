package com.uhoh;

import java.io.*;

public class DiskSchnauzer extends Schnauzer
{
  String filename;
  String active_string;
  int pct_threshold;
  
  // Create a DiskSchnauzer.  Args are:
  // - Filename to indicate filesystem to monitor.
  // - Active time string (DAY;HH:MM-HH:MM[,...]).
  // - Tags.
  // - Reference of the EventCollector.
  // - Threshold (%) to alert if fs usage exceeds.
 
  DiskSchnauzer(String f, String a, String t, EventCollector ec, int pct)
  {
    filename = f;
    active_string = a;
    tags = t;
    event_collector = ec;
    pct_threshold = pct;
    
    log("Filesystem alert if " + filename + " usage is > " + pct_threshold + "% (" + a + " / " + t + ")");
  }
  
  // Periodically check the filesystem space utilisation.
  
  public void run()
  {
    log("Monitoring disk space on " + filename);
    
    File fn = new File(filename);

    while(keep_running)
    {
      int pct_used = (int)((((float)fn.getTotalSpace() - (float)fn.getFreeSpace()) / (float)fn.getTotalSpace()) * 100.0);
      
      if(is_active(active_string) && pct_used > pct_threshold)
      {
        //string_processor("Filesystem " + filename + " usage of " + pct_used + "% exceeds limit of "+ pct_threshold + "%");
        string_processor("Filesystem " + filename + " usage exceeds limit of "+ pct_threshold + "%");
      }
      
      do_pause(60000);
    }
    
    log("Filesystem monitoring of " + filename + " stopped");
  }
}
