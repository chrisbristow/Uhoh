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
  HashMap<String, Object[]> ui_red = new HashMap<String, Object[]>();
  HashMap<String, Object[]> ui_amber = new HashMap<String, Object[]>();
  HashMap<String, Object[]> ui_green = new HashMap<String, Object[]>();
  HashMap<String, Long> ui_rtime = new HashMap<String, Long>();
  FileOutputStream logmgr = null;

  // Initialise a new ServerLoop().
  
  ServerLoop(String props_file)
  {
    try
    {
      Properties props = new Properties();
      props.load(new FileReader(props_file));

      udp_port = Integer.parseInt(props.getProperty("udp_port_number"));
      broadcast_address = props.getProperty("udp_broadcast_address");
      tcp_port = Integer.parseInt(props.getProperty("tcp_port_number"));
      client_timeout = Long.parseLong(props.getProperty("client_timeout"));
      server_heartbeat_interval = Long.parseLong(props.getProperty("heartbeat_interval"));
      server_disk_log_name = props.getProperty("disk_log_name");
      server_disk_log_size = Long.parseLong(props.getProperty("disk_log_size"));
      ui_rtime.put("ALL", new Long(props.getProperty("ui_display_time_ALL")));
      ui_rtime.put("PROCESS", new Long(props.getProperty("ui_display_time_PROCESS")));
      ui_rtime.put("FILE", new Long(props.getProperty("ui_display_time_FILE")));
      ui_rtime.put("IDLE", new Long(props.getProperty("ui_display_time_IDLE")));

      log("Client/Server UDP ports:              " + udp_port + " / " + (udp_port + 1));
      log("Server broadcast address:             " + broadcast_address);
      log("Web UI server TCP port:               " + tcp_port);
      log("Client inactivity timeout:            " + client_timeout + " ms");
      log("Server will sent heartbeats every:    " + server_heartbeat_interval + " ms");
      log("Server will log events to:            " + server_disk_log_name);
      log("Event log will roll after it reaches: " + server_disk_log_size + " bytes");
      log("UI: Log file events held for:         " + ui_rtime.get("FILE") + " ms");
      log("UI: Process events held for:          " + ui_rtime.get("PROCESS") + " ms");
      log("UI: Idle client events held for:      " + ui_rtime.get("IDLE") + " ms");
      log("UI: Other events held for:            " + ui_rtime.get("ALL") + " ms");
    }
    catch(Exception e)
    {
      log("Exception: Unable to load the server properties");
      System.exit(1);
    }
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
              client_q.put(new Object[]{"ALERT", client_host_name, "IDLE", new Long(System.currentTimeMillis()), "SYSTEM", "GREEN", "No updates"});
            }
            catch(InterruptedException e)
            {
              log("Exception queueing client idle alert:");
              e.printStackTrace();
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
            else if(new_update[0].equals("ALERT"))
            {
              HashSet<String> tags = new HashSet<String>(Arrays.asList(((String)new_update[5]).split(",")));

              if(tags.contains("RED"))
              {
                ui_red.put(new_update[1] + ": " + new_update[6], new Object[]{new Long(System.currentTimeMillis()), (String)new_update[2]});
              }
              else if(tags.contains("AMBER"))
              {
                ui_amber.put(new_update[1] + ": " + new_update[6], new Object[]{new Long(System.currentTimeMillis()), (String)new_update[2]});
              }
              else if(tags.contains("GREEN"))
              {
                ui_green.put(new_update[1] + ": " + new_update[6], new Object[]{new Long(System.currentTimeMillis()), (String)new_update[2]});
              }

              disk_log(log("ALERT%%" + new_update[1] + "%%" + new_update[2] + "%%" + new_update[3] + "%%" + new_update[4] + "%%" + new_update[5] + "%%" + new_update[6]));
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

  void disk_log(String s)
  {
    try
    {
      if(logmgr == null)
      {
        logmgr = new FileOutputStream(server_disk_log_name, true);
      }

      logmgr.write((s + "\n").getBytes());

      if(logmgr.getChannel().size() > server_disk_log_size)
      {
        logmgr.close();
        logmgr = null;
        (new File(server_disk_log_name)).renameTo(new File(server_disk_log_name + ".1"));
        (new File(server_disk_log_name)).createNewFile();
      }
    }
    catch(Exception e)
    {
      log("Exception logging to disk:");
      e.printStackTrace();
    }
  }
}
