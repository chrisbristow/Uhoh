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

/*
  The UnicastFetcher() is started if a Client is run with the "servers=XXX" parameter.  This
  class is a thread which sends configuration requests to each server in the servers= list
  in turn until one of them returns a configuration reply.  UnicastFetcher() suppresses
  sending configuration requests by examining whether the SocketMonitor() reference it has
  been passed has a SchnauzerConfigurizer() set.
 */

public class UnicastFetcher extends UhohBase implements Runnable
{
  SocketEventCollector event_collector = null;
  String server_ips = "";
  int udp_port;
  String cl_type = "";
  SocketMonitor csm = null;

  UnicastFetcher(SocketEventCollector ec, String s_ips, int u_port, String client_type, SocketMonitor sm)
  {
    event_collector = ec;
    server_ips = s_ips;
    udp_port = u_port;
    cl_type = client_type;
    csm = sm;
  }

  public void run()
  {
    while(true)
    {
      for(String svr : server_ips.split(","))
      {
        if(csm.cfz == null)
        {
          try
          {
            String our_name = InetAddress.getLocalHost().getHostName();
            byte[] config_cmd = new String("CONFREQ%%" + our_name + "%%" + cl_type).getBytes();
            DatagramPacket sp = new DatagramPacket(config_cmd, config_cmd.length, InetAddress.getByName(svr), (udp_port + 1));

            log("Sending unicast config request to: " + svr + "/" + (udp_port + 1));

            event_collector.udp_socket.send(sp);
          }
          catch(Exception e)
          {
            log("Exception sending configuration request to " + svr);
            e.printStackTrace();
          }
        }

        do_pause(5000);
      }
    }
  }
}
