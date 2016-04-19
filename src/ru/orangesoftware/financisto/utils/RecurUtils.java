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
package ru.orangesoftware.financisto.utils;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.datetime.DateUtils;
import ru.orangesoftware.financisto.datetime.Period;
import ru.orangesoftware.financisto.datetime.PeriodType;
import android.content.Context;

public class RecurUtils {

	private static final long DAY_IN_MS = 24*60*60*1000L;	

	public interface Layoutable {
		int getLayoutId();
	}
	
	public enum RecurInterval implements Layoutable, LocalizableEnum {
		NO_RECUR(0, R.string.recur_interval_no_recur),
		EVERY_X_DAY(R.layout.recur_every_x_day, R.string.recur_interval_every_x_day),
		DAILY(0, R.string.recur_interval_daily),
		WEEKLY(R.layout.recur_weekly, R.string.recur_interval_weekly){
			@Override
			public Period next(long startDate) {
				Calendar c = Calendar.getInstance();
				c.setTimeInMillis(startDate);
				startDate = DateUtils.startOfDay(c).getTimeInMillis();
				c.add(Calendar.DAY_OF_MONTH, 6);
				long endDate = DateUtils.endOfDay(c).getTimeInMillis();
				return new Period(PeriodType.CUSTOM, startDate, endDate);
			}
		},		
		MONTHLY(0, R.string.recur_interval_monthly){
			@Override
			public Period next(long startDate) {
				Calendar c = Calendar.getInstance();
				c.setTimeInMillis(startDate);
				startDate = DateUtils.startOfDay(c).getTimeInMillis();
				c.add(Calendar.MONTH, 1);
				c.add(Calendar.DAY_OF_MONTH, -1);
				long endDate = DateUtils.endOfDay(c).getTimeInMillis();
				return new Period(PeriodType.CUSTOM, startDate, endDate);
			}
		},
		SEMI_MONTHLY(R.layout.recur_semi_monthly, R.string.recur_interval_semi_monthly),
		YEARLY(0, R.string.recur_interval_yearly);		
		
		private final int layoutId;
		private final int titleId;
		
		private RecurInterval(int layoutId, int titleId) {
			this.layoutId = layoutId;
			this.titleId = titleId;
		}

		@Override
		public int getLayoutId() {
			return layoutId;
		}
		
		@Override
		public int getTitleId() {
			return titleId;
		}

		public Period next(long startDate) {
			throw new UnsupportedOperationException();
		}
	}

	public enum RecurPeriod implements Layoutable, LocalizableEnum {
		STOPS_ON_DATE(R.layout.recur_stops_on_date, R.string.recur_stops_on_date){
			@Override
			public String toSummary(Context context, long param) {
				DateFormat df = DateUtils.getShortDateFormat(context);
				return String.format(context.getString(R.string.recur_stops_on_date_summary), df.format(new Date(param)));
			}			
			@Override
			public Period[] repeat(RecurInterval interval, long startDate, long periodParam) {
				long endDate = 0;
				LinkedList<Period> periods = new LinkedList<Period>();
				while (endDate < periodParam) {
					Period p = interval.next(startDate);
					startDate = p.end+1;
					endDate = p.end;
					periods.add(p);
				}
				return periods.toArray(new Period[periods.size()]);
			}
		},
//		INDEFINETELY(0, R.string.recur_indefinitely){
//			@Override
//			public String toSummary(Context context, long param) {
//				return context.getString(R.string.recur_indefinitely);
//			}
//		},
		EXACTLY_TIMES(R.layout.recur_exactly_n_times, R.string.recur_exactly_n_times){
			@Override
			public String toSummary(Context context, long param) {
				return String.format(context.getString(R.string.recur_exactly_n_times_summary), param);
			}
			@Override
			public Period[] repeat(RecurInterval interval, long startDate, long periodParam) {
				LinkedList<Period> periods = new LinkedList<Period>();
				while (periodParam-- > 0) {
					Period p = interval.next(startDate);
					startDate = p.end+1;
					periods.add(p);
				}
				return periods.toArray(new Period[periods.size()]);
			}
		};	

		private final int layoutId;
		private final int titleId;
		
		private RecurPeriod(int layoutId, int titleId) {
			this.layoutId = layoutId;
			this.titleId = titleId;
		}

		@Override
		public int getLayoutId() {
			return layoutId;
		}

