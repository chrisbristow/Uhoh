package com.uhoh;

import java.net.*;
import java.util.*;

public class SocketEventCollector extends EventCollector
{
  DatagramSocket udp_socket = null;
  Hashtable<String, Long> servers = new Hashtable<String, Long>();
  long uid_c = 0;
  String our_name = null;
  
  SocketEventCollector(int udp_port)
  {
    super();
    
    try
    {
      log("Starting listener on UDP port " + udp_port);
      udp_socket = new DatagramSocket(udp_port);
      udp_socket.setReuseAddress(true);
      our_name = InetAddress.getLocalHost().getHostName();
    }
    catch(Exception e)
    {
      log("Fatal exception creating UDP socket:");
      e.printStackTrace();
      System.exit(1);
    }
    
    Thread smon = new Thread(new SocketMonitor(this));
    smon.start();
  }
  
  void process_event(String event)
  {
    if(event == null)
    {
      event = "SYSTEM%%NULL%%Idle";
    }

    for(Enumeration<String> e = servers.keys(); e.hasMoreElements();)
    {
      String server_info = e.nextElement();
      
      try
      {
        StringTokenizer st = new StringTokenizer(server_info, "/");
        String ip_addr = st.nextToken();
        int ip_port = Integer.parseInt(st.nextToken());
        long utime = (new Date()).getTime();
        String uid = utime + "_" + uid_c;
        uid_c ++;
        byte[] alert_cmd = ("ALERT%%" + our_name + "%%" + uid + "%%" + utime + "%%" + event).getBytes();
        DatagramPacket sp = new DatagramPacket(alert_cmd, alert_cmd.length, InetAddress.getByName(ip_addr), ip_port);

        log(server_info + " -> " + event);

        udp_socket.send(sp);
      }
      catch(Exception ex)
      {
        log("Exception sending event to " + server_info);
        ex.printStackTrace();
      }
    }
  }
}
