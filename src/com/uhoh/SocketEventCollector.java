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
  This class is derived from EventCollector() and adds UDP socket handling.
 */

public class SocketEventCollector extends EventCollector
{
  DatagramSocket udp_socket = null;
  Hashtable<String, Long> servers = new Hashtable<String, Long>();
  long uid_c = 0;
  String our_name = null;

  SocketEventCollector(int udp_port, String s_ips, String client_type)
  {
    super();

    // A UDP socket is created for sending alerts to Servers.  Note that if we
    // can't create this socket, then an Uhoh Client can't operate, so if
    // socket creation fails, we close down the Uhoh Client.

    try
    {
      log("Listening for messages from Servers on UDP port " + udp_port);
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

    // Another thread, SocketMonitor(), is started to collect UDP messages from Servers.

    Thread smon = new Thread(new SocketMonitor(this, s_ips, udp_port, client_type));
    smon.start();
  }

  // The process_event() method from EventCollector() is overridden to allow it to forward
  // alerts from the LinkedBlockingQueue() to all known servers.

  void process_event(Object[] event)
  {
    // If the event in the LinkedBlockingQueue() is a null object, then create an "Idle"
    // alert.  This is used by a Client to indicate to the Server that the Client
    // hasn't collected any alerts recently - ie. a heartbeat to prevent the Server from
    // thinking that a Client has died.

    if(event == null)
    {
      event = new Object[]{ "SYSTEM%%NULL%%Idle", "IDLE" };
    }

    // Loop through the list of all known Servers and send the alert to them as a UDP message.

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
        byte[] alert_cmd = ("ALERT%%" + our_name + "%%" + event[1] + "%%" + utime + "%%" + event[0]).getBytes();
        DatagramPacket sp = new DatagramPacket(alert_cmd, alert_cmd.length, InetAddress.getByName(ip_addr), ip_port);

        log(server_info + " -> (" + event[1] + ") " + event[0]);

        udp_socket.send(sp);

        // Here, we check for alerts which may trigger a derived alert by requesting that
        // all "multi" (ie. derived) groups are checked to see if they have registered
        // qualifying alerts within their time windows.

        String alert_tags = ((String)event[0]).split("%%")[1];

        for(int x = 0; x < multi_list.size(); x ++)
        {
          multi_list.get(x).check_for_complete(alert_tags);
        }
      }
      catch(Exception ex)
      {
        log("Exception sending event to " + server_info);
        ex.printStackTrace();
      }
    }
  }
}
