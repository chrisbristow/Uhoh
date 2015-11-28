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
        string_processor("Filesystem " + filename + " usage exceeds limit of "+ pct_threshold + "%");
      }
      
      do_pause(60000);
    }
    
    log("Filesystem monitoring of " + filename + " stopped");
  }
}
