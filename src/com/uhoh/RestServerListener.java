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
  The RestServerListener() object listens for incoming HTTP connections from the
  Web UI and spawns RestServerWorker() threads to handle serving the content.
 */

public class RestServerListener extends UhohBase implements Runnable
{
  ServerLoop server_loop;
  int tcp_port;

  RestServerListener(int t, ServerLoop s)
  {
    server_loop = s;
    tcp_port = t;
  }

  public void run()
  {
    ServerSocket ss = null;

    try
    {
      System.out.println("Starting a REST server on: " + tcp_port);
      ss = new ServerSocket(tcp_port);
    }
    catch(Exception e)
    {
      System.out.println("Exception starting REST server:");
      e.printStackTrace();
      System.exit(1);
    }

    while(true)
    {
      try
      {
        Socket worker_ss = ss.accept();

        Thread worker = new Thread(new RestServerWorker(worker_ss, server_loop));
        worker.start();
      }
      catch(Exception e)
      {
        System.out.println("Exception: accept() failed:");
        e.printStackTrace();
      }
    }
  }
}
