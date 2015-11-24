package com.uhoh;

import java.io.*;
import java.net.*;
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
