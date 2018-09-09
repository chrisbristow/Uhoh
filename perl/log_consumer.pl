#!/usr/bin/perl
#
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

# ---------------
# log_consumer.pl
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

$|=1;

if($#ARGV<0)
{
  print STDERR "Usage: log_consumer.pl <filename>\n";
  exit(1);
}

$filename = $ARGV[0];

@srv=stat($filename);
$inode=$srv[1];
$sz=$srv[7];
$mt=$srv[9];

open(IN, "< $filename") || die "Error: $!";
seek(IN, 0, 2);

for(;;)
{
  while(<IN>)
  {
    print;
  }

  if(-f $filename)
  {
    @srv = stat($filename);

    if($inode != $srv[1] || $srv[7] < $sz)
    {
      close(IN);

      $inode = $srv[1];
      $sz = $srv[7];
      $mt = $srv[9];

      open(IN, "< $filename") || die "Error: $!";
    }
  }

  @srv = stat($filename);
  $sz = $srv[7];
  $mt = $srv[9];

  sleep(1);
}