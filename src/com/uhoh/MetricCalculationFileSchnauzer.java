/*
        Licence
        -------
        Copyright (c) 2016, Chris Bristow
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

import java.util.*;

/*
  The MetricCalculationFileSchnauzer() class is an versatile FileSchnauzer() which is used
  to capture the number of times a line has appeared in a log file, or to capture specific metrics
  from a log file.  After a configurable period, the MetricCalculationFileSchnauzer() can log:
  - The number of times a match has occurred.
  - The total / minimum / maximum / average from a metric captured.
 */

public class MetricCalculationFileSchnauzer extends BasicMatchFileSchnauzer
{
  long match_count = 0;
  long match_total = 0;
  long match_minimum = 999999999999999999L;
  long match_maximum = 0;
  long metrics_interval = 60000;
  long next_checkpoint = 0;
  long greater_than = -1;
  long less_than = -1;
  MetricCalcs capture_type;

  // Construct a MetricCalculationFileSchnauzer().

  MetricCalculationFileSchnauzer(String f, String a, String t, EventCollector ec, String cap, long m, MetricCalcs tp, String rx, String lt, String gt, String msg)
  {
    super(f, a, t, ec, rx, "", cap);
    metrics_interval = m;
    capture_type = tp;
    message = msg;
    next_checkpoint = (new Date()).getTime() + metrics_interval;

    try
    {
      if(message != null && message != "")
      {
        log(" - Message: " + message);
      }

      if(gt != null)
      {
        greater_than = Long.parseLong(gt);
        log(" - Alert if N > " + greater_than);
      }

      if(lt != null)
      {
        less_than = Long.parseLong(lt);
        log(" - Alert if N < " + less_than);
      }
    }
    catch(Exception e)
    {
      log("Warning: Unable to parse minimum / maximum / message configuration parameters");
    }

    log(" - Interval is " + m + " ms");
    log(" - Type is " + capture_type.toString());
  }

  // Override string_processor() to perform the regex match and either count
  // matches or collect metrics (using the translate_string() method).
  //
  // Note that the THRESHOLD type is used by the ThresholdFileSchnauzer() class
  // which is derived from this one.

  public void string_processor(String s)
  {
    if(is_matching(s))
    {
      if(capture_type == MetricCalcs.COUNT || capture_type == MetricCalcs.THRESHOLD)
      {
        match_count++;
      }
      else
      {
        try
        {
          long captured_value = Long.parseLong(translate_string(s, tx));

          match_total += captured_value;

          if(captured_value > match_maximum)
          {
            match_maximum = captured_value;
          }

          if(captured_value < match_minimum)
          {
            match_minimum = captured_value;
          }

          match_count++;
        }
        catch(Exception e)
        {
          log("Warning: Capture of metric from \"" + s + "\" has failed");
        }
      }
    }
  }

  // Output:
  // - match count.
  // - or, total captured.
  // - or, average from captured - if any data has been captured.
  // - or, maximum from captured - if any data has been captured.
  // - or, minimum from captured - if any data has been captured.
  // ... then reset the counters.

  public void timed_event_processor()
  {
    if((new Date()).getTime() > next_checkpoint)
    {
      if(is_active(active_string))
      {
        switch(capture_type)
        {
          case THRESHOLD:
            String threshold_msg = message;

            if(threshold_msg == null || threshold_msg == "")
            {
              threshold_msg = "" + match_count;
            }

            if(greater_than >= 0 && match_count > greater_than)
            {
              event_collector.dispatch("CLIENT%%" + tags + "%%" + filename + ": " + threshold_msg, "FILE");
            }

            if(less_than > 0 && match_count < less_than)
            {
              event_collector.dispatch("CLIENT%%" + tags + "%%" + filename + ": " + threshold_msg, "FILE");
            }
            break;

          case COUNT:
            event_collector.dispatch("CLIENT%%" + tags + "%%" + filename + ": " + match_count, "FILE");
            break;

          case TOTAL:
            if(message != null && (greater_than >= 0 || less_than > 0))
            {
              if(greater_than >= 0 && match_total > greater_than)
              {
                event_collector.dispatch("CLIENT%%" + tags + "%%" + filename + ": " + message, "FILE");
              }

              if(less_than > 0 && match_total < less_than)
              {
                event_collector.dispatch("CLIENT%%" + tags + "%%" + filename + ": " + message, "FILE");
              }
            }
            else
            {
              event_collector.dispatch("CLIENT%%" + tags + "%%" + filename + ": " + match_total, "FILE");
            }
            break;

          case AVERAGE:
            if(match_count > 0)
            {
              long match_average = match_total / match_count;

              if(message != null && (greater_than >= 0 || less_than > 0))
              {
                if(greater_than >= 0 && match_average > greater_than)
                {
                  event_collector.dispatch("CLIENT%%" + tags + "%%" + filename + ": " + message, "FILE");
                }
                else if(less_than > 0 && match_average < less_than)
                {
                  event_collector.dispatch("CLIENT%%" + tags + "%%" + filename + ": " + message, "FILE");
                }
              }
              else
              {
                event_collector.dispatch("CLIENT%%" + tags + "%%" + filename + ": " + match_average, "FILE");
              }
            }
            break;

          case MINIMUM:
            if(match_count > 0)
            {
              event_collector.dispatch("CLIENT%%" + tags + "%%" + filename + ": " + match_minimum, "FILE");
            }
            break;

          case MAXIMUM:
            if(match_count > 0)
            {
              event_collector.dispatch("CLIENT%%" + tags + "%%" + filename + ": " + match_maximum, "FILE");
            }
            break;
        }
      }

      match_count = 0;
      match_total = 0;
      match_minimum = 999999999999999999L;
      match_maximum = 0;
      next_checkpoint = (new Date()).getTime() + metrics_interval;
    }
  }
}
