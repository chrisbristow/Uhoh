package com.uhoh;

public class BasicMatchFileSchnauzer extends FileSchnauzer
{
  String regex = "";
  
  // Construct a BasicMatchFileSchnauzer adding a regex expression.
  
  BasicMatchFileSchnauzer(String f, String a, String t, EventCollector ec, String r)
  {
    super(f, a, t, ec);
    regex = r;
    log("Matching lines containing \"" + r + "\" in file " + f + " (" + a + " / " + t + ")");
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
      event_collector.dispatch("SYSTEM%%" + tags + "%%" + s);
    }
  }
}
