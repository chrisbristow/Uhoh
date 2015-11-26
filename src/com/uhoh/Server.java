package com.uhoh;

public class Server
{
  // Launch a standalone Uhoh server.
  
  public static void main(String[] args)
  {
    try
    {
      new ServerLoop(args[0]).run();
    }
    catch(Exception e)
    {
      System.err.println("Usage: com.uhoh.Server <server_properties_file>");
      System.exit(1);
    }
  }
}
