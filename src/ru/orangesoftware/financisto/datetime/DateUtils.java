/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.datetime;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.provider.Settings;

public class DateUtils {

	public static final DateFormat FORMAT_TIMESTAMP_ISO_8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	public static final DateFormat FORMAT_DATE_ISO_8601 = new SimpleDateFormat("yyyy-MM-dd");
	public static final DateFormat FORMAT_TIME_ISO_8601 = new SimpleDateFormat("HH:mm:ss");
	public static final DateFormat FORMAT_DATE_RFC_2445 = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

    public static Period getPeriod(PeriodType period) {
        return period.calculatePeriod();
	}

	public static Calendar startOfDay(Calendar c) {
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		return c;
	}

	public static Calendar endOfDay(Calendar c) {
		c.set(Calendar.HOUR_OF_DAY, 23);
		c.set(Calendar.MINUTE, 59);
		c.set(Calendar.SECOND, 59);
		c.set(Calendar.MILLISECOND, 999);
		return c;
	}

    public static long atMidnight(long date) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(date);
        return startOfDay(c).getTimeInMillis();
    }

    public static long atDayEnd(long date) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(date);
        return endOfDay(c).getTimeInMillis();
    }

	public static Date atDateAtTime(long now, Calendar startDate) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(now);
		c.set(Calendar.HOUR_OF_DAY, startDate.get(Calendar.HOUR_OF_DAY));
		c.set(Calendar.MINUTE, startDate.get(Calendar.MINUTE));
		c.set(Calendar.SECOND, startDate.get(Calendar.SECOND));
		c.set(Calendar.MILLISECOND, startDate.get(Calendar.MILLISECOND));
		return c.getTime();
	}

	public static DateFormat getShortDateFormat(Context context) {
		return android.text.format.DateFormat.getDateFormat(context);
	}

	public static DateFormat getLongDateFormat(Context context) {
		return android.text.format.DateFormat.getLongDateFormat(context);
	}

	public static DateFormat getMediumDateFormat(Context context) {
		return android.text.format.DateFormat.getMediumDateFormat(context);
	}

	public static DateFormat getTimeFormat(Context context) {
		return android.text.format.DateFormat.getTimeFormat(context);
	}

	public static boolean is24HourFormat(Context context) {
		return "24".equals(Settings.System.getString(context.getContentResolver(), Settings.System.TIME_12_24));
	}

	public static void zeroSeconds(Calendar dateTime) {
		dateTime.set(Calendar.SECOND, 0);
		dateTime.set(Calendar.MILLISECOND, 0);
	}
}
