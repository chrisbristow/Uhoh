package com.uhoh;

import java.net.*;
import java.io.*;
import java.util.*;

public class ServerSocketMonitor extends UhohBase implements Runnable
{
  DatagramSocket udp_socket = null;
  ServerLoop server_loop;

  ServerSocketMonitor(DatagramSocket u, ServerLoop s)
  {
    udp_socket = u;
    server_loop = s;
  }
  
  public void run()
  {
    log("Listening for incoming UDP messages");
    
    while(true)
    {
      byte[] buffer = new byte[65536];
      
      try
      {
        DatagramPacket pk = new DatagramPacket(buffer, 65536);
        udp_socket.receive(pk);
        String in_data = new String(pk.getData()).trim();
        InetSocketAddress from = (InetSocketAddress)pk.getSocketAddress();
        
        //log("UDP_REC: " + in_data);
        
        String[] cmd = in_data.split("%%");
        server_loop.client_q.put(new Object[]{ "CLIENT_UPD", cmd[1], from });
        
        if(cmd[0].equals("CONFREQ"))
        {
          log("Loading configuration for " + cmd[1]);
          
          StringBuffer sb = new StringBuffer("CONFIG%%");
          BufferedReader fb = new BufferedReader(new FileReader("clientconfigs/" + cmd[1]));
          String next_line;
          
          while((next_line = fb.readLine()) != null)
          {
            sb.append(next_line);
            sb.append("%%");
          }
          
          fb.close();

          byte[] config_reply = new String(sb.toString()).getBytes();
          DatagramPacket sp = new DatagramPacket(config_reply, config_reply.length, from.getAddress(), from.getPort());
          
          log("Sending config reply for " + cmd[1] + " to: " + from.getAddress().getHostAddress() + "/" + from.getPort());
          
          udp_socket.send(sp);
        }
        else
        {
          if(cmd[0].equals("ALERT"))
          {
            server_loop.client_q.put(cmd);
          }
        }
      }
      catch(Exception e)
      {
        log("Exception processing incoming UDP command:");
        e.printStackTrace();
      }
    }
  }
}
