package com.uhoh;

public class BasicMatchFileSchnauzer extends FileSchnauzer
{
  String regex = "";
  String message = "";
  
  // Construct a BasicMatchFileSchnauzer adding a regex expression.
  
  BasicMatchFileSchnauzer(String f, String a, String t, EventCollector ec, String r, String msg)
  {
    super(f, a, t, ec);
    regex = r;
    message = msg;
    log("Matching lines containing \"" + r + "\" in file " + f + " (" + a + " / " + t + ")");

    if(message.length() != 0)
    {
      log(" - Message: " + message);
    }
  }
  
  // Check to see if a string matches the regex.
  
  boolean is_matching(String s)
  {
    boolean ok = false;
    
    if(s.matches(regex))
    {
      ok = true;
    }
    
    return(ok);
  }
  
  // Override string_processor() to add the regex match.
  
  public void string_processor(String s)
  {
    if(is_matching(s))
    {
      if(message.length() == 0)
      {
        event_collector.dispatch("SYSTEM%%" + tags + "%%" + filename + ": " + s);
      }
      else
      {
        event_collector.dispatch("SYSTEM%%" + tags + "%%" + filename + ": " + message);
      }
    }
  }
}
