/*
        Licence
        -------
        Copyright (c) 2015-2017, Chris Bristow
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

/*
  The SocketMonitor() is used by the Client to process incoming UDP messages
  from Servers.  For example, Server notification broadcasts, configuration
  responses etc.
 */

public class SocketMonitor extends UhohBase implements Runnable
{
  SocketEventCollector event_collector = null;
  SchnauzerConfigurizer cfz = null;
  String server_ips = "";
  int udp_port;
  String cl_type = "";
  
  // Construct a SocketMonitor() by passing it a reference
  // to the EventCollector() as well as a list of IP addresses
  // of Servers (if the Client has been started with a preset list)
  // and the UDP port the Server is broadcasting on.
  
  SocketMonitor(SocketEventCollector ec, String s_ips, int u_port, String client_type)
  {
    event_collector = ec;
    server_ips = s_ips;
    udp_port = u_port;
    cl_type = client_type;
  }
  
  // Listen for incoming UDP messages and deal with them.  Messages
  // types are:
  // SRVHB%%<server_host> - Broadcast "advert" messages from servers.
  // CONFIG%%<config_string> - Response to a CONFREQ (config request).
  // RESET%% - Tells this Client to reset all monitoring configuration
  //           so that it will re-initialise itself by re-loading it's
  //           configuration.
  
  public void run()
  {

    log("Listening for incoming UDP messages");

    // If the Client has been started with the "servers=XXX" parameter set, then add the given
    // servers to the Client's server list and start a UnicastFetcher() thread.

    if(server_ips.length() > 0)
    {
      for(String svr : server_ips.split(","))
      {
        log("Unicast server: " + svr + "/" + (udp_port + 1));
        event_collector.servers.put(svr + "/" + (udp_port + 1), new Long(new Date().getTime()));
      }

      new Thread(new UnicastFetcher(event_collector, server_ips, udp_port, cl_type, this)).start();
    }
    
    while(true)
    {
      byte[] buffer = new byte[65536];
      
      try
      {
        DatagramPacket pk = new DatagramPacket(buffer, 65536);
        event_collector.udp_socket.receive(pk);
        String in_data = new String(pk.getData());

        // If "servers=XXX" was set, ignore SRVHB messages from Servers.

        if(in_data.startsWith("SRVHB%%") && server_ips.equals(""))
        {
          InetSocketAddress from = (InetSocketAddress)pk.getSocketAddress();
          String new_server_ip = from.getAddress().getHostAddress();
          int new_server_port = from.getPort();
          event_collector.servers.put(new_server_ip + "/" + new_server_port, new Long(new Date().getTime()));

          // Configuration requests are made each time a Client receives a Server heartbeat
          // until such time as the Client has received a Configuration Reply message.

          if(cfz == null)
          {
            String our_name = InetAddress.getLocalHost().getHostName();
            byte[] config_cmd = new String("CONFREQ%%" + our_name + "%%" + cl_type).getBytes();
            DatagramPacket sp = new DatagramPacket(config_cmd, config_cmd.length, from.getAddress(), new_server_port);
            
            log("Sending config request to: " + new_server_ip + "/" + new_server_port);
            
            event_collector.udp_socket.send(sp);
          }
        }
        else if(in_data.startsWith("CONFIG%%"))
        {
          // When the Server sends a configuration reply, the Client creates a
          // SchnauzerConfigurizer() object to manage the parsing of the
          // configuration and setting up of schnauzers.

          log("Configuration received");
          cfz = new SchnauzerConfigurizer(in_data, event_collector);
        }
        else if(in_data.startsWith("RESET%%"))
        {
          // A reset command resets the Client back to it's start-up state.

          log("Received a reset command");
          cfz.terminate_all();
          cfz = null;
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
