#       Licence
#       -------
#       Copyright (c) 2015-2017, Chris Bristow
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

# ---------------
# log_consumer.py
# ---------------

# This program consumes a log file (reads from the tail of the file).
# If the file is "rolled" (ie. copied/moved to a backup file name and
# a new file created in it's place with the same name, then this
# program will continue to read from the new file.
#
# This program can be used as a basis for implementing Uhoh Alert
# Handlers - which read from Uhoh Server log files and act on
# the alerts contained therein (for example, sending emails or
# generating derived alarms).

import sys
import os
import time

def main(filename):
  fisopen = False
  inode = 0
  seek = 2
  fsize = 0

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
        print(nextline.rstrip())

if __name__ == '__main__':
  if len(sys.argv) < 2:
    print('Usage: log_consumer.py <filename>')
    exit(1)

  else:
    main(sys.argv[1])