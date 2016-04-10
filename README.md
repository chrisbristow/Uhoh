Keep an eye on your stuff.  Find out when things are going wrong.  Simple, lightweight distributed system monitoring.
=====================================================================================================================

Why Uhoh ?
----------
Uhoh is an application which is performs basic monitoring tasks on hosts, relaying any issues found back
to a set of centralised servers.  Key features of Uhoh include:

- Clients run on hosts to be monitored.
- Clients perform log file, process, disk and custom monitoring tasks.
- Clients fetch all of their configuration from Servers.
  - No local configuration needed on hosts.
- Servers collate alerts from clients.
- Servers output alert streams for other programs to consume and analyse.
- Servers host a basic web alert view user interface.
- Alerts correlation is supported allowing combinations of alerts to trigger further alerts.
- 100% Java & Javascript.
- Really tiny installation (both Servers and Clients).

I know what you're thinking - "There are plenty of other monitoring solutions out there, so why go with Uhoh ?".
The answer to this is really simplicity.  Very little time needs to be invested in getting Uhoh up and running
so you never need to leave setting up monitoring to later date.  Uhoh doesn't get in the way of your
deployment schedule and you can think of it in a similar way to Test Driven Development -
"Monitoring Driven Deployment" maybe ?  Keeping things lightweight and simple will encourage the use of more
complete and robust system monitoring - resulting in increased reliability all round.

Getting Started.
----------------
Download the zip file and unpack onto a host suitable for use as a Server.
You should have the following folders:

- src
- web
- clientconfigs

An uhoh.jar archive, containing both the Server and Client is provided, but if
you would like to compile from source, it's as simple as running:

