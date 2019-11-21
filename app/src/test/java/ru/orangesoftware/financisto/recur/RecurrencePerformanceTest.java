package ru.orangesoftware.financisto.recur;

import android.util.Log;

import java.util.Date;
import java.util.List;

import ru.orangesoftware.financisto.test.DateTime;

import static ru.orangesoftware.financisto.test.DateTime.date;
import static org.junit.Assert.*;

public class RecurrencePerformanceTest {

    public void test_should_generate_scheduled_times_for_specific_period() throws Exception {
        String dailyPattern = "2011-08-02T21:40:00~DAILY:interval@1#~INDEFINETELY:null";
        generateDates(dailyPattern, date(2011, 8, 1));
        generateDates(dailyPattern, date(2011, 9, 2));
        generateDates(dailyPattern, date(2011, 12, 2));
        generateDates(dailyPattern, date(2012, 9, 2));
        generateDates(dailyPattern, date(2014, 9, 2));
        generateDates(dailyPattern, date(2016, 9, 2));
    }

    private List<Date> generateDates(String pattern, DateTime date) {
        long start = date.atMidnight().asLong();
        long end = date.atDayEnd().asLong();
        long t0 = System.currentTimeMillis();
        try {
            Recurrence r = Recurrence.parse(pattern);
            return r.generateDates(new Date(start), new Date(end));
        } finally {
            long t1 = System.currentTimeMillis();
            Log.i("RecurrencePerformanceTest", "Generated " + start + "-" + end + ": " + (t1 - t0) + "ms");
        }
    }

}