		@Override
		public int getTitleId() {
			return titleId;
		}
		
		public abstract String toSummary(Context context, long param);

		public abstract Period[] repeat(RecurInterval interval, long startDate, long periodParam);
	
	}

	public static abstract class Recur implements Cloneable {
		
		public final RecurInterval interval;
		public long startDate;
		public RecurPeriod period;
		public long periodParam;
		
		protected Recur(RecurInterval interval, HashMap<String, String> values) {
			this.interval = interval;
			this.startDate = getLong(values, "startDate");
			this.period = RecurPeriod.valueOf(values.get("period"));
			this.periodParam = getLong(values, "periodParam");
		}
		
		public Recur(RecurInterval interval) {
			this.interval = interval;
			this.startDate = System.currentTimeMillis();
			this.period = RecurPeriod.EXACTLY_TIMES;
			this.periodParam = 1;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(interval.name()).append(",");
			sb.append("startDate=").append(startDate).append(",");
			sb.append("period=").append(period.name()).append(",");
			sb.append("periodParam=").append(periodParam).append(",");
			toString(sb);
			return sb.toString();
		}

		protected void toString(StringBuilder sb) {
			// do nothing
		}

		@Override
		public Recur clone() {
			try {
				return (Recur)super.clone();
			} catch (CloneNotSupportedException e) {
				throw new RuntimeException(e);
			}
		}

		public String toString(Context context) {
			DateFormat df = DateUtils.getShortDateFormat(context);
			StringBuilder sb = new StringBuilder();
			sb.append(context.getString(R.string.recur_repeat_starts_on)).append(" ");
			sb.append(df.format(new Date(startDate))).append(", ");
			sb.append(context.getString(interval.titleId)).append(", ");
			sb.append(period.toSummary(context, periodParam));
			return sb.toString();
		}
		
		//public abstract long getNextRecur(long currentDate);

	}
	
	public static class NoRecur extends Recur {
		
		protected NoRecur(HashMap<String, String> values) {
			super(RecurInterval.NO_RECUR, values);
		}
				
		public NoRecur() {
			super(RecurInterval.NO_RECUR);
		}
	}

	public static class EveryXDay extends Recur {
		
		public int days;

		protected EveryXDay(HashMap<String, String> values) {
			super(RecurInterval.EVERY_X_DAY, values);
			this.days = getInt(values, "days");
		}
		
		public EveryXDay() {
			super(RecurInterval.EVERY_X_DAY);
			this.days = 1;
		}

		@Override
		protected void toString(StringBuilder sb) {
			sb.append("days=").append(days);
		}
		
		//@Override
		public long getNextRecur(long currentDate) {
			if (currentDate <= startDate) {
				return startDate;
			}
			long period = days*DAY_IN_MS;
			long delta = currentDate - startDate;
			long n = delta/period;
			long next = n*period;
			if (next > currentDate) {
				return next;
			} else {
				return next+period;
			}
		}
				
	}
	
	public static class Daily extends Recur {
		
		protected Daily(HashMap<String, String> values) {
			super(RecurInterval.DAILY, values);
		}
				
		public Daily() {
			super(RecurInterval.DAILY);
		}
	}

	public static enum DayOfWeek {
		SUN(R.id.daySun), 
		MON(R.id.dayMon), 
		TUE(R.id.dayTue), 
		WED(R.id.dayWed), 
		THR(R.id.dayThr), 
		FRI(R.id.dayFri), 
		SAT(R.id.daySat);
		
		public final int checkboxId;
		
		private DayOfWeek(int checkboxId) {
			this.checkboxId = checkboxId;
		}
		
	}
	
	public static class Weekly extends Recur {
		
		private final EnumSet<DayOfWeek> days;
		
		protected Weekly(HashMap<String, String> values) {
			super(RecurInterval.WEEKLY, values);
			this.days = EnumSet.noneOf(DayOfWeek.class);
			String s = values.get("days");
			if (!Utils.isEmpty(s)) {
				String[] a = s.split(",");
				for (String d : a) {
					days.add(DayOfWeek.valueOf(d));
				}
			}
		}
		
		public Weekly() {
			super(RecurInterval.WEEKLY);
			this.days = EnumSet.allOf(DayOfWeek.class);
			days.remove(DayOfWeek.SAT);
			days.remove(DayOfWeek.SUN);
		}
		
