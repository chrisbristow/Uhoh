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
import java.io.*;

/*
  A ServerSocketMonitor() object is created to handle incoming UDP
  traffic from Clients - ie:
  - Alerts.
  - Configuration requests.
 */

public class ServerSocketMonitor extends UhohBase implements Runnable
{
  DatagramSocket udp_socket = null;
  ServerLoop server_loop;

  // The ServerLoop() object creates the UDP socket for the Server and the socket
  // reference is passed to the ServerSocketMonitor() for use.  A reference to
  // the ServerLoop() for alert dispatch is also passed to ServerSocketMonitor().

  ServerSocketMonitor(DatagramSocket u, ServerLoop s)
  {
    udp_socket = u;
    server_loop = s;
  }

  // This is the loop which handles incoming UDP traffic.
  
  public void run()
  {
    System.out.println("Listening for incoming UDP messages");
    
    while(true)
    {
      byte[] buffer = new byte[65536];
      
      try
      {
        DatagramPacket pk = new DatagramPacket(buffer, 65536);
        udp_socket.receive(pk);
        String in_data = new String(pk.getData()).trim();
        InetSocketAddress from = (InetSocketAddress)pk.getSocketAddress();

        String[] cmd = in_data.split("%%");
        server_loop.client_q.put(new Object[]{ "CLIENT_UPD", cmd[1], from });
        
        if(cmd[0].equals("CONFREQ"))
        {
          String config_file_name = cmd[1];
          System.out.println("Loading configuration for " + config_file_name);

          if(cmd.length == 3)
          {
            config_file_name = cmd[2];
            System.out.println("Configuration override: " + config_file_name);
          }
          
          StringBuffer sb = new StringBuffer("CONFIG%%");
          File config_file = new File("clientconfigs/" + config_file_name);

          if(config_file.exists())
          {
            BufferedReader fb = new BufferedReader(new FileReader(config_file));
            String next_line;

            while((next_line = fb.readLine()) != null)
            {
              sb.append(next_line);
              sb.append("%%");
            }

            fb.close();

            byte[] config_reply = new String(sb.toString()).getBytes();
            DatagramPacket sp = new DatagramPacket(config_reply, config_reply.length, from.getAddress(), from.getPort());

            System.out.println("Sending config reply for " + cmd[1] + " to: " + from.getAddress().getHostAddress() + "/" + from.getPort());

            udp_socket.send(sp);
          }
          else
          {
            System.out.println("Warning: A client configuration file for " + config_file_name + " doesn't exist");
            server_loop.client_q.put(new Object[]{"ALERT", cmd[1], "IDLE", new Long(System.currentTimeMillis()), "SERVER", server_loop.no_config_tags, "No configuration available for " + config_file_name});
          }
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
        System.out.println("Exception processing incoming UDP command:");
        e.printStackTrace();
      }
    }
  }
}
