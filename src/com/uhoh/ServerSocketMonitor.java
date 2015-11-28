// Listen for incoming UDP messages from Clients.

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
