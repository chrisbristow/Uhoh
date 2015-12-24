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

            if(line.trim().matches(".*" + regex + ".*"))
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
