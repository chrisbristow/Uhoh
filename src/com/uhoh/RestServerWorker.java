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
              String y_axis = "Volume";

              if(rest_items.length == 5)
              {
                y_axis = rest_items[4].replaceAll("%20", " ");
              }

              try
              {
                BufferedReader rr = new BufferedReader(new FileReader("web/metric.html"));

                while((next_line = rr.readLine()) != null)
                {
                  wb.append(next_line.replaceFirst("_URL_", rest_items[2] + "/" + rest_items[3]).replaceFirst("_METRIC_", y_axis));
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
            else if(url.startsWith("/metrics"))
            {
              // Serve the last metric view for Prometheus consumption.

              LinkedBlockingQueue<String> tmp_q = new LinkedBlockingQueue<String>();
              server_loop.client_q.put(new Object[]{ "PROM_REQ", tmp_q });
              String prom_doc = tmp_q.poll(rest_request_timeout, TimeUnit.MILLISECONDS);

              if(prom_doc != null)
              {
                content = prom_doc;
              }
              else
              {
                content = "";
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
            else if(url.startsWith("/uhoh.css"))
            {
              // Serve the Uhoh UI CSS file.

              StringBuffer wb = new StringBuffer();
              String next_line;

              try
              {
                BufferedReader rr = new BufferedReader(new FileReader("web/uhoh.css"));

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
                System.out.println("Exception loading uhoh.css:");
                fnf.printStackTrace();
              }
            }
            else if(url.startsWith("/sview/"))
            {
              // Serve the service view configuration pages.

              String[] rest_items = url.replaceFirst("\\?.*$", "").split("/");

              content = service_to_json("serviceviews/" + rest_items[2]);
            }
            else if(url.startsWith("/dashboard/"))
            {
              // Serve a dashboard view.

              String[] rest_items = url.replaceFirst("\\?.*$", "").split("/");

              content = dashboard_to_html("dashboards/" + rest_items[2], rest_items[2]);
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

                wb.append("{\r\n  \"status\": \"ok\",\r\n  \"items\":\r\n  [\r\n");

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

  // service_to_json() loads a Service Map View definition file and converts
  // it into JSON format ready for dispatch to the Service Map View Javascript
  // user interface.

  String service_to_json(String f)
  {
    String r = "";

    try
    {
      StringBuffer wb = new StringBuffer("{ \"status\": \"ok\", \"layers\": [ { ");
      String next_line;
      BufferedReader rr = new BufferedReader(new FileReader(f));
      int layer_count = 0;
      int element_count = 0;

      while((next_line = rr.readLine()) != null)
      {
        next_line = next_line.trim();
        HashMap<String, String> args = get_kvps(next_line);

        if(next_line.startsWith("element:"))
        {
          String tag = args.get("tag");
          String name = next_line.replaceFirst("^\\s*element:\\s+.+name=", "");

          if(tag != null && next_line.contains("name="))
          {
            if(element_count > 0)
            {
              wb.append(",");
            }

            wb.append("{ \"tag\": \"" + tag + "\", \"name\": \"" + name + "\" } ");
            element_count++;
          }
        }
        else if(next_line.startsWith("layer:"))
        {
          String name = next_line.replaceFirst("^\\s*layer:\\s+.+name=", "");

          if(next_line.contains("name="))
          {
            if(name.equals("NONE"))
            {
              name = "";
            }

            if(layer_count > 0)
            {
              wb.append("] }, { ");
            }

            wb.append("\"layer\": \"" + name + "\", \"elements\": [ ");
            layer_count++;
            element_count = 0;
          }
        }
      }

      rr.close();

      wb.append("] } ] }");

      r = wb.toString();
    }
    catch(Exception e)
    {
      System.out.println("Exception loading service view file: " + f);
      e.printStackTrace();
      r = "{ \"status\": \"error\" }";
    }

    return(r);
  }

  // dashboard_to_html() loads a Dashboard View definition file and converts
  // it into HTML format ready for dispatch to the browser.

  String dashboard_to_html(String f, String db)
  {
    String r = "";

    try
    {
      String next_line;
      String colour = "#262626";
      RandomAccessFile rr = new RandomAccessFile(f, "r");

      while((next_line = rr.readLine()) != null)
      {
        next_line = next_line.trim();
        HashMap<String, String> args = get_kvps(next_line);

        if(next_line.startsWith("background:"))
        {
          colour = args.get("colour");
        }
      }

      rr.seek(0);

      StringBuffer wb = new StringBuffer("<html>\r\n  <head>\r\n    <title>" + db + "</title>\r\n    <link href='https://fonts.googleapis.com/css?family=Roboto+Slab' rel='stylesheet' type='text/css'>\r\n  </head>\r\n  <body bgcolor='" + colour + "'>\r\n");

      while((next_line = rr.readLine()) != null)
      {
        next_line = next_line.trim();
        HashMap<String, String> args = get_kvps(next_line);

        if(next_line.startsWith("row:"))
        {
          String size = args.get("size");
          String url = args.get("url");
          String url2 = args.get("url2");

          if(size != null && url != null)
          {
            if(url2 == null)
            {
              wb.append("    <nobr>\r\n      <iframe src=\"" + url + "\" width=\"100%\" height=\"" + size + "\" scrolling=\"no\" frameborder=\"0\"></iframe>\r\n    </nobr>\r\n");
            }
            else
            {
              wb.append("    <nobr>\r\n      <iframe src=\"" + url + "\" width=\"50%\" height=\"" + size + "\" scrolling=\"no\" frameborder=\"0\"></iframe>\r\n      <iframe src=\"" + url2 + "\" width=\"50%\" height=\"" + size + "\" scrolling=\"no\" frameborder=\"0\"></iframe>\r\n    </nobr>\r\n");
            }
          }
        }
        else if(next_line.startsWith("title:"))
        {
          if(next_line.contains("name="))
          {
            String name = next_line.replaceFirst("^\\s*title:\\s+name=", "");

            wb.append("    <center style=\"font: bold 12pt 'Roboto Slab', arial, sans-serif; color: #ffffff\">" + name + "</center>\r\n");
          }
        }
      }

      rr.close();

      wb.append("  </body>\r\n</html>");

      r = wb.toString();
    }
    catch(Exception e)
    {
      System.out.println("Exception loading service view file: " + f);
      e.printStackTrace();
      r = "<html>Dashboard " + db + " not found</html>";
    }

    return(r);
  }
}
