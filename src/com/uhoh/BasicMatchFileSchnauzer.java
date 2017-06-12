/*
        Licence
        -------
        Copyright (c) 20165-2017, Chris Bristow
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

/*
  A BasicMatchFileSchnauzer() implements a usable file reader and processor.
 */

public class BasicMatchFileSchnauzer extends FileSchnauzer
{
  String regex = "";
  String message = "";
  String tx = "";

  // Construct a BasicMatchFileSchnauzer() adding a regex expression, translator string and custome
  // alert message to the parameters used to construct a FileSchnauzer().
  
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
      log(" - Capture using: " + tx);
    }
  }
  
  // The is_matching() method checks whether a line in a file matches the regex for this
  // BasicMatchFileSchnauzer().  Note that the ".*" prefix and suffix ensures that the
  // configuration can specify only a partial string match (similar to Perl regexes).
  
  boolean is_matching(String s)
  {
    boolean ok = false;
    
    if(s.matches(".*" + regex + ".*"))
    {
      ok = true;
    }
    
    return(ok);
  }
  
  // The string_processor() implementation in a BasicMatchFileSchnauzer():
  // - Checks that a file line matches the regex.
  // - Translates the file line, if necessary.
  // - Decides if to log the actual file line or use the custom alert message in the alert dispatched.
  
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