javac -d . src/com/uhoh/*.java
jar -cvf uhoh.jar com

The clientconfigs folder contains Client monitoring configuration files.
For a fresh installation, only the example configuration file will exist in this folder.
Create configuration files for your hosts (filenames are the hostnames) in clientconfigs.

Start the Server:

java -cp uhoh.jar com.uhoh.Server server.properties

Copy uhoh.jar to a Client host and start as follows:

java -cp uhoh.jar com.uhoh.Client 8888

Point your browser to the UI web server (http://server_host:7777/).

Servers use UDP broadcast to indicate their presence to Clients.  However, for environments which don't
support UDP broadcast (eg. AWS), Clients can be provided with a comma-separated list of Server IP
addresses in addition to the UDP port.  For example, to start a Client in a system with two
Servers at 172.31.11.1 and 172.31.11.2 use:

java -cp uhoh.jar com.uhoh.Client 8888 172.31.11.1,172.31.11.2

(Note that the Client will always request its configuration from the first IP address given.)

Creating a Client Configuration File.
-------------------------------------
The "example_client_config" file provided with the base install of Uhoh contains detailed notes on how the different
directives used for configuring log file, process, disk and custom monitoring work.

An Uhoh client will create a separate thread (known as a Schnauzer) to monitor each item it has been instructed
to look at.  The only exception to this is for process monitoring where a single thread is used to monitor all processes.

For Log File Monitoring, Schauzers will consume the file - coping with files that are “rolled” (where the
existing log is copied to another name and a new log is created once the log reaches a certain size).  Lines in
the log file are matched using a regular expression and then an action is taken.  Action types available for log files are:

- Send the log file line matched back to the Server.
- Send a custom message back to the Server for each match.
- Count how many matches there have been over a set period of time and send the number of matches to the Server at the end of the period.
- Count how many matches there have been over a set period of time and sent a custom alert or the count value back to the Server if the count is outside an expected range.

For Process Monitoring, the Schnauzer will:

- Send an alert back to the Server if the number of running instances of a process (identified via a regular expression) is outside a given acceptable range.

The command used to fetch the process table can be specified - eg. “ps -fe” for Linux,
“tasklist.exe” for Windows, “/usr/ucb/ps -wwaux” for Solaris etc.

Disk Monitoring checks periodically that the free space available for a particular filesystem or volume hasn’t exceeded a given amount.

Socket monitoring checks to see if a TCP socket is available and accepting incoming connections (for example for checking
whether a web server is running).

Finally, Custom Monitoring allows the Client to run a command and relay output from that command that matches a regular expression back to the Server.

Managing Running Clients.
-------------------------
If a Client configuration is updated, the Client will need to be told that a new configuration is available.  The simplest
way to do this is just to re-start the Client, but wouldn't it be better if there was an easier way ?  Well, there is - the
"reset" procedure.  Here's how it works:

- Servers periodically check for a file called "reset" located in their root folder.
- If this file exists, the Server will open and read the file.
- For each host name listed in the file, the Server will send a "reset" command to the appropriate Client.
  - The file should contain a new host name on each line.
- The Clients which have been reset will close down their existing Schnauzers and re-request configuration from the Server.
- The Server finally deletes the "reset" file.

Occasionally, a host will need to be decommissioned.  If this host is running a Client and the Client is closed down, then
the Server will repeatedly log a "No updates" alert for the Client.  To prevent this happening and make the Server forget about
the Client, use a "forget" file, as follows:

- Servers periodically check for a file called "forget" located in their root folder.
- If this file exists, the Server will open and read the file.
- For each host name listed in the file, the Server will remove the host from its internal Client watchlist.
  - The file should contain a new host name on each line.

The Browser User Interface.
---------------------------
Each Server hosts an embedded web server which is used to serve a simple browser based user interface which can be used to view
certain types of alerts.  The user interface page lists recently active alerts divided into three severity
categories - Red, Amber and Green.  For alerts to appear in the Red section (at the top of the list) they must
be tagged with the ID RED (other tags can be included beside this tag).  Alerts tagged AMBER appear below Red and
those tagged GREEN appear below Amber.

The Server retains alerts in the web UI for a set period of time - different categories of alert are retained for different times:

- Log file alerts are retained for 10 minutes.
- Process alerts are retained for 65 seconds.
- Disk usage and Custom alerts are retained for 125 seconds.
- Idle client alerts are retained for 75 seconds.

All of these times can be configured in the server.properties file.

Note that the Browser UI requires the Mootools Javascript library, Google Fonts and the Google Visualization framework, so
UI users will need Internet access.

Visual Metric Display (Charts).
-------------------------------
If a log file Schnauzer has been configured to record the number of matches in a log file over a period of time (using
the "alert_count" directive), the Server can be instructed to log these metrics to disk and a browser used to
display charts showing the variation in value over time.  Metrics are grouped together in 24-hour chunks.  To configure
the Server to start recording metrics, simply add a "METRIC_" prefix to the tag which contains the identifying name
of the metric - eg. "METRIC_COUNT_1" for the metric "COUNT_1".

Once the Server has started recording metrics, these can be displayed in a browser via a URL of the format:

- /metric/YYYY-MM-DD/METRIC_NAME

Eg:

- /metric/2015-12-29/COUNT_1

To always display metric values from the current day, replace the YYYY-MM-DD part of the URL with the string
TODAY - eg:

- /metric/TODAY/COUNT_1

Let’s Present an Example Usage Scenario for Uhoh.
-------------------------------------------------
“Amalgamated Paperclips PLC has a network of Raspberry Pi 2s, running Ubuntu Linux, which are used to control the
factory clump-presses.  Although the clump-press control software running on each of them is very reliable, it
needs to be monitored constantly so that issues logged concerning the health of the clump-presses themselves can
be captured, a clump-press minder dispatched to fix the problem, so to avoid the entire production line grinding to a halt.
Three beige-box PCs, also running Ubuntu, are also deployed on the network.  One sits in the management office and the
other two in the clump-press support workshop.”

Here’s how the system is set up from a monitoring perspective:

- Uhoh is installed on all of the Pi 2s and Clients started, listening on UDP port 8888.
- The three PCs also have Uhoh installed on them and have been set up to run Uhoh Servers.
- The three Servers maintain all Client configuration files and if a Client config is updated, then that config is pushed to the other two Servers such that they all stay in sync.  Once the push has been performed, a “reset” for the updated Client can be sent from one of the Servers.
- Each Client config contains configuration directives to:
  - Check that the clump-press minding daemon (“clumppressd”) is running.
  - Check that no more than 80% of disk space is in use.
  - Check the /var/log/clumppressd.log file for indications that the press being controlled hasn’t developed a fault.
- Remember, if a clump-press catches fire, Uhoh will notice that its Client is no longer running and generate an alert - probably before the smell of smoke makes it to the clump-press minders.
- The three Servers, of course, are all used as Web UI servers for the factory management office users and clump-press minders.
- The two PCs located in the clump-press support workshop also run various other Python scripts which:
  - Consume the server.log log file from the Server.
  - Pick up and correlate alerts which appear in this file.
  - Write messages to other log files.
  - Send email alerts to support staff inboxes.
- The two clump-press support workshop PCs also run Uhoh Clients themselves which monitor the Python scripts’ output logs and feed alerts back to the Servers. (Derived alerts.)


