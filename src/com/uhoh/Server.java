package com.uhoh;

public class Server
{
  // Launch a standalone Uhoh server.
  
  public static void main(String[] args)
  {
    try
    {
      new ServerLoop(Integer.parseInt(args[0]), args[1], Integer.parseInt(args[2])).run();
    }
    catch(Exception e)
    {
      System.err.println("Usage: com.uhoh.Server <udp_port_number> <broadcast_address> <web_tcp_port>");
      System.exit(1);
    }
  }
}
