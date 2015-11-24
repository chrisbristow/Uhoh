package com.uhoh;

import java.net.*;
import java.util.*;

public class SocketMonitor extends UhohBase implements Runnable
{
  SocketEventCollector event_collector = null;
  boolean is_configured = false;
  SchnauzerConfigurizer cfz = null;
  
  // Construct a SocketMonitor by passing it a reference
  // to the EventCollector.
  
  SocketMonitor(SocketEventCollector ec)
  {
    event_collector = ec;
  }
  
  // Listen for incoming UDP messages and deal with them.  Messages
  // are:
  // SRVHB%%<server_host> - Broadcast "advert" messages from servers.
  // CONFIG%%<config_string> - Response to a CONFREQ (config request).
  
  public void run()
  {
    log("Listening for incoming UDP messages");
    
    while(true)
    {
      byte[] buffer = new byte[65536];
      
      try
      {
        DatagramPacket pk = new DatagramPacket(buffer, 65536);
        event_collector.udp_socket.receive(pk);
        String in_data = new String(pk.getData());
        
        //System.out.println("DBG: UDP_REC: " + in_data);
        
        if(in_data.startsWith("SRVHB%%"))
        {
          InetSocketAddress from = (InetSocketAddress)pk.getSocketAddress();
          String new_server_ip = from.getAddress().getHostAddress();
          int new_server_port = from.getPort();
          event_collector.servers.put(new_server_ip + "/" + new_server_port, new Long(new Date().getTime()));
          
          if(!is_configured)
          {
            String our_name = InetAddress.getLocalHost().getHostName();
            byte[] config_cmd = new String("CONFREQ%%" + our_name).getBytes();
            DatagramPacket sp = new DatagramPacket(config_cmd, config_cmd.length, from.getAddress(), new_server_port);
            
            log("Sending config request to: " + new_server_ip + "/" + new_server_port);
            
            event_collector.udp_socket.send(sp);
          }
        }
        else if(in_data.startsWith("CONFIG%%"))
        {
          log("Configuration received");
          cfz = new SchnauzerConfigurizer(in_data, event_collector);
          is_configured = true;
        }
        else if(in_data.startsWith("RESET%%"))
        {
          log("Received a reset command");
          cfz.terminate_all();
          cfz = null;
          is_configured = false;
        }
      }
      catch(Exception e)
      {
        log("Exception processing incoming UDP data:");
        e.printStackTrace();
      }
    }
  }
}
