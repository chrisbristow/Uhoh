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

import java.io.*;

/*
  A FileSchnauzer() is the base class for all file processing schnauzers.
  FileSchnauzer() implements the basic roll-proof file read and
  calls string_processor() for each line found.
  Sub-classes of FileSchnauzer() implement specific variants
  of string_processor() so FileSchnauzer() isn't ever used as-is.
  */

public abstract class FileSchnauzer extends Schnauzer
{
  String filename;
  String active_string;
  boolean started = false;
  
  // Create a FileSchnauzer.  Args are:
  // - Filename to read from.
  // - Active time string (DAY;HH:MM-HH:MM[,...]).
  // - Tags.
  // - Reference of the EventCollector().
  
  FileSchnauzer(String f, String a, String t, EventCollector ec)
  {
    filename = f;
    active_string = a;
    tags = t;
    event_collector = ec;
  }
  
  // Read lines from the file and dispatch to the EventCollector() via the
  // string_processor() method.  Also calls timed_event_processor()
  // when idle (used for timed metric counts).
  //
  // The "keep_running" flag is used to close down a FileSchnauzer() if a Client's
  // configuration has changed.
  
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
