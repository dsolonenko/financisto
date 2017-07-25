package ru.orangesoftware.financisto.recur;

import com.google.ical.iter.RecurrenceIterator;
import com.google.ical.iter.RecurrenceIteratorFactory;
import com.google.ical.values.RRule;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import static ru.orangesoftware.financisto.recur.RecurrencePeriod.dateToDateValue;
import static ru.orangesoftware.financisto.recur.RecurrencePeriod.dateValueToDate;

public class DateRecurrenceIterator {

	private final RecurrenceIterator ri;
    private Date firstDate;

	private DateRecurrenceIterator(RecurrenceIterator ri) {
		this.ri = ri;
	}

	public boolean hasNext() {
		return firstDate != null || ri.hasNext();
	}

	public Date next() {
        if (firstDate != null) {
            Date date = firstDate;
            firstDate = null;
            return date;
        }
		return dateValueToDate(ri.next());
	}

	public static DateRecurrenceIterator create(RRule rrule, Date nowDate, Date startDate) throws ParseException {
        RecurrenceIterator ri = RecurrenceIteratorFactory.createRecurrenceIterator(rrule,
                dateToDateValue(startDate), Calendar.getInstance().getTimeZone());
        Date date = null;
        while (ri.hasNext() && (date = dateValueToDate(ri.next())).before(nowDate));
        //ri.advanceTo(dateToDateValue(nowDate));
        DateRecurrenceIterator iterator = new DateRecurrenceIterator(ri);
        iterator.firstDate = date;
        return iterator;
	}

    public static DateRecurrenceIterator empty() {
        return new EmptyDateRecurrenceIterator();
    }

    private static class EmptyDateRecurrenceIterator extends DateRecurrenceIterator {
        public EmptyDateRecurrenceIterator() {
            super(null);
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Date next() {
            return null;
        }
    }
}
