/*
        Licence
        -------
        Copyright (c) 2016, Chris Bristow
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

/*
  The Client class is a static class used to launch a Client.  It's main() method
  expects either a single argument - the UDP port number on which to listen for broadcasts
  from Servers, or, in addition the the UDP port, a second argument - a comma-separated
  list of IP addresses where Servers are located.
  The optional second argument is needed in environments where UDP broadcast isn't
  supported (for example Amazon Web Services).

  Here's the start-up sequence for a Client:
  - The Client() creates a SocketEventCollector() object.
  - The SocketEventCollector() object spawns a SocketMonitor() thread.
  - The SocketMonitor() thread listens for Server heartbeat UDP messages.
  - The SocketMonitor() thread sends configuration request to one of the Servers identified.
  - The SocketMonitor() thread creates a SchnauzerConfigurizer() object once it receives
    a configuration reply from the Server.
  - The SchnauzerConfigurizer() object spawns Schnauzer() threads for each item to
    be monitored.
 */

public class Client
{
  public static void main(String[] args)
  {
    try
    {
      String server_ips = "";

      if(args.length == 2)
      {
        server_ips = args[1];
      }

      new SocketEventCollector(Integer.parseInt(args[0]), server_ips).run();
    }
    catch(Exception e)
    {
      System.err.println("Usage: com.uhoh.Client <udp_port_number> [<server_ip_address(es)>]");
      System.exit(1);
    }
  }
}