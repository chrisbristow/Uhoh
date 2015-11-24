package com.uhoh;

public class Client
{
  // Launch a standalone Uhoh client.
  
  public static void main(String[] args)
  {
    try
    {
      Thread ec = new Thread(new SocketEventCollector(Integer.parseInt(args[0])));
      ec.start();
    }
    catch(Exception e)
    {
      System.err.println("Usage: com.uhoh.Client <udp_port_number>");
      System.exit(1);
    }
  }
}
