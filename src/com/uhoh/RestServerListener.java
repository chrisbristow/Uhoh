package com.uhoh;

import java.net.*;

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
      log("Starting a REST server on: " + tcp_port);
      ss = new ServerSocket(tcp_port);
    }
    catch(Exception e)
    {
      log("Exception starting REST server:");
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
        log("Exception: accept() failed:");
        e.printStackTrace();
      }
    }
  }
}
