#!/usr/bin/perl
#
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

# --------------------
# metric_aggregator.pl
# --------------------

# This program reads Uhoh Performance Management metric files and creates a new file
# containing a daily average for each day.
#
# Arguments supplied are:
# - <metric_name>: The name of the metric to read in.
# - <days_back>: The number of days worth of data to read in.
# - <start_hour>: The hour of day after which values are used.
# - <end_hour>: The hour of day after which values are not used.
# - <aggregate_name>: The name of the metric to create containing calculated average values.
# - <AVERAGE|TOTAL>: Choose whether the daily output is average or total.

$| = 1;

if($#ARGV != 5)
{
  print STDERR "metric_aggregator.pl <metric_name> <days_back> <start_hour> <end_hour> <aggregate_name> <AVERAGE|TOTAL>\n";
  exit(1);
}

$metric_name = $ARGV[0];
$days_back = $ARGV[1];
$start_time = $ARGV[2];
$end_time = $ARGV[3];
$agg_name = $ARGV[4];
$agg_type = $ARGV[5];

($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = localtime(time);
$dir_name = "../metrics/".sprintf("%04d-%02d-%02d", $year + 1900, $mon + 1, $mday);

if( ! -d $dir_name)
{
  print "Creating $dir_name";
  mkdir($dir_name);
}

open(OUT, "> ${dir_name}/${agg_name}") || die "Error: $!";

$s_utime = time - (86400 * $days_back);

for($c = 0; $c <= $days_back; $c ++)
{
  ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = localtime($s_utime);
  $dir_name = "../metrics/".sprintf("%04d-%02d-%02d", $year + 1900, $mon + 1, $mday);
  $daily_total = 0;
  $daily_count = 0;

  if( -d $dir_name)
  {
    if( -f "${dir_name}/${metric_name}")
    {
      open(IN, "< ${dir_name}/${metric_name}") || die "Error: $!";

      while(<IN>)
      {
        if($_ =~ m/^\d+,\d+,\d+,(\d+),\d+,\d+,([0-9\.]+)$/)
        {
          $hr = $1;
          $vl = $2;

          if($hr >= $start_time && $hr < $end_time)
          {
            $daily_total += $vl;
            $daily_count ++;
          }
        }
      }

      close(IN);
    }
  }

  if($agg_type =~ m/^.*TOTAL.*$/)
  {
    printf(OUT "%d,%d,%d,%d,0,0,%f\n", $year + 1900, $mon + 1, $mday, $end_time, $daily_total);
    print "T";
  }
  elsif($daily_count > 0)
  {
    printf(OUT "%d,%d,%d,%d,0,0,%f\n", $year + 1900, $mon + 1, $mday, $end_time, ($daily_total / $daily_count));
    print "A";
  }
  else
  {
    printf(OUT "%d,%d,%d,%d,0,0,%f\n", $year + 1900, $mon + 1, $mday, $end_time, 0);
    print "A";
  }

  $s_utime += 86400;
}

close(OUT);