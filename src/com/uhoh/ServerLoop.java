package com.uhoh;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;

// A ServerLoop is called to create an Uhoh server.

public class ServerLoop extends UhohBase
{
  int udp_port;
  int tcp_port;
  String broadcast_address;
  DatagramSocket udp_socket = null;
  String our_name = null;
  HashMap<String, Object[]> clients = new HashMap<String, Object[]>();
  LinkedBlockingQueue<Object[]> client_q = new LinkedBlockingQueue<Object[]>();
  LinkedBlockingQueue<Object[]> ui_rec = new LinkedBlockingQueue<Object[]>();
  HashMap<String, Object[]> ui_red = new HashMap<String, Object[]>();
  HashMap<String, Object[]> ui_amber = new HashMap<String, Object[]>();
  HashMap<String, Object[]> ui_green = new HashMap<String, Object[]>();
  HashMap<String, Long> ui_rtime = new HashMap<String, Long>();

  // Initialise a new ServerLoop().
  
  ServerLoop(int u, String b, int t)
  {
    udp_port = u;
    broadcast_address = b;
    tcp_port = t;

    ui_rtime.put("ALL", 125000L);
    ui_rtime.put("PROCESS", 65000L);
    ui_rtime.put("FILE", 600000L);
    ui_rtime.put("IDLE", 75000L);
  }

  // Start and run the ServerLoop.  Note that this class isn't to be used as
  // a Thread - the run() method never returns to the caller (ie. runs in the
  // same Thread as the caller).
  //
  // If we can't create a UDP listener, then it's game over and we stop the JVM.
  //
  // This method does a number of things:
  // - Initialises the server's listening UDP port.
  // - Creates a Thread (ServerSocketMonitor) to listen for incoming UDP messages.
  //   The ServerSocketMonitor Thread communicates back to ServerLoop using
  //   a LinkedBlockingQueue.
  // - In a never-ending loop:
  //   - Sends periodic heartbeat (SRVHB) broadcast UDP messages.
  //   - Checks for the "reset" file and sends reset messages to clients listed
  //     in the file.
  //   - Checks for clients which have stopped transmitting and flags them as dead.
  //   - Stores details of new clients.
  
  public void run()
  {
    try
    {
      log("Starting server on UDP port " + (udp_port + 1));
      log("Broadcasting heartbeat messages to: " + broadcast_address);
      udp_socket = new DatagramSocket(udp_port + 1);
      udp_socket.setReuseAddress(true);
      our_name = InetAddress.getLocalHost().getHostName();
    }
    catch(Exception e)
    {
      log("Fatal exception creating UDP socket:");
      e.printStackTrace();
      System.exit(1);
    }
    
    Thread ssm = new Thread(new ServerSocketMonitor(udp_socket, this));
    ssm.start();

    Thread rest = new Thread(new RestServerListener(tcp_port, this));
    rest.start();
    
    long next_hb = System.currentTimeMillis() - 10;
    
    while(true)
    {
      if(System.currentTimeMillis() > next_hb)
      {
        next_hb = System.currentTimeMillis() + server_heartbeat_interval;

        try
        {
          byte[] advert_cmd = ("SRVHB%%" + our_name).getBytes();
          DatagramPacket sp = new DatagramPacket(advert_cmd, advert_cmd.length, InetAddress.getByName(broadcast_address), udp_port);
          udp_socket.send(sp);
        }
        catch(Exception e)
        {
          log("Exception sending UDP broadcast:");
          e.printStackTrace();
        }

        File reset_file = new File("reset");

        if(reset_file.exists())
        {
          try
          {
            BufferedReader rf = new BufferedReader(new FileReader(reset_file));
            String next_line;

            while((next_line = rf.readLine()) != null)
            {
              String client_name = next_line.trim();

              if(clients.containsKey(client_name))
              {
                log("Sending reset to " + client_name);
                InetSocketAddress client_socket_addr = (InetSocketAddress)clients.get(client_name)[1];
                byte[] reset_cmd = ("RESET%%" + client_name).getBytes();
                DatagramPacket sp = new DatagramPacket(reset_cmd, reset_cmd.length, client_socket_addr.getAddress(), client_socket_addr.getPort());
                udp_socket.send(sp);
              }
            }

            rf.close();
          }
          catch(Exception e)
          {
            log("Exception handling reset file:");
            e.printStackTrace();
          }

          reset_file.delete();
        }

        Iterator<String> iter = clients.keySet().iterator();

        while(iter.hasNext())
        {
          String client_host_name = iter.next();

          if((Long)clients.get(client_host_name)[0] < (System.currentTimeMillis() - client_timeout))
          {
            log(client_host_name + " has stopped tranmitting");

            try
            {
              ui_rec.put(new Object[]{new Long(System.currentTimeMillis()), client_host_name, "No updates", "GREEN", "IDLE" });
            }
            catch(InterruptedException e)
            {
              log("Exception queueing alert:");
              e.printStackTrace();
            }
          }
        }

        Object[] ui_item = new Object[]{ "MT"};

        while(ui_item != null)
        {
          ui_item = ui_rec.poll();

          if(ui_item != null)
          {
            if(ui_item[3].equals("RED"))
            {
              ui_red.put(ui_item[1] + ": " + ui_item[2], new Object[]{(Long)ui_item[0], (String)ui_item[4]});
            }
            else if(ui_item[3].equals("AMBER"))
            {
              ui_amber.put(ui_item[1] + ": " + ui_item[2], new Object[]{(Long)ui_item[0], (String)ui_item[4]});
            }
            else if(ui_item[3].equals("GREEN"))
            {
              ui_green.put(ui_item[1] + ": " + ui_item[2], new Object[]{(Long)ui_item[0], (String)ui_item[4]});
            }
          }
        }
      }

      try
      {
        Object[] new_update = new Object[]{ "MT"};

        while(new_update != null)
        {
          new_update = client_q.poll(server_heartbeat_interval, TimeUnit.MILLISECONDS);

          if(new_update != null)
          {
            if(new_update[0].equals("CLIENT_UPD"))
            {
              String new_client = (String)new_update[1];

              if(!(clients.containsKey(new_client)))
              {
                log("New client: " + new_client);
              }

              clients.put(new_client, new Object[]{new Long(System.currentTimeMillis()), (InetSocketAddress)new_update[2]});
            }
            else if(new_update[0].equals("REST_REQ"))
            {
              StringBuffer sb = new StringBuffer("{\"status\":\"ok\",");
              sb.append("\"red\":[");
              sb.append(get_ui_items(ui_red));
              sb.append("],");
              sb.append("\"amber\":[");
              sb.append(get_ui_items(ui_amber));
              sb.append("],");
              sb.append("\"green\":[");
              sb.append(get_ui_items(ui_green));
              sb.append("]}");

              ((LinkedBlockingQueue)new_update[1]).put(sb.toString());
            }
          }
        }
      }
      catch(Exception e)
      {
        log("Exception reading update queue:");
        e.printStackTrace();
      }
    }
  }

  String get_ui_items(HashMap<String, Object[]> ui_disp)
  {
    LinkedList<Map.Entry<String, Object[]>> lst = new LinkedList<Map.Entry<String, Object[]>>(ui_disp.entrySet());

    Collections.sort(lst, new Comparator<Map.Entry<String, Object[]>>() {
      public int compare(Map.Entry<String, Object[]> o1, Map.Entry<String, Object[]> o2)
      {
        return(((Long)o2.getValue()[0]).compareTo((Long)o1.getValue()[0]));
      }
    });

    ArrayList<String> msgs = new ArrayList<String>();

    for(Map.Entry<String, Object[]> entry : lst)
    {
      msgs.add(entry.getKey());
    }

    Iterator<String> iter = msgs.iterator();
    StringBuffer sb = new StringBuffer("");
    String pfx = "";

    while(iter.hasNext())
    {
      String msg = iter.next();
      Date ev_dt = new Date();
      ev_dt.setTime(((Long)ui_disp.get(msg)[0]));
      sb.append(pfx);
      sb.append("\"" + ev_dt.toString() + ": " + msg.replaceAll("\"", "") + "\"");
      pfx = ",";

      if(((Long)ui_disp.get(msg)[0]) < (System.currentTimeMillis() - ui_rtime.get((String)ui_disp.get(msg)[1])))
      {
        ui_disp.remove(msg);
      }
    }

    return(sb.toString());
  }
}
