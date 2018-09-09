#       Licence
#       -------
#       Copyright (c) 2015-2018, Chris Bristow
#       All rights reserved.
#
#       Redistribution and use in source and binary forms, with or without
#       modification, are permitted provided that the following conditions are met:
#
#       1. Redistributions of source code must retain the above copyright notice, this
#       list of conditions and the following disclaimer.
#       2. Redistributions in binary form must reproduce the above copyright notice,
#       this list of conditions and the following disclaimer in the documentation
#       and/or other materials provided with the distribution.
#
#       THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
#       ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
#       WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
#       DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
#       ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
#       (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
#       LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
#       ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
#       (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
#       SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#
#       The views and conclusions contained in the software and documentation are those
#       of the authors and should not be interpreted as representing official policies,
#       either expressed or implied, of the FreeBSD Project.

# --------------------
# elastic_fantastic.py
# --------------------
#
# This script reads the Uhoh Server Log Stream file and pushes alert data into
# an ElasticSearch cluster.  The index for the data is of the form "uhoh-YYYYMMDD"
# and type is "server".
#
# Start-up arguments are:
# - Name of the Log Stream file.
# - URL of the ElasticSearch cluster:
#       eg. http://localhost:9200
#
# The script suppresses loading of data into ElasticSearch if it detects
# that the server is an FT Secondary instance.

import sys
import os
import time
import urllib.request
import re
import datetime

def main(filename, url_x):
  fisopen = False
  inode = 0
  seek = 2
  fsize = 0
  suppress_until = 0
  ft_timeout = 17

  while(True):
    if fisopen == False:
      try:
        st = os.stat(filename)
        inode = st.st_ino
        fd = open(filename)
        fd.seek(0, seek)
        fisopen = True
        seek = 0
        fsize = st.st_size

      except Exception:
        time.sleep(1)

    else:
      cp = fd.tell()
      nextline = fd.readline()

      if not nextline:
        try:
          st = os.stat(filename)

          if(st.st_ino != inode or st.st_size < fsize):
            fd.close()
            fisopen = False
          else:
            fd.seek(cp)
            time.sleep(1)

          fsize = st.st_size

        except Exception:
          time.sleep(1)

      else:
        print("")
        print("Log Stream: " + nextline.rstrip())

        alert_s = nextline.rstrip().split('%%')

        if alert_s[0].endswith("ALERT"):
          if alert_s[5] == "FT_SECONDARY":
            suppress_until = int(time.time() + ft_timeout)
            print("FT Secondary")

          else:
            if int(time.time() > suppress_until):
              ztime = datetime.datetime.fromtimestamp(int(alert_s[3]) // 1000).isoformat() + time.strftime('%z')
              payload = '{ "host": "' + alert_s[1] + '", "datetime": "' + ztime + '", "tags": "' + alert_s[5] + '", "message": "' + alert_s[6] + '" }'
              a_url = url_x + "/uhoh-" + time.strftime('%Y%m%d') + "/server"
              req = urllib.request.Request(url=a_url, method='POST', data=payload.encode())

              try:
                xreq = urllib.request.urlopen(req)
                all_lines = xreq.readlines()
                xreq.close()
                print("Posted: " + payload + " to " + a_url)

              except Exception:
                print("Warning: Posting to ElasticSearch has failed: " + nextline.rstrip())

            else:
              print("Posting to ElasticSearch: Suppressed for " + str(suppress_until - int(time.time())) + " second(s)")

if __name__ == '__main__':
  if len(sys.argv) < 2:
    print('Usage: elastic_fantastic.py <log_stream_file> <elastic_url>')
    exit(1)

  else:
    main(sys.argv[1], sys.argv[2])
