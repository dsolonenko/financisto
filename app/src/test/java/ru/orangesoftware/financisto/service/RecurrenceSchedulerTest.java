package ru.orangesoftware.financisto.service;

import android.content.Context;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import ru.orangesoftware.financisto.db.AbstractDbTest;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.RestoredTransaction;
import ru.orangesoftware.financisto.model.TransactionInfo;
import ru.orangesoftware.financisto.test.DateTime;

import static org.junit.Assert.*;

public class RecurrenceSchedulerTest extends AbstractDbTest {

    private static final long ONE_DAY_MS = 1 * 24 * 60 * 60 * 1000L;

    @Test
    public void shouldRestoreMissedOneTimeSchedule() throws Exception {
        assertRestored(Arrays.asList(yesterday()), System.currentTimeMillis(), 1);
    }

    @Test
    public void shouldRestoreMissedRecurrenceSchedule() throws Exception {
        assertRestored(Arrays.asList(everyDay()), fifthOfOctober(2010), 2, 2, 2, 2, 2);
    }

    @Test
    public void should_not_restore_missed_schedule_the_same_day() throws Exception {
        TransactionInfo t = everyDayAtNoon();
        t.lastRecurrence = DateTime.date(2010, 10, 2).atNoon().asLong();
        assertRestored(Arrays.asList(t), DateTime.date(2010, 10, 2).at(13, 0, 0, 0).asLong());
    }

    @Test
    public void shouldRestoreMissedSchedules() throws Exception {
        assertRestored(Arrays.asList(on4thOfOctober(2010), everyDay(), every2Days()), fifthOfOctober(2010), 1, 2, 2, 2, 2, 2, 3, 3);
    }

    @Test
    public void shouldRestoreNoMoreThan1000MissedSchedules() throws Exception {
        assertRestoredSize(Arrays.asList(everyDay()), fifthOfOctober(2013), 1000);
    }

    private long fifthOfOctober(int year) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 5);
        c.set(Calendar.MONTH, Calendar.OCTOBER);
        c.set(Calendar.YEAR, year);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private void assertRestored(List<TransactionInfo> schedules, long now, long... ids) {
        List<RestoredTransaction> restored = assertRestoredSize(schedules, now, ids.length);
        for (int i = 0; i < ids.length; i++) {
            assertEquals(ids[i], restored.get(i).transactionId);
        }
    }

    private List<RestoredTransaction> assertRestoredSize(List<TransactionInfo> schedules, long now, int count) {
        // given
        DatabaseAdapter db = new FakeDatabaseAdapter(getContext(), new ArrayList<TransactionInfo>(schedules));
        RecurrenceScheduler scheduler = new RecurrenceScheduler(db);
        // when
        List<RestoredTransaction> missed = scheduler.getMissedSchedules(now);
        // then
        assertNotNull(missed);
        assertEquals(count, missed.size());
        return missed;
    }

    private TransactionInfo yesterday() {
        TransactionInfo t = new TransactionInfo();
        t.id = 1;
        t.recurrence = null;
        t.dateTime = System.currentTimeMillis() - ONE_DAY_MS;
        return t;
    }

    private TransactionInfo on4thOfOctober(int year) {
        TransactionInfo t = new TransactionInfo();
        t.id = 1;
        t.recurrence = null;
        t.dateTime = fifthOfOctober(year) - ONE_DAY_MS;
        return t;
    }

    private TransactionInfo everyDay() {
        TransactionInfo t = new TransactionInfo();
        t.id = 2;
        t.recurrence = "2010-10-01T00:00:00~DAILY:interval@1#~INDEFINETELY:null";
        t.lastRecurrence = 1;
        return t;
    }

    private TransactionInfo everyDayAtNoon() {
        TransactionInfo t = new TransactionInfo();
        t.id = 2;
        t.recurrence = "2010-10-01T12:00:00~DAILY:interval@1#~INDEFINETELY:null";
        t.lastRecurrence = 1;
        return t;
    }

    private TransactionInfo every2Days() {
        TransactionInfo t = new TransactionInfo();
        t.id = 3;
        t.recurrence = "2010-10-01T00:10:00~DAILY:interval@2#~EXACTLY_TIMES:count@3#";
        t.lastRecurrence = 1;
        return t;
    }

    static class FakeDatabaseAdapter extends DatabaseAdapter {

        private final ArrayList<TransactionInfo> scheduledTransactions;

        FakeDatabaseAdapter(Context context, ArrayList<TransactionInfo> scheduled) {
            super(context);
            this.scheduledTransactions = scheduled;
        }

        @Override
        public ArrayList<TransactionInfo> getAllScheduledTransactions() {
            return scheduledTransactions;
        }
    }

}
