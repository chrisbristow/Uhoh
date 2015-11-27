Keep an eye on your stuff.  Find out when things are going wrong.  Simple, lightweight distributed systems monitoring.
======================================================================================================================

What is Uhoh ?
--------------
Uhoh is an application which is designed to perform basic monitoring tasks on hosts, relaying any issues found back
to a set of centralised servers.  Key features of Uhoh include:

- Clients run on hosts to be monitored.
- Clients perform log file, process, disk and custom monitoring tasks.
- Clients fetch all of their configuration from Servers.
- No local configuration needed on hosts.
- Servers collate alarms from clients.
- Servers output alarm streams for other programs to consume and analyse.
- Servers host a basic web alarm view user interface.
- 100% Java / Javascript.
- Tiny installation (Servers and Clients).

Getting Started.
----------------
Download the zip file and unpack onto a host suitable for use as a Server.
Compile the source and create uhoh.jar:

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

Creating a Client Configuration File.
-------------------------------------
The example_client_config file provided with the base install of Uhoh contains detailed notes on how the different
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

For Process Monitoring, the Schauzer will:

Send an alert back to the Server if the number of running instances of a process (identified via a regular expression) is outside a
given acceptable range.
The command used to fetch the process table can be specified - eg. “ps -fe” for Linux,
“tasklist.exe” for Windows, “/usr/ucb/ps -wwaux” for Solaris etc.

Disk Monitoring checks periodically that the free space available for a particular filesystem or volume hasn’t exceeded a given amount.

Finally, Custom Monitoring allows the Client to run a command and relay output from that command that matches a regular expression back to the Server.

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
- The two clump-press support workshop PCs also run Uhoh Clients themselves which monitor the Python scripts’ output logs and feed alarms back to the Servers. (Derived alarms.)


