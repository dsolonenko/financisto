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
package ru.orangesoftware.financisto.recur;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import ru.orangesoftware.financisto.recur.RecurrenceViewFactory.DayOfWeek;
import ru.orangesoftware.financisto.recur.RecurrenceViewFactory.MonthlyPattern;
import ru.orangesoftware.financisto.recur.RecurrenceViewFactory.SpecificDayPostfix;
import ru.orangesoftware.financisto.recur.RecurrenceViewFactory.SpecificDayPrefix;

import com.google.ical.values.Frequency;
import com.google.ical.values.RRule;
import com.google.ical.values.Weekday;
import com.google.ical.values.WeekdayNum;

public class RecurrencePattern {
	
	private static final LinkedList<WeekdayNum> WEEKDAYS = new LinkedList<WeekdayNum>();
	private static final LinkedList<WeekdayNum> WEEKENDS = new LinkedList<WeekdayNum>();
	
	static {
		WEEKDAYS.add(new WeekdayNum(0, Weekday.MO));
		WEEKDAYS.add(new WeekdayNum(0, Weekday.TU));
		WEEKDAYS.add(new WeekdayNum(0, Weekday.WE));
		WEEKDAYS.add(new WeekdayNum(0, Weekday.TH));
		WEEKDAYS.add(new WeekdayNum(0, Weekday.FR));		
		WEEKENDS.add(new WeekdayNum(0, Weekday.SU));
		WEEKENDS.add(new WeekdayNum(0, Weekday.SA));
	}
	
	public final RecurrenceFrequency frequency;
	public final String params;

	public RecurrencePattern(RecurrenceFrequency frequency, String params) {
		this.frequency = frequency;
		this.params = params;
	}

	public static RecurrencePattern parse(String recurrencePattern) {
		String[] a = recurrencePattern.split(":");
		return new RecurrencePattern(RecurrenceFrequency.valueOf(a[0]), a[1]);
	}

	public static RecurrencePattern noRecur() {
		return new RecurrencePattern(RecurrenceFrequency.NO_RECUR, null);
	}

	public static RecurrencePattern empty(RecurrenceFrequency frequency) {
		return new RecurrencePattern(frequency, null);
	}

	public void updateRRule(RRule r) {
		HashMap<String, String> state = RecurrenceViewFactory.parseState(params);
		int interval = Integer.parseInt(state.get(RecurrenceViewFactory.P_INTERVAL));
		r.setInterval(interval);			
		switch (frequency) {
		case DAILY:
			r.setFreq(Frequency.DAILY);
			break;
		case WEEKLY:
			r.setFreq(Frequency.WEEKLY);
			LinkedList<WeekdayNum> byDay = new LinkedList<WeekdayNum>();
			String days = state.get(RecurrenceViewFactory.P_DAYS);
			String[] a = days.split(",");
			for (String s : a) {
				DayOfWeek d = DayOfWeek.valueOf(s);  
				byDay.add(new WeekdayNum(0, Weekday.valueOf(d.rfcName)));
			}
			r.setByDay(byDay);
			break;
		case MONTHLY:
			r.setFreq(Frequency.MONTHLY);
			//int count = Integer.parseInt(state.get(RecurrenceViewFactory.P_COUNT));
			MonthlyPattern pattern = MonthlyPattern.valueOf(state.get(RecurrenceViewFactory.P_MONTHLY_PATTERN+"_0"));
			switch (pattern) {
			case EVERY_NTH_DAY:
				int everyNthDay = Integer.parseInt(state.get(RecurrenceViewFactory.P_MONTHLY_PATTERN_PARAMS+"_0"));
				r.setByMonthDay(new int[]{everyNthDay});
				break;
			case SPECIFIC_DAY:
				String s = state.get(RecurrenceViewFactory.P_MONTHLY_PATTERN_PARAMS+"_0");
				String[] x = s.split("-");
				SpecificDayPrefix prefix = SpecificDayPrefix.valueOf(x[0]);
				SpecificDayPostfix postfix = SpecificDayPostfix.valueOf(x[1]);
				int num = prefix == SpecificDayPrefix.LAST ? -1 : prefix.ordinal()+1;
				switch (postfix) {
					case DAY:
						r.setByMonthDay(new int[]{num});
						break;
					case WEEKDAY:
						r.setByDay(WEEKDAYS);							
						r.setBySetPos(new int[]{num});
						break;
					case WEEKEND_DAY:
						r.setByDay(WEEKENDS);
						r.setBySetPos(new int[]{num});
						break;
					default:
						//su-sa
						Weekday day = Weekday.values()[postfix.ordinal()-3];
						r.setByDay(Collections.singletonList(new WeekdayNum(num, day)));
					break;
				}
				break;
			}

			break;
		default:
			break;
		}
	}

	public Object stateToString() {
		return frequency.name()+":"+params;
	}

}

