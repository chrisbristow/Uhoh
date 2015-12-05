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
  print STDERR "Usage: t.pl <filename>\n";
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