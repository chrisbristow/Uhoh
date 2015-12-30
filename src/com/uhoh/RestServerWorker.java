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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

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
          //log("HTTP_REC: " + sb.toString());

          if(sb.toString().startsWith("GET"))
          {
            String url = sb.toString().split(" ")[1];

            log("Serving REST request for " + url);

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
            else if(url.equals("/"))
            {
              StringBuffer wb = new StringBuffer();
              String next_line;
              BufferedReader rr = new BufferedReader(new FileReader("web/ui.html"));

              while((next_line = rr.readLine()) != null)
              {
                wb.append(next_line);
                wb.append("\r\n");
              }

              rr.close();

              content = wb.toString();
            }
            else if(url.startsWith("/metric/"))
            {
              String[] rest_items = url.replaceFirst("\\?.*$", "").split("/");
              StringBuffer wb = new StringBuffer();
              String next_line;
              BufferedReader rr = new BufferedReader(new FileReader("web/metric.html"));

              while((next_line = rr.readLine()) != null)
              {
                wb.append(next_line.replaceFirst("_URL_", rest_items[2] + "/" + rest_items[3]));
                wb.append("\r\n");
              }

              rr.close();

              content = wb.toString();
            }
            else if(url.startsWith("/mdata/"))
            {
              String[] rest_items = url.split("/");
              StringBuffer wb = new StringBuffer();
              String next_line;
              String dir_name = rest_items[2];

              if(dir_name.equals("TODAY"))
              {
                GregorianCalendar gc = new GregorianCalendar();
                dir_name = String.format("%04d-%02d-%02d", gc.get(Calendar.YEAR), gc.get(Calendar.MONTH) + 1, gc.get(Calendar.DAY_OF_MONTH));
              }

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
      log("Exception handling REST request:");
      e.printStackTrace();
    }
  }
}
