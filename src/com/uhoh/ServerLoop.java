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

import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;

/*
  The ServerLoop() object is the main control part of an Uhoh Server.
  It handles:
  - Loading an parsing of the Server properties file.
  - Spawning the ServerSocketMonitor() and RestServerListener() threads.
  - Sending Server heartbeat messages.
  - Handling "reset" and "forget" commands from an adminstrator.
  - Monitoring for unresponsive clients.
  - Management of active alerts for the web UI.
  - Logging of alerts to the Server disk log.
 */

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
  HashMap<String, Object[]> ui_purple = new HashMap<String, Object[]>();
  HashMap<String, Long> ui_rtime = new HashMap<String, Long>();
  String unicast_addrs = null;
  String secondary_server = null;
  String dead_client_tags = "GREEN";
  String no_config_tags = "GREEN,NO_CLIENT_CONFIG";
  HashMap<String, Object[]> last_metric_store = new HashMap<String, Object[]>();

  // Initialise a new ServerLoop() by loading and parsing the Server
  // properties file.
  
  public ServerLoop(String props_file)
  {
    logging_pfx = "S";

    try
    {
      Properties props = new Properties();
      props.load(new FileReader(props_file));

      udp_port = Integer.parseInt(props.getProperty("udp_port_number"));
      broadcast_address = props.getProperty("udp_broadcast_address");
      tcp_port = Integer.parseInt(props.getProperty("tcp_port_number"));
      client_timeout = Long.parseLong(props.getProperty("client_timeout"));
      client_remove_time = Long.parseLong(props.getProperty("client_remove_time"));
      server_heartbeat_interval = Long.parseLong(props.getProperty("heartbeat_interval"));
      rolling_disk_log_name = props.getProperty("disk_log_name");
      rolling_disk_log_size = Long.parseLong(props.getProperty("disk_log_size"));
      ui_rtime.put("ALL", new Long(props.getProperty("ui_display_time_ALL")));
      ui_rtime.put("PROCESS", new Long(props.getProperty("ui_display_time_PROCESS")));
      ui_rtime.put("FILE", new Long(props.getProperty("ui_display_time_FILE")));
      ui_rtime.put("IDLE", new Long(props.getProperty("ui_display_time_IDLE")));
      dead_client_tags = props.getProperty("dead_client_tags");
      no_config_tags = props.getProperty("no_config_tags");

      log("Client/Server UDP ports:              " + udp_port + " / " + (udp_port + 1));
      log("Server broadcast address:             " + broadcast_address);
      log("Web UI server TCP port:               " + tcp_port);
      log("Client inactivity timeout:            " + client_timeout + " ms");
      log("Client watchlist timeout:             " + client_remove_time + " ms");
      log("Server will sent heartbeats every:    " + server_heartbeat_interval + " ms");
      log("Server will log events to:            " + rolling_disk_log_name);
      log("Event log will roll after it reaches: " + rolling_disk_log_size + " bytes");
      log("Dead client alert tags are:           " + dead_client_tags);
      log("Unknown client alert tags are:        " + no_config_tags);
      log("UI: Log file events held for:         " + ui_rtime.get("FILE") + " ms");
      log("UI: Process events held for:          " + ui_rtime.get("PROCESS") + " ms");
      log("UI: Idle client events held for:      " + ui_rtime.get("IDLE") + " ms");
      log("UI: Other events held for:            " + ui_rtime.get("ALL") + " ms");

      if(props.getProperty("prom_metric_max_age") != null)
      {
        prom_metric_max_age = Long.parseLong(props.getProperty("prom_metric_max_age"));
        log("Prometheus metric max age:            " + prom_metric_max_age + " ms");
      }

      if(props.getProperty("udp_unicast_address") != null)
      {
        unicast_addrs = props.getProperty("udp_unicast_address").trim();
        log("Server unicast address(es):           " + unicast_addrs);
      }

      if(props.getProperty("secondary_server") != null)
      {
        secondary_server = props.getProperty("secondary_server").trim();
        log("Secondary server:                     " + secondary_server);
      }

      if(props.getProperty("launch") != null)
      {
        String launch = props.getProperty("launch").trim();
        log("Running:                              " + launch);
        Runtime.getRuntime().exec(launch);
      }
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
  
  public void run()
  {
    // If we can't create a UDP listener, then it's game over and we stop the JVM.

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

    // Spawn the ServerSocketMonitor() thread to handling incoming UDP
    // messages from Clients.
    
    Thread ssm = new Thread(new ServerSocketMonitor(udp_socket, this));
    ssm.start();

    // Spawn a RestServerListener() thread to handling incoming HTTP
    // requests from the Web UI.

    Thread rest = new Thread(new RestServerListener(tcp_port, this));
    rest.start();
    
    long next_hb = System.currentTimeMillis() - 10;
    long ka_log_interval = 60000;
    long next_ka_log = System.currentTimeMillis() + ka_log_interval;
    
    while(true)
    {
      if(System.currentTimeMillis() > next_hb)
      {
        // Send a Server heartbeat (broadcast or unicast - if unicast heartbeating is configured).

        next_hb = System.currentTimeMillis() + server_heartbeat_interval;

        get_ui_items(ui_red);
        get_ui_items(ui_amber);
        get_ui_items(ui_green);
        get_ui_items(ui_purple);

        try
        {
          byte[] advert_cmd = ("SRVHB%%" + our_name).getBytes();
          DatagramPacket sp = new DatagramPacket(advert_cmd, advert_cmd.length, InetAddress.getByName(broadcast_address), udp_port);
          udp_socket.send(sp);

          if(unicast_addrs != null)
          {
            for(String uaddr : unicast_addrs.split(","))
            {
              DatagramPacket sp_u = new DatagramPacket(advert_cmd, advert_cmd.length, InetAddress.getByName(uaddr.trim()), udp_port);
              udp_socket.send(sp_u);
            }
          }

          if(secondary_server != null)
          {
            long utime = (new Date()).getTime();
            byte[] ss_cmd = ("ALERT%%" + our_name + "%%FT%%" + utime + "%%SERVER%%FT_SECONDARY%%FT primary is: " + our_name).getBytes();
            DatagramPacket sp_u = new DatagramPacket(ss_cmd, ss_cmd.length, InetAddress.getByName(secondary_server.trim()), udp_port + 1);
            udp_socket.send(sp_u);
          }
        }
        catch(Exception e)
        {
          log("Exception sending UDP message:");
          e.printStackTrace();
        }

        // Handle Client resets.

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

        // Remove a Client from the Server's watchlist if the Client is to be
        // decommissioned.

        File forget_file = new File("forget");

        if(forget_file.exists())
        {
          try
          {
            BufferedReader ff = new BufferedReader(new FileReader(forget_file));
            String next_line;

            while((next_line = ff.readLine()) != null)
            {
              String client_name = next_line.trim();

              log("Removing monitoring for " + client_name);
              clients.remove(client_name);
            }

            ff.close();
          }
          catch(Exception e)
          {
            log("Exception handling forget file:");
            e.printStackTrace();
          }

          forget_file.delete();
        }

        // Check to see if any Clients have become unresponsive.

        Iterator<String> iter = clients.keySet().iterator();

        while(iter.hasNext())
        {
          String client_host_name = iter.next();

          if((Long)clients.get(client_host_name)[0] < (System.currentTimeMillis() - client_timeout))
          {
            log(client_host_name + " has stopped transmitting");

            try
            {
              client_q.put(new Object[]{"ALERT", client_host_name, "IDLE", new Long(System.currentTimeMillis()), "SERVER", dead_client_tags, "No updates from client"});
            }
            catch(InterruptedException e)
            {
              log("Exception queueing client idle alert:");
              e.printStackTrace();
            }

            if((Long)clients.get(client_host_name)[0] < (System.currentTimeMillis() - client_remove_time))
            {
              log(client_host_name + " will now be removed from the watchlist");
              clients.remove(client_host_name);
            }
          }
        }

        if(System.currentTimeMillis() > next_ka_log)
        {
          log("Uhoh");
          next_ka_log = System.currentTimeMillis() + ka_log_interval;
        }
      }

      // Read the update queue for things to do:

      try
      {
        Object[] new_update = new Object[]{"MT"};

        while(new_update != null)
        {
          new_update = client_q.poll(server_heartbeat_interval, TimeUnit.MILLISECONDS);

          if(new_update != null)
          {
            if(new_update[0].equals("CLIENT_UPD"))
            {
              // Add a newly started Client to the Server watchlist.

              String new_client = (String)new_update[1];

              if(!(clients.containsKey(new_client)))
              {
                log("New client: " + new_client);
              }

              clients.put(new_client, new Object[]{new Long(System.currentTimeMillis()), (InetSocketAddress)new_update[2]});
            }
            else if(new_update[0].equals("REST_REQ"))
            {
              // Handle incoming Web UI requests.

              StringBuffer sb = new StringBuffer("{\"status\":\"ok\",");
              sb.append("\"red\":[");
              sb.append(get_ui_items(ui_red));
              sb.append("],");
              sb.append("\"amber\":[");
              sb.append(get_ui_items(ui_amber));
              sb.append("],");
              sb.append("\"green\":[");
              sb.append(get_ui_items(ui_green));
              sb.append("],");
              sb.append("\"purple\":[");
              sb.append(get_ui_items(ui_purple));
              sb.append("]}");

              ((LinkedBlockingQueue)new_update[1]).put(sb.toString());
            }
            else if(new_update[0].equals("ALERT"))
            {
              // Handle alerts.

              HashSet<String> tags = new HashSet<String>(Arrays.asList(((String)new_update[5]).split(",")));

              if(tags.contains("RED"))
              {
                ui_red.put(new_update[1] + ": " + new_update[6], new Object[]{new Long(System.currentTimeMillis()), (String)new_update[2], (String)new_update[5]});
              }
              else if(tags.contains("AMBER"))
              {
                ui_amber.put(new_update[1] + ": " + new_update[6], new Object[]{new Long(System.currentTimeMillis()), (String)new_update[2], (String)new_update[5]});
              }
              else if(tags.contains("GREEN"))
              {
                ui_green.put(new_update[1] + ": " + new_update[6], new Object[]{new Long(System.currentTimeMillis()), (String)new_update[2], (String)new_update[5]});
              }
              else if(tags.contains("PURPLE"))
              {
                ui_purple.put(new_update[1] + ": " + new_update[6], new Object[]{new Long(System.currentTimeMillis()), (String)new_update[2], (String)new_update[5]});
              }

              disk_log("ALERT%%" + new_update[1] + "%%" + new_update[2] + "%%" + new_update[3] + "%%" + new_update[4] + "%%" + new_update[5] + "%%" + new_update[6]);
            }
            else if(new_update[0].equals("PROM_REQ"))
            {
              // Handle incoming requests from Prometheus:

              StringBuffer sb = new StringBuffer("");
              Iterator<String> iter = last_metric_store.keySet().iterator();
              HashSet<String> to_purge = new HashSet<String>();

              while(iter.hasNext())
              {
                String desc = iter.next();
                Object[] m_val = last_metric_store.get(desc);

                if((Long)m_val[1] < (System.currentTimeMillis() - prom_metric_max_age))
                {
                  to_purge.add(desc);
                }
                else
                {
                  sb.append("uhoh_metric{server=\"" + our_name + "\",metric=\"" + desc + "\"} " + m_val[0]);
                  sb.append("\r\n");
                }
              }

              // Purge any metrics which haven't been updated for "prom_metric_max_age":

              Iterator<String> iter2 = to_purge.iterator();

              while(iter2.hasNext())
              {
                String m_name = iter2.next();

                log("Purging idle metric: " + m_name);

                last_metric_store.remove(m_name);
              }

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

  // The get_ui_items() method sorts and maintains the list of current
  // alerts.  This list is used by the Web UI.

  public String get_ui_items(HashMap<String, Object[]> ui_disp)
  {
    LinkedList<Map.Entry<String, Object[]>> lst = new LinkedList<Map.Entry<String, Object[]>>(ui_disp.entrySet());

    Collections.sort(lst, new Comparator<Map.Entry<String, Object[]>>()
    {
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
      sb.append("[\"" + ev_dt.toString() + ": " + msg.replaceAll("\"", "").replaceAll("\\\\", "\\\\\\\\") + "\", \"" + (String)ui_disp.get(msg)[2] + "\"]");
      pfx = ",";

      if(((Long)ui_disp.get(msg)[0]) < (System.currentTimeMillis() - ui_rtime.get((String)ui_disp.get(msg)[1])))
      {
        log("Purging: " + msg);

        try
        {
          ui_disp.remove(msg);
        }
        catch(Exception e)
        {
          log("Exception purging timed-out alert: " + msg + " :");
          e.printStackTrace();
        }
      }
    }

    return(sb.toString());
  }

  // Write messages to a log file using the log() method.
  // The disk_log() method also handles alerts which contain tags indicating that they should be
  // written to metric capture files.

  void disk_log(String s)
  {
    log(s);

    try
    {
      String[] msg = s.split("%%");
      String[] tag = msg[5].split(",");

      for(int i = 0; i < tag.length; i ++)
      {
        if(tag[i].startsWith("METRIC_"))
        {
          double metric_value = 0;
          boolean contains_metric = false;

          try
          {
            metric_value = Double.parseDouble(msg[6].split(": ")[1]);
            contains_metric = true;
          }
          catch(Exception e1)
          {
            log("Warning: No metric found in: " + msg[6]);
          }

          if(contains_metric)
          {
            String metric_name = tag[i].replaceFirst("METRIC_", "");

            last_metric_store.put(metric_name, new Object[]{ metric_value, System.currentTimeMillis() });

            File metric_dir = new File("metrics");

            if(!metric_dir.exists())
            {
              log("Creating folder: metrics");
              metric_dir.mkdir();
            }

            GregorianCalendar gc = new GregorianCalendar();
            String dir_name = String.format("%04d-%02d-%02d", gc.get(Calendar.YEAR), gc.get(Calendar.MONTH) + 1, gc.get(Calendar.DAY_OF_MONTH));
            File date_dir = new File("metrics/" + dir_name);

            if(!date_dir.exists())
            {
              log("Creating folder: metrics/" + dir_name);
              date_dir.mkdir();
            }

            String metric_line = gc.get(Calendar.YEAR) + "," + (gc.get(Calendar.MONTH) + 1) + "," + gc.get(Calendar.DAY_OF_MONTH) + "," + gc.get(Calendar.HOUR_OF_DAY) + "," + gc.get(Calendar.MINUTE) + "," + gc.get(Calendar.SECOND) + "," + metric_value + "\n";
            FileWriter metric_file = new FileWriter("metrics/" + dir_name + "/" + metric_name, true);
            metric_file.write(metric_line);
            metric_file.close();

            log("Value " + metric_value + " written to metrics/" + dir_name + "/" + metric_name);
          }
        }
      }
    }
    catch(Exception e)
    {
      log("Exception logging metric: " + s + ":");
      e.printStackTrace();
    }
  }
}
