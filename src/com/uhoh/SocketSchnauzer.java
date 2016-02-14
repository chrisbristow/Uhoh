package com.uhoh;

import java.net.*;

public class SocketSchnauzer extends Schnauzer
{
  String ip;
  int port;
  String active_string;
  long seconds;
  int timeout;
  String message;

  SocketSchnauzer(String a_ip, int a_port, String a_active, String a_tags, EventCollector a_event_collector, long a_seconds, int a_timeout, String a_message)
  {
    ip = a_ip;
    port = a_port;
    active_string = a_active;
    tags = a_tags;
    event_collector = a_event_collector;
    seconds = a_seconds;
    timeout = a_timeout;
    message = a_message;
  }

  public void run()
  {
    log("Socket check: " + ip + "/" + port + " starting");

    while(keep_running)
    {
      try
      {
        if(is_active(active_string))
        {
          Socket s = new Socket();
          s.connect(new InetSocketAddress(ip, port), timeout);
        }
      }
      catch(Exception e)
      {
        string_processor(message);
      }

      do_pause(seconds);
    }

    log("Socket check: " + ip + "/" + port + " stopping");
  }
}