		@Override
		protected void toString(StringBuilder sb) {
			sb.append("days=");
			boolean appendComma = false; 
			for (DayOfWeek d : days) {
				if (appendComma) {
					sb.append(",");
				}
				sb.append(d.name());
				appendComma = true;
			}
		}

		public boolean isSet(DayOfWeek d) {
			return days.contains(d);
		}
		
		public void set(DayOfWeek d) {
			days.add(d);
		}
				
		public void unset(DayOfWeek d) {
			days.remove(d);
		}
	}

	public static class SemiMonthly extends Recur {
		
		public int firstDay;
		public int secondDay;
		
		protected SemiMonthly(HashMap<String, String> values) {
			super(RecurInterval.SEMI_MONTHLY, values);
			this.firstDay = getInt(values, "firstDay"); 
			this.secondDay = getInt(values, "secondDay");
		}

		public SemiMonthly() {
			super(RecurInterval.SEMI_MONTHLY);
			this.firstDay = 15;
			this.secondDay = 30;
		}

		@Override
		protected void toString(StringBuilder sb) {
			sb.append("firstDay=").append(firstDay).append(",");
			sb.append("secondDay=").append(secondDay).append(",");
		}
		
	}
 
	public static class Monthly extends Recur {
		
		protected Monthly(HashMap<String, String> values) {
			super(RecurInterval.MONTHLY, values);
		}
				
		public Monthly() {
			super(RecurInterval.MONTHLY);
		}

	}

	public static class Yearly extends Recur {
		
		protected Yearly(HashMap<String, String> values) {
			super(RecurInterval.YEARLY, values);
		}
				
		public Yearly() {
			super(RecurInterval.YEARLY);
		}
	}

	public static Recur createFromExtraString(String extra) {
		if (Utils.isEmpty(extra)) {
			return new NoRecur();
		}
		String[] a = extra.split(",");
		RecurInterval interval = RecurInterval.valueOf(a[0]);
		HashMap<String, String> values = toMap(a);
		switch (interval) {
		case NO_RECUR:
			return new NoRecur(values); 
		case EVERY_X_DAY:
			return new EveryXDay(values); 
		case DAILY:
			return new Daily(values);
		case WEEKLY:
			return new Weekly(values);
		case SEMI_MONTHLY:
			return new SemiMonthly(values);
		case MONTHLY:
			return new Monthly(values);
		case YEARLY:
			return new Yearly(values);
		}
		return null;
	}
	
	public static Recur createRecur(RecurInterval interval) {
		
		switch (interval) {
		case NO_RECUR:
			return new NoRecur(); 
		case EVERY_X_DAY:
			return new EveryXDay(); 
		case DAILY:
			return new Daily();
		case WEEKLY:
			return new Weekly();
		case SEMI_MONTHLY:
			return new SemiMonthly();
		case MONTHLY:
			return new Monthly();
		case YEARLY:
			return new Yearly();
		}
		return null;
	}

	private static HashMap<String, String> toMap(String[] a) {
		HashMap<String, String> map = new HashMap<String, String>();
		for (String s : a) {
			String[] kv = s.split("=");
			if (kv.length > 1) {
				map.put(kv[0], kv[1]);
			}
		}
		return map;
	}
	
	private static long getLong(HashMap<String, String> values, String string) {
		return Long.parseLong(values.get(string));
	}

	private static int getInt(HashMap<String, String> values, String string) {
		return Integer.parseInt(values.get(string));
	}


	public static Recur createDefaultRecur() {
		Calendar c = Calendar.getInstance();
		NoRecur m = new NoRecur();
		m.startDate = DateUtils.startOfDay(c).getTimeInMillis();
		m.period = RecurPeriod.STOPS_ON_DATE;
		c.add(Calendar.MONTH, 1);
		m.periodParam = DateUtils.endOfDay(c).getTimeInMillis();
		return m;
	}

	public static Period[] periods(Recur recur) {
		RecurInterval interval = recur.interval;
		RecurPeriod period = recur.period;
		if (interval == RecurInterval.NO_RECUR) {
			if (period != RecurPeriod.STOPS_ON_DATE) {
                return new Period[]{PeriodType.THIS_MONTH.calculatePeriod()};
			}
			return new Period[]{new Period(PeriodType.CUSTOM, recur.startDate, recur.periodParam)};
		} else {
			return period.repeat(interval, recur.startDate, recur.periodParam);
		}		
	}

}
