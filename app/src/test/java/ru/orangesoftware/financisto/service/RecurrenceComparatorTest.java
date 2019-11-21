package ru.orangesoftware.financisto.service;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import ru.orangesoftware.financisto.model.TransactionInfo;

import static org.junit.Assert.*;

public class RecurrenceComparatorTest {

    RecurrenceScheduler.RecurrenceComparator comparator;

    /**
     * Correct order by nextDateTime:
     * 2010-12-01
     * 2010-12-02
     * 2010-11-23 <- today
     * 2010-11-11
     * 2010-10-08
     * NULL
     */
    public void testShouldCheckCorrectOrderOfSortedSchedules() {
        // given
        long today = date(2010, 11, 23).getTime();
        comparator = new RecurrenceScheduler.RecurrenceComparator(today);
        TransactionInfo[] transactions = {
                create(date(2010, 11, 11)),
                create(date(2010, 10, 8)),
                null,
                create(date(2010, 12, 1)),
                create(date(2010, 12, 2)),
                create(null),
                create(null)
        };
        // when
        Arrays.sort(transactions, comparator);
        // then
        assertEquals(date(2010, 12, 1), transactions[0].nextDateTime);
        assertEquals(date(2010, 12, 2), transactions[1].nextDateTime);
        assertEquals(date(2010, 11, 11), transactions[2].nextDateTime);
        assertEquals(date(2010, 10, 8), transactions[3].nextDateTime);
    }

    private TransactionInfo create(Date nextDateTime) {
        TransactionInfo ti = new TransactionInfo();
        ti.nextDateTime = nextDateTime;
        return ti;
    }

    private Date date(int y, int m, int d) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, y);
        c.set(Calendar.MONTH, m - 1);
        c.set(Calendar.DAY_OF_MONTH, d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

}
