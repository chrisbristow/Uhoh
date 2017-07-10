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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/*
  The RestServerWorker() class is used to create threads which handle incoming
  HTTP requests from the Web UI.  These requests are:
  - To serve static content (ie. when a user first loads the Web UI).
  - To serve REST requests containing currently active alerts.
  - To serve REST requests containing metric information.
  RestServerWorker() threads terminate once the Web UI client (browser)
  requesting content has closed its HTTP connection.
 */

public class RestServerWorker extends UhohBase implements Runnable
{
  ServerLoop server_loop;
  Socket ss;

  RestServerWorker(Socket s, ServerLoop sl)
  {
    ss = s;
    server_loop = sl;
  }

  public void run()
  {
    try
    {
      InputStream in = ss.getInputStream();
      int b = 0;
      StringBuffer sb = new StringBuffer();

      while((b = in.read()) != -1)
      {
        if(b != 10)
        {
          sb.append((char)b);
        }
        else
        {
          if(sb.toString().startsWith("GET"))
          {
            // Server REST requests for active alert data.

            String url = sb.toString().split(" ")[1];

            System.out.println("Serving REST request for " + url);

            OutputStream os = ss.getOutputStream();
            String content = "{ \"status\": \"error\" }";

            if(url.startsWith("/ui"))
            {
              LinkedBlockingQueue<String> tmp_q = new LinkedBlockingQueue<String>();
              server_loop.client_q.put(new Object[]{ "REST_REQ", tmp_q });
              String json = tmp_q.poll(rest_request_timeout, TimeUnit.MILLISECONDS);

              if(json != null)
              {
                content = json;
              }
            }
            else if(url.startsWith("/metric/"))
            {
              // Serve the metrics Web UI static container page.

              String[] rest_items = url.replaceFirst("\\?.*$", "").split("/");
              StringBuffer wb = new StringBuffer();
              String next_line;

              try
              {
                BufferedReader rr = new BufferedReader(new FileReader("web/metric.html"));

                while((next_line = rr.readLine()) != null)
                {
                  wb.append(next_line.replaceFirst("_URL_", rest_items[2] + "/" + rest_items[3]));
                  wb.append("\r\n");
                }

                rr.close();

                content = wb.toString();
              }
              catch(Exception fnf)
              {
                System.out.println("Exception loading performance management view:");
                fnf.printStackTrace();
              }
            }
            else if(url.startsWith("/service/"))
            {
              // Serve the service view Web UI static container page.

              String[] rest_items = url.replaceFirst("\\?.*$", "").split("/");
              StringBuffer wb = new StringBuffer();
              String next_line;

              try
              {
                BufferedReader rr = new BufferedReader(new FileReader("web/service.html"));

                while((next_line = rr.readLine()) != null)
                {
                  wb.append(next_line.replaceFirst("_URL_", rest_items[2]));
                  wb.append("\r\n");
                }

                rr.close();

                content = wb.toString();
              }
              catch(Exception fnf)
              {
                System.out.println("Exception loading service view:");
                fnf.printStackTrace();
              }
            }
            else if(url.startsWith("/sview/"))
            {
              // Serve the service view configuration pages.

              String[] rest_items = url.replaceFirst("\\?.*$", "").split("/");
              StringBuffer wb = new StringBuffer();
              String next_line;

              try
              {
                BufferedReader rr = new BufferedReader(new FileReader("serviceviews/" + rest_items[2]));

                while((next_line = rr.readLine()) != null)
                {
                  wb.append(next_line);
                  wb.append("\r\n");
                }

                rr.close();

                content = wb.toString();
              }
              catch(Exception fnf)
              {
                System.out.println("Exception loading service view:");
                fnf.printStackTrace();
              }
            }
            else if(url.startsWith("/mdata/"))
            {
              // Serve REST requests containing metric data.

              String[] rest_items = url.split("/");
              StringBuffer wb = new StringBuffer();
              String next_line;
              String dir_name = rest_items[2];

              if(dir_name.equals("TODAY"))
              {
                GregorianCalendar gc = new GregorianCalendar();
                dir_name = String.format("%04d-%02d-%02d", gc.get(Calendar.YEAR), gc.get(Calendar.MONTH) + 1, gc.get(Calendar.DAY_OF_MONTH));
              }

              try
              {
                BufferedReader rr = new BufferedReader(new FileReader("metrics/" + dir_name + "/" + rest_items[3]));
                String pfx = "";

                wb.append("{\r\n  \"items\":\r\n  [\r\n");

                while((next_line = rr.readLine()) != null)
                {
                  wb.append(pfx + "    [ " + next_line + " ]");
                  pfx = ",\r\n";
                }

                wb.append("\r\n  ]\r\n}");

                rr.close();

                content = wb.toString();
              }
              catch(Exception fnf)
              {
                System.out.println("Exception loading performance management data:");
                fnf.printStackTrace();
              }
            }
            else
            {
              // Serve the alerts Web UI static container page.

              StringBuffer wb = new StringBuffer();
              String next_line;

              try
              {
                BufferedReader rr = new BufferedReader(new FileReader("web/ui.html"));

                while((next_line = rr.readLine()) != null)
                {
                  wb.append(next_line);
                  wb.append("\r\n");
                }

                rr.close();

                content = wb.toString();
              }
              catch(Exception fnf)
              {
                System.out.println("Exception loading fault management view:");
                fnf.printStackTrace();
              }
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
      System.out.println("Exception handling REST request:");
      e.printStackTrace();
    }
  }
}
