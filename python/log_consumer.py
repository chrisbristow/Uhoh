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
        #print('DBG: Opening: ' + filename)
        st = os.stat(filename)
        inode = st.st_ino
        fd = open(filename)
        fd.seek(0, seek)
        fisopen = True
        seek = 0
        fsize = st.st_size

      except Exception:
        #print('Error: File '+ filename +' not found')
        time.sleep(1)

    else:
      cp = fd.tell()
      nextline = fd.readline()

      if not nextline:
        try:
          st = os.stat(filename)

          if(st.st_ino != inode or st.st_size < fsize):
            #print('DBG: Rolled: ' + filename)
            fd.close()
            fisopen = False
          else:
            fd.seek(cp)
            time.sleep(1)
            #print('DBG: Sleep: ' + filename)

          fsize = st.st_size

        except Exception:
          #print('Error: File '+ filename +' not found')
          time.sleep(1)

      else:
        print(nextline.rstrip())

if __name__ == '__main__':
  if len(sys.argv) < 2:
    print('Usage: log_consumer.py <filename>')
    exit(1)

  else:
    main(sys.argv[1])