/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.recur;

import android.test.AndroidTestCase;
import android.util.Log;
import ru.orangesoftware.financisto.test.DateTime;

import java.util.Date;
import java.util.List;

import static ru.orangesoftware.financisto.test.DateTime.date;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/18/11 7:15 PM
 */
public class RecurrencePerformanceTest extends AndroidTestCase {

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
            Log.i("RecurrencePerformanceTest", "Generated "+start+"-"+end+": "+(t1-t0)+"ms");
        }
    }

}
