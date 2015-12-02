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

public class SocketEventCollector extends EventCollector
{
  DatagramSocket udp_socket = null;
  Hashtable<String, Long> servers = new Hashtable<String, Long>();
  long uid_c = 0;
  String our_name = null;
  
  SocketEventCollector(int udp_port, String s_ips)
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
    
    Thread smon = new Thread(new SocketMonitor(this, s_ips, udp_port));
    smon.start();
  }
  
  void process_event(Object[] event)
  {
    if(event == null)
    {
      event = new Object[]{ "SYSTEM%%NULL%%Idle", "IDLE" };
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
        uid_c ++;
        byte[] alert_cmd = ("ALERT%%" + our_name + "%%" + (String)event[1] + "%%" + utime + "%%" + (String)event[0]).getBytes();
        DatagramPacket sp = new DatagramPacket(alert_cmd, alert_cmd.length, InetAddress.getByName(ip_addr), ip_port);

        log(server_info + " -> (" + (String)event[1] + ") " + (String)event[0]);

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
