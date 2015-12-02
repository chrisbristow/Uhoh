/*
        Licence
        -------
        Copyright (c) 2015, Chris Bristow
        All rights reserved.

        Redistribution and use in source and binary forms, with or without
        modification, are permitted provided that the following conditions are met:

        1. Redistributions of source code must retain the above copyright notice, this
        list of conditions and the following disclaimer.
        2. Redistributions in binary form must reproduce the above copyright notice,
        this list of conditions and the following disclaimer in the documentation
        and/or other materials provided with the distribution.

        THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
        ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
        WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
        DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
        ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
        (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
        LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
        ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
        (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
        SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

        The views and conclusions contained in the software and documentation are those
        of the authors and should not be interpreted as representing official policies,
        either expressed or implied, of the FreeBSD Project.
*/

package com.uhoh;

import java.net.*;
import java.util.*;

public class SocketMonitor extends UhohBase implements Runnable
{
  SocketEventCollector event_collector = null;
  boolean is_configured = false;
  SchnauzerConfigurizer cfz = null;
  String server_ips = "";
  int udp_port;
  
  // Construct a SocketMonitor by passing it a reference
  // to the EventCollector.
  
  SocketMonitor(SocketEventCollector ec, String s_ips, int u_port)
  {
    event_collector = ec;
    server_ips = s_ips;
    udp_port = u_port;
  }
  
  // Listen for incoming UDP messages and deal with them.  Messages
  // are:
  // SRVHB%%<server_host> - Broadcast "advert" messages from servers.
  // CONFIG%%<config_string> - Response to a CONFREQ (config request).
  
  public void run()
  {
    unicast_client_init();

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
          unicast_client_init();
        }
      }
      catch(Exception e)
      {
        log("Exception processing incoming UDP data:");
        e.printStackTrace();
      }
    }
  }

  void unicast_client_init()
  {
    if(server_ips.length() > 0)
    {
      boolean send_cfg = true;

      for(String svr : server_ips.split(","))
      {
        log("Unicast server: " + svr + "/" + (udp_port + 1));

        event_collector.servers.put(svr + "/" + (udp_port + 1), new Long(new Date().getTime()));

        if(send_cfg)
        {
          try
          {
            String our_name = InetAddress.getLocalHost().getHostName();
            byte[] config_cmd = new String("CONFREQ%%" + our_name).getBytes();
            DatagramPacket sp = new DatagramPacket(config_cmd, config_cmd.length, InetAddress.getByName(svr), (udp_port + 1));

            log("Sending unicast config request to: " + svr + "/" + (udp_port + 1));

            event_collector.udp_socket.send(sp);
            send_cfg = false;
          }
          catch(Exception e)
          {
            log("Exception sending configuration request to " + svr);
            e.printStackTrace();
          }
        }
      }
    }
  }
}
