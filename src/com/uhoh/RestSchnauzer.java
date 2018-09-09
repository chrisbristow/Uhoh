/*
        Licence
        -------
        Copyright (c) 2015-2018, Chris Bristow
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

import java.io.*;
import java.net.*;

/*
  The RestSchnauzer initialises a simple, single-threaded HTTP server
  within an Uhoh Client.  This server accepts HTTP GET requests in the format:

  /alert/<TAGS>/<MESSAGE>

  In receipt of such a request, the Client will forward an alert to the
  Server containing the given tags and message.

  This facility can be used to collect incoming webhooks from applications.
 */

public class RestSchnauzer extends Schnauzer
{
  int tcp_port;

  RestSchnauzer(EventCollector ec, int t)
  {
    event_collector = ec;
    tcp_port = t;
  }

  public void run()
  {
    ServerSocket ss = null;
    boolean ok = false;

    try
    {
      log("Starting a REST server on: " + tcp_port);
      ss = new ServerSocket(tcp_port);
      ok = true;
    }
    catch(Exception e)
    {
      log("Exception starting REST server:");
      e.printStackTrace();
    }

    if(ok)
    {
      while(true)
      {
        try
        {
          Socket worker_ss = ss.accept();

          InputStream in = worker_ss.getInputStream();
          int b = 0;
          StringBuffer sb = new StringBuffer();

          while((b = in.read()) != -1)
          {
            if(b != 10)
            {
              sb.append((char) b);
            }
            else
            {
              if(sb.toString().startsWith("GET"))
              {
                String url = sb.toString().split(" ")[1];

                log("Received REST request:" + url);

                OutputStream os = worker_ss.getOutputStream();
                String content = "{ \"status\": \"error\" }";

                if(url.startsWith("/alert"))
                {
                  String[] rest_items = url.split("/");

                  try
                  {
                    tags = rest_items[2];
                    string_processor(URLDecoder.decode(rest_items[3], "UTF-8"));
                    content = "{ \"status\": \"ok\" }";
                  }
                  catch(Exception ue)
                  {
                    log("Exception: REST URL parse failed:");
                    ue.printStackTrace();
                  }
                }
                else
                {
                  log("Error: URL not supported");
                }

                os.write("HTTP/1.1 200 OK\r\n".getBytes());
                os.write(("Content-length: " + content.length() + "\r\n").getBytes());
                os.write("\r\n".getBytes());
                os.write((content + "\r\n").getBytes());
              }

              sb.setLength(0);
            }
          }
        }
        catch(Exception e)
        {
          log("Exception: accept() failed:");
          e.printStackTrace();
        }
      }
    }
  }
}