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

public class BasicMatchFileSchnauzer extends FileSchnauzer
{
  String regex = "";
  String message = "";
  String tx = "";
  
  // Construct a BasicMatchFileSchnauzer adding a regex expression.
  
  BasicMatchFileSchnauzer(String f, String a, String t, EventCollector ec, String r, String msg, String txlate)
  {
    super(f, a, t, ec);
    regex = r;
    message = msg;
    tx = txlate;

    if(regex == null)
    {
      regex = tx;
    }

    log("Matching lines containing \"" + regex + "\" in file " + f + " (" + a + " / " + t + ")");

    if(message.length() != 0)
    {
      log(" - Message: " + message);
    }

    if(tx.length() != 0)
    {
      log(" - Translate using: " + tx);
    }
  }
  
  // Check to see if a string matches the regex.
  
  boolean is_matching(String s)
  {
    boolean ok = false;
    
    if(s.matches(".*" + regex + ".*"))
    {
      ok = true;
    }
    
    return(ok);
  }

  // Translate an input string extacting specified parts.

  String translate_string(String input, String rx)
  {
    String output = input;

    if(!rx.equals(""))
    {
      try
      {
        output = input.replaceFirst(rx, "$1");

        log("Translated " + input + " to " + output);
      }
      catch(Exception e)
      {
        log("Translation failed from " + input + " to " + output);
      }
    }

    return(output);
  }
  
  // Override string_processor() to add the regex match.
  
  public void string_processor(String s)
  {
    if(is_matching(s))
    {
      if(message.length() == 0)
      {
        event_collector.dispatch("CLIENT%%" + tags + "%%" + filename + ": " + translate_string(s, tx), "FILE");
      }
      else
      {
        event_collector.dispatch("CLIENT%%" + tags + "%%" + filename + ": " + message, "FILE");
      }
    }
  }
}
