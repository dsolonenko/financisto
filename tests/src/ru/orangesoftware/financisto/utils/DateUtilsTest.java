/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.utils;

import android.test.AndroidTestCase;
import ru.orangesoftware.financisto.datetime.Period;
import ru.orangesoftware.financisto.datetime.PeriodType;
import ru.orangesoftware.financisto.test.DateTime;

import static ru.orangesoftware.financisto.test.DateTime.date;
import static ru.orangesoftware.financisto.datetime.PeriodType.*;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 8/28/12 11:39 PM
 */
public class DateUtilsTest extends AndroidTestCase {

    long refTime;

    public void test_should_support_different_periods_for_blotter_filter() {
        givenRefTime(date(2012, 8, 28).at(23, 52, 0, 0));
        assertPeriod(TODAY, date(2012, 8, 28), date(2012, 8, 28));
        assertPeriod(YESTERDAY, date(2012, 8, 27), date(2012, 8, 27));
        assertPeriod(THIS_WEEK, date(2012, 8, 27), date(2012, 9, 2));
        assertPeriod(THIS_MONTH, date(2012, 8, 1), date(2012, 8, 31));
        assertPeriod(LAST_WEEK, date(2012, 8, 20), date(2012, 8, 26));
        assertPeriod(LAST_MONTH, date(2012, 7, 1), date(2012, 7, 31));
        assertPeriod(THIS_AND_LAST_WEEK, date(2012, 8, 20), date(2012, 9, 2));
        assertPeriod(THIS_AND_LAST_MONTH, date(2012, 7, 1), date(2012, 8, 31));

        assertPeriod(TOMORROW, date(2012, 8, 29), date(2012, 8, 29));
        assertPeriod(NEXT_WEEK, date(2012, 9, 3), date(2012, 9, 9));
        assertPeriod(NEXT_MONTH, date(2012, 9, 1), date(2012, 9, 30));
        assertPeriod(THIS_AND_NEXT_MONTH, date(2012, 8, 1), date(2012, 9, 30));
        assertPeriod(NEXT_3_MONTHS, date(2012, 8, 1), date(2012, 10, 31));
    }

    private void givenRefTime(DateTime dateTime) {
        refTime = dateTime.asLong();
    }

    private void assertPeriod(PeriodType periodType, DateTime start, DateTime end) {
        Period period = periodType.calculatePeriod(refTime);
        assertEquals(start.atMidnight().asLong(), period.start);
        assertEquals(end.atDayEnd().asLong(), period.end);
    }

}
