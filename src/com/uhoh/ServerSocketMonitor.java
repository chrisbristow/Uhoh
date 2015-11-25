package com.uhoh;

import java.net.*;
import java.io.*;
import java.util.*;

public class ServerSocketMonitor extends UhohBase implements Runnable
{
  DatagramSocket udp_socket = null;
  ServerLoop server_loop;
  FileOutputStream logmgr = null;
  
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
            HashSet<String> tags = new HashSet<String>(Arrays.asList(cmd[5].split(",")));

            if(tags.contains("RED"))
            {
              server_loop.ui_rec.put(new Object[]{ new Long(System.currentTimeMillis()), cmd[1], cmd[6], "RED", cmd[2] });
            }
            else if(tags.contains("AMBER"))
            {
              server_loop.ui_rec.put(new Object[]{new Long(System.currentTimeMillis()), cmd[1], cmd[6], "AMBER", cmd[2] });
            }
            else if(tags.contains("GREEN"))
            {
              server_loop.ui_rec.put(new Object[]{new Long(System.currentTimeMillis()), cmd[1], cmd[6], "GREEN", cmd[2] });
            }
          }

          String log_line = log(in_data);
          disk_log(log_line);
        }
      }
      catch(Exception e)
      {
        log("Exception processing incoming UDP command:");
        e.printStackTrace();
      }
    }
  }

  void disk_log(String s)
  {
    try
    {
      if(logmgr == null)
      {
        logmgr = new FileOutputStream(server_disk_log_name, true);
      }

      logmgr.write((s + "\n").getBytes());

      if(logmgr.getChannel().size() > server_disk_log_size)
      {
        logmgr.close();
        logmgr = null;
        (new File(server_disk_log_name)).renameTo(new File(server_disk_log_name + ".1"));
        (new File(server_disk_log_name)).createNewFile();
      }
    }
    catch(Exception e)
    {
      log("Exception logging to disk:");
      e.printStackTrace();
    }
  }
}
