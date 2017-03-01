/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.filter;

import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.datetime.DateUtils;
import ru.orangesoftware.financisto.datetime.Period;
import ru.orangesoftware.financisto.datetime.PeriodType;

import java.util.Calendar;

/**
* Created by IntelliJ IDEA.
* User: denis.solonenko
* Date: 12/17/12 9:06 PM
*/
public class DateTimeCriteria extends Criteria {

    public static final long START_OF_ERA;
    public static final long END_OF_ERA;

    static {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, 1970);
        c.set(Calendar.MONTH, 1);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        START_OF_ERA = c.getTimeInMillis();
        c.set(Calendar.YEAR, 2025);
        c.set(Calendar.MONTH, 12);
        c.set(Calendar.DAY_OF_MONTH, 31);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        END_OF_ERA = c.getTimeInMillis();
    }

    private final Period period;

    public DateTimeCriteria(Period period) {
        super(BlotterFilter.DATETIME, WhereFilter.Operation.BTW, String.valueOf(period.start), String.valueOf(period.end));
        this.period = period;
    }

    public DateTimeCriteria(PeriodType period) {
        this(DateUtils.getPeriod(period));
    }

    public DateTimeCriteria(long start, long end) {
        this(new Period(PeriodType.CUSTOM, start, end));
    }

    public String toStringExtra() {
        StringBuilder sb = new StringBuilder();
        sb.append(BlotterFilter.DATETIME).append(",#,");
        sb.append(period.type.name());
        if (period.isCustom()) {
            sb.append(",");
            sb.append(period.start).append(",");
            sb.append(period.end);
        }
        return sb.toString();
    }

    public static Criteria fromStringExtra(String extra) {
        String[] a = extra.split(",");
        if ("#".equals(a[1])) {
            // new format support
            PeriodType period = PeriodType.valueOf(a[2]);
            if (period == PeriodType.CUSTOM) {
                return new DateTimeCriteria(new Period(PeriodType.CUSTOM, Long.parseLong(a[3]), Long.parseLong(a[4])));
            } else {
                return new DateTimeCriteria(DateUtils.getPeriod(period));
            }
        } else {
            // legacy support
            WhereFilter.Operation op = WhereFilter.Operation.valueOf(a[1]);
            if (op == WhereFilter.Operation.GTE) {
                return new DateTimeCriteria(new Period(PeriodType.CUSTOM, Long.parseLong(a[2]), END_OF_ERA));
            } else if (op == WhereFilter.Operation.LTE) {
                return new DateTimeCriteria(new Period(PeriodType.CUSTOM, START_OF_ERA, Long.parseLong(a[2])));
            } else if (a.length > 3) {
                return new DateTimeCriteria(new Period(PeriodType.CUSTOM, Long.parseLong(a[2]), Long.parseLong(a[3])));
            } else {
                return new DateTimeCriteria(DateUtils.getPeriod(PeriodType.THIS_MONTH));
            }
        }
    }

    public Period getPeriod() {
        return period;
    }

}
