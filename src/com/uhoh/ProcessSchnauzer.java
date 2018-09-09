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

import java.util.*;
import java.io.*;

/*
  The ProcessSchnauzer() uses a configurable process listing command to check
  whether specific process are running, and how many instances are running.

  Note that only one ProcessSchnauzer() ever runs in a Client.  This single instance
  performs all process checking from a list of processes to check.
 */

public class ProcessSchnauzer extends Schnauzer
{
  String ps_command;
  HashMap<String, Object[]> ps_hash;
  
  // Start a ProcessSchnauzer() thread.  The HashMap supplied contains the
  // list of processes to check.
  
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
            
            if(line.matches(".*" + mtch + ".*"))
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
              event_collector.dispatch("CLIENT%%" + tags + "%%Too few instances of " + mtch + " running", "PROCESS");
              log("Process: \"" + mtch + "\" Instances: " + ps_hash.get(mtch)[4] + " Min: " + ps_hash.get(mtch)[2] + " Max: " + ps_hash.get(mtch)[3]);
            }
            else if((Integer)ps_hash.get(mtch)[4] > (Integer)ps_hash.get(mtch)[3])
            {
              event_collector.dispatch("CLIENT%%" + tags + "%%Too many instances of " + mtch + " running", "PROCESS");
              log("Process: \"" + mtch + "\" Instances: " + ps_hash.get(mtch)[4] + " Min: " + ps_hash.get(mtch)[2] + " Max: " + ps_hash.get(mtch)[3]);
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
