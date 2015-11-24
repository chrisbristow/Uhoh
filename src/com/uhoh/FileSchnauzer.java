// A FileSchnauzer is the base class for all file processors.
// FileSchnauzer implements the basic roll-proof file read and
// calls string_processor() for each line found.
// Sub-classes of FileSchnauzer implement specific variants
// of string_processor().

package com.uhoh;

import java.io.*;

public abstract class FileSchnauzer extends Schnauzer
{
  String filename;
  String active_string;
  boolean started = false;
  
  // Create a FileSchnauzer.  Args are:
  // - Filename to read from.
  // - Active time string (DAY;HH:MM-HH:MM[,...]).
  // - Tags.
  // - Reference of the EventCollector.
  
  FileSchnauzer(String f, String a, String t, EventCollector ec)
  {
    filename = f;
    active_string = a;
    tags = t;
    event_collector = ec;
  }
  
  // Read lines from the file and dispatch to the EventCollector via the
  // string_processor() method.  Also calls timed_event_processor()
  // when idle (used for timed metric counts).
  
  public void run()
  {
    while(keep_running)
    {
      RandomAccessFile in = null;
      
      try
      {
        in = new RandomAccessFile(filename, "r");
        
        if(!started)
        {
          in.seek(new File(filename).length());
          started = true;
        }
      }
      catch(Exception e)
      {
        in = null;
        do_pause(1000);
      }
      
      if(in != null)
      {
        boolean file_is_current = true;
        
        try
        {
          while(file_is_current && keep_running)
          {
            String n_line = null;
            long file_size = new File(filename).length();
            
            while((n_line = in.readLine()) != null)
            {
              if(is_active(active_string))
              {
                string_processor(n_line);
              }
            }
            
            timed_event_processor();
            
            do_pause(1000);
            
            if(new File(filename).length() < file_size)
            {
              file_is_current = false;
            }
          }
        
          in.close();
        }
        catch(Exception e)
        {
          log("An exception occurred reading file: " + filename);
          e.printStackTrace();
          do_pause(1000);
        }
      }
    }
    
    log("File reader for " + filename + " stopped");
  }
}
