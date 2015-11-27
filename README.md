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
those tagged GREEN appear below Amber.  The Server retains (TBC)

