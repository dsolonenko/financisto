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

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.ActivityLayout;
import ru.orangesoftware.financisto.activity.ActivityLayoutListener;
import ru.orangesoftware.financisto.activity.RecurrenceActivity;
import ru.orangesoftware.financisto.model.MultiChoiceItem;
import ru.orangesoftware.financisto.datetime.DateUtils;
import ru.orangesoftware.financisto.utils.EnumUtils;
import ru.orangesoftware.financisto.utils.LocalizableEnum;
import ru.orangesoftware.financisto.utils.Utils;
import ru.orangesoftware.financisto.view.NodeInflater;

import java.text.ParseException;
import java.util.*;

import static ru.orangesoftware.financisto.activity.UiUtils.applyTheme;

public class RecurrenceViewFactory {

    private final RecurrenceActivity activity;

    public RecurrenceViewFactory(RecurrenceActivity activity) {
        this.activity = activity;
    }

    public RecurrenceView create(RecurrencePattern p) {
        switch (p.frequency) {
            case DAILY:
                return new DailyView();
            case WEEKLY:
                return new WeeklyView();
            case MONTHLY:
                return new MonthlyView(p.params);
//		case SEMI_MONTHLY:
//			return new SemiMonthlyView();
            case GEEKY:
                return new GeekyView();
            default:
                return null;
        }
    }

    public RecurrenceView create(RecurrenceUntil r) {
        switch (r) {
            case EXACTLY_TIMES:
                return new ExactlyTimesView();
            case STOPS_ON_DATE:
                return new StopsOnDateView();
            default:
                return null;
        }
    }

    static HashMap<String, String> parseState(String state) {
        if (state == null) {
            return new HashMap<>();
        }
        HashMap<String, String> map = new HashMap<>();
        String[] a = state.split("#");
        for (String s : a) {
            String[] p = s.split("@");
            if (p.length == 2) {
                map.put(p[0], p[1]);
            }
        }
        return map;
    }

    abstract class AbstractView implements RecurrenceView, ActivityLayoutListener {

        private final LocalizableEnum r;
        protected final ActivityLayout x;

        AbstractView(LocalizableEnum r) {
            this.r = r;
            LayoutInflater layoutInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            NodeInflater nodeInflater = new NodeInflater(layoutInflater);
            this.x = new ActivityLayout(nodeInflater, this);
        }

        @Override
        public String stateToString() {
            StringBuilder sb = new StringBuilder();
            sb.append(r.name()).append(":");
            HashMap<String, String> state = new HashMap<>();
            stateToMap(state);
            for (Map.Entry<String, String> e : state.entrySet()) {
                sb.append(e.getKey()).append("@").append(e.getValue()).append("#");
            }
            return sb.toString();
        }

        @Override
        public void stateFromString(String state) {
            HashMap<String, String> map = parseState(state);
            stateFromMap(map);
        }

        @Override
        public abstract boolean validateState();

        protected abstract void stateToMap(HashMap<String, String> state);

        protected abstract void stateFromMap(HashMap<String, String> state);

        @Override
        public void onSelected(int id, List<? extends MultiChoiceItem> items) {
        }

        @Override
        public void onSelectedId(int id, long selectedId) {
        }

        @Override
        public void onSelectedPos(int id, int selectedPos) {
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            onClick(v, id);
        }

        protected abstract void onClick(View v, int id);

    }

    static final String P_INTERVAL = "interval";
    static final String P_DAYS = "days";
    static final String P_COUNT = "count";
    static final String P_DATE = "date";
    static final String P_MONTHLY_PATTERN = "monthly_pattern";
    static final String P_MONTHLY_PATTERN_PARAMS = "monthly_pattern_params";

    // ******************************************************************
    // DAILY
    // ******************************************************************
    private class GeekyView extends AbstractView {

        private final EditText geekyEditText = new EditText(activity);

        GeekyView() {
            super(RecurrenceFrequency.GEEKY);
            geekyEditText.setText("FREQ=MONTHLY;BYDAY=FR;BYMONTHDAY=13");
            geekyEditText.setMinLines(3);
            geekyEditText.setMaxLines(5);
        }

        @Override
        public void createNodes(LinearLayout layout) {
            removeAllViewsFromParent(geekyEditText);
            x.addEditNode(layout, R.string.recur_rrule, geekyEditText);
        }

        @Override
        protected void onClick(View v, int id) {
        }

        @Override
        public boolean validateState() {
            if (Utils.isEmpty(geekyEditText)) {
                geekyEditText.setError(activity.getString(R.string.specify_value));
                return false;
            }
            return true;
        }

        @Override
        protected void stateToMap(HashMap<String, String> state) {
            state.put(P_INTERVAL, geekyEditText.getText().toString().toUpperCase());
        }

        @Override
        protected void stateFromMap(HashMap<String, String> state) {
            String interval = state.get(P_INTERVAL);
            geekyEditText.setText(interval != null ? interval.toUpperCase() : "");
        }

    }

    // ******************************************************************
    // DAILY
    // ******************************************************************
    private class DailyView extends AbstractView {

        private final EditText repeatDaysEditText = numericEditText(activity);

        DailyView() {
            super(RecurrenceFrequency.DAILY);
            repeatDaysEditText.setText("1");
        }

        @Override
        public void createNodes(LinearLayout layout) {
            removeAllViewsFromParent(repeatDaysEditText);
            x.addEditNode(layout, R.string.recur_interval_every_x_day, repeatDaysEditText);
        }

        @Override
        protected void onClick(View v, int id) {
        }

        @Override
        public boolean validateState() {
            if (Utils.isEmpty(repeatDaysEditText)) {
                repeatDaysEditText.setError(activity.getString(R.string.specify_value));
                return false;
            }
            return true;
        }

        @Override
        protected void stateToMap(HashMap<String, String> state) {
            state.put(P_INTERVAL, repeatDaysEditText.getText().toString());
        }

        @Override
        protected void stateFromMap(HashMap<String, String> state) {
            repeatDaysEditText.setText(state.get(P_INTERVAL));
        }

    }

    // ******************************************************************
    // WEEKLY
    // ******************************************************************
    enum DayOfWeek implements LocalizableEnum {
        MON(R.id.dayMon, R.string.day_mon, "MO"),
        TUE(R.id.dayTue, R.string.day_tue, "TU"),
        WED(R.id.dayWed, R.string.day_wed, "WE"),
        THR(R.id.dayThr, R.string.day_thr, "TH"),
        FRI(R.id.dayFri, R.string.day_fri, "FR"),
        SAT(R.id.daySat, R.string.day_sat, "SA"),
        SUN(R.id.daySun, R.string.day_sun, "SU");

        public final int checkboxId;
        public final int titleId;
        public final String rfcName;

        DayOfWeek(int checkboxId, int titleId, String rfcName) {
            this.checkboxId = checkboxId;
            this.titleId = titleId;
            this.rfcName = rfcName;
        }

        @Override
        public int getTitleId() {
            return titleId;
        }

    }

    private class DayOfWeekItem implements MultiChoiceItem {

        public final DayOfWeek d;
        private final long id;
        private final String title;
        private boolean checked;

        DayOfWeekItem(DayOfWeek d) {
            this.d = d;
            this.id = d.checkboxId;
            this.title = activity.getString(d.titleId);
        }

        @Override
        public long getId() {
            return id;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public boolean isChecked() {
            return checked;
        }

        @Override
        public void setChecked(boolean checked) {
            this.checked = checked;
        }

    }

    private class WeeklyView extends AbstractView {

        private final EditText repeatWeeksEditText = numericEditText(activity);
        private TextView daysOfWeekText;

        private EnumSet<DayOfWeek> days = EnumSet.allOf(DayOfWeek.class);

        WeeklyView() {
            super(RecurrenceFrequency.WEEKLY);
            repeatWeeksEditText.setText("1");
            days.remove(DayOfWeek.SAT);
            days.remove(DayOfWeek.SUN);
        }

        private void updateDaysOfWeekText() {
            daysOfWeekText.setText(daysToString());
            daysOfWeekText.setError(null);
        }

        private String daysToString() {
            StringBuilder sb = new StringBuilder();
            for (DayOfWeek d : days) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(activity.getString(d.titleId));
            }
            if (sb.length() == 0) {
                sb.append(activity.getString(R.string.no_recur));
            }
            return sb.toString();
        }

        @Override
        public void createNodes(LinearLayout layout) {
            removeAllViewsFromParent(repeatWeeksEditText);
            x.addEditNode(layout, R.string.recur_interval_every_x_week, repeatWeeksEditText);
            daysOfWeekText = x.addListNode(layout, R.id.recurrence_pattern, R.string.recurrence_weekly_days, daysToString());
        }

        @Override
        protected void onClick(View v, int id) {
            if (id == R.id.recurrence_pattern) {
                ArrayList<MultiChoiceItem> items = new ArrayList<>();
                for (DayOfWeek d : DayOfWeek.values()) {
                    DayOfWeekItem i = new DayOfWeekItem(d);
                    i.setChecked(days.contains(d));
                    items.add(i);
                }
                x.selectMultiChoice(activity, R.id.recurrence_pattern, R.string.recur_interval_every_x_week, items);
            }
        }

        @Override
        public void onSelected(int id, List<? extends MultiChoiceItem> items) {
            if (id == R.id.recurrence_pattern) {
                days.clear();
                for (MultiChoiceItem i : items) {
                    DayOfWeekItem di = (DayOfWeekItem) i;
                    if (di.isChecked()) {
                        days.add(di.d);
                    }
                }
                updateDaysOfWeekText();
            }
        }

        @Override
        public boolean validateState() {
            if (Utils.isEmpty(repeatWeeksEditText)) {
                repeatWeeksEditText.setError(activity.getString(R.string.specify_value));
                return false;
            }
            if (days.isEmpty()) {
                daysOfWeekText.setError(activity.getString(R.string.specify_value));
                return false;
            }
            return true;
        }

        @Override
        protected void stateToMap(HashMap<String, String> state) {
            state.put(P_INTERVAL, repeatWeeksEditText.getText().toString());
            StringBuilder sb = new StringBuilder();
            for (DayOfWeek d : days) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(d.name());
            }
            state.put(P_DAYS, sb.toString());
        }

        @Override
        protected void stateFromMap(HashMap<String, String> state) {
            repeatWeeksEditText.setText(state.get(P_INTERVAL));
            String s = state.get(P_DAYS);
            String[] a = s.split(",");
            days.clear();
            for (String d : a) {
                days.add(DayOfWeek.valueOf(d));
            }
        }

    }

    // ******************************************************************
    // MONTHLY
    // ******************************************************************
    enum MonthlyPattern implements LocalizableEnum {
        EVERY_NTH_DAY(R.string.recurrence_monthly_every_nth_day),
        SPECIFIC_DAY(R.string.recurrence_monthly_specific_day);

        private final int titleId;

        MonthlyPattern(int titleId) {
            this.titleId = titleId;
        }

        @Override
        public int getTitleId() {
            return titleId;
        }

    }

    enum SpecificDayPrefix implements LocalizableEnum {
        FIRST(R.string.first), SECOND(R.string.second),
        THIRD(R.string.third), FOURTH(R.string.fourth),
        LAST(R.string.last);

        private final int titleId;

        SpecificDayPrefix(int titleId) {
            this.titleId = titleId;
        }

        @Override
        public int getTitleId() {
            return titleId;
        }
    }

    enum SpecificDayPostfix implements LocalizableEnum {
        DAY(R.string.day),
        WEEKDAY(R.string.weekday),
        WEEKEND_DAY(R.string.weekend_day),
        SUNDAY(R.string.sunday),
        MONDAY(R.string.monday),
        TUESDAY(R.string.tuesday),
        WEDNESDAY(R.string.wednesday),
        THURSDAY(R.string.thursday),
        FRIDAY(R.string.friday),
        SATURDAY(R.string.saturday);

        private final int titleId;

        SpecificDayPostfix(int titleId) {
            this.titleId = titleId;
        }

        @Override
        public int getTitleId() {
            return titleId;
        }
    }

    private static final int[] DAY_TITLES = {R.string.recur_interval_semi_monthly_1, R.string.recur_interval_semi_monthly_2};

    abstract class AbstractMonthlyView extends AbstractView {

        private int num;
        public final MonthlyPattern[] pattern;
        final SpecificDayPrefix[] prefix;
        final SpecificDayPostfix[] postfix;

        private final EditText repeatMonthsEditText = numericEditText(activity);
        private final EditText[] everyNthDayEditText;
        private final TextView[] patternText;
        private final TextView[] specificDayText;

        AbstractMonthlyView(RecurrenceFrequency frequency, int num) {
            super(frequency);
            this.num = num;
            pattern = new MonthlyPattern[num];
            for (int i = 0; i < num; i++) {
                pattern[i] = MonthlyPattern.EVERY_NTH_DAY;
            }
            prefix = new SpecificDayPrefix[num];
            for (int i = 0; i < num; i++) {
                prefix[i] = SpecificDayPrefix.FIRST;
            }
            postfix = new SpecificDayPostfix[num];
            for (int i = 0; i < num; i++) {
                postfix[i] = SpecificDayPostfix.DAY;
            }
            everyNthDayEditText = new EditText[num];
            for (int i = 0; i < num; i++) {
                everyNthDayEditText[i] = numericEditText(activity);
            }
            specificDayText = new TextView[num];
            patternText = new TextView[num];
            repeatMonthsEditText.setText("1");
        }

        @Override
        public void createNodes(LinearLayout layout) {
            int num = this.num;
            for (int i = 0; i < num; i++) {
                if (num > 1) {
                    x.addTitleNodeNoDivider(layout, DAY_TITLES[i]);
                }
                patternText[i] = x.addListNode(layout, 100 + i, R.string.recurrence_monthly_pattern, activity.getString(pattern[i].titleId));
                switch (pattern[i]) {
                    case EVERY_NTH_DAY:
                        removeAllViewsFromParent(everyNthDayEditText[i]);
                        x.addEditNode(layout, R.string.recurrence_monthly_every_nth_day, everyNthDayEditText[i]);
                        everyNthDayEditText[i].setText("15");
                        break;
                    case SPECIFIC_DAY:
                        specificDayText[i] = x.addListNode(layout, 200 + i, R.string.recurrence_monthly_specific_day, specificDayStr(i));
                        break;
                }
            }
            removeAllViewsFromParent(repeatMonthsEditText);
            x.addEditNode(layout, R.string.recur_interval_every_x_month, repeatMonthsEditText);
        }

        private String specificDayStr(int i) {
            return activity.getString(prefix[i].titleId) + " " + activity.getString(postfix[i].titleId);
        }

        @Override
        protected void onClick(View v, int id) {
            if (id > 199) {
                int k = id - 200;
                String[] prefixes = EnumUtils.getLocalizedValues(activity, SpecificDayPrefix.values());
                String[] postfixes = EnumUtils.getLocalizedValues(activity, SpecificDayPostfix.values());
                int prefixesLength = prefixes.length;
                int postfixesLength = postfixes.length;
                String[] items = new String[prefixesLength * postfixesLength];
                for (int i = 0; i < prefixesLength; i++) {
                    for (int j = 0; j < postfixesLength; j++) {
                        items[i * postfixesLength + j] = prefixes[i] + " " + postfixes[j];
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, items);
                int selected = prefix[k].ordinal() * postfixesLength + postfix[k].ordinal();
                x.selectPosition(activity, id, R.string.recurrence_period, adapter, selected);
            } else {
                int k = id - 100;
                ArrayAdapter<String> adapter = EnumUtils.createDropDownAdapter(activity, MonthlyPattern.values());
                x.selectPosition(activity, id, R.string.recurrence_period, adapter, pattern[k].ordinal());
            }
        }

        @Override
        public void onSelectedPos(int id, int selectedPos) {
            if (id > 199) {
                int k = id - 200;
                SpecificDayPrefix[] prefixes = SpecificDayPrefix.values();
                SpecificDayPostfix[] postfixes = SpecificDayPostfix.values();
                int postfixesLength = postfixes.length;
                int selectedPrefix = selectedPos / postfixesLength;
                int selectedPostfix = selectedPos - selectedPrefix * postfixesLength;
                prefix[k] = prefixes[selectedPrefix];
                postfix[k] = postfixes[selectedPostfix];
                specificDayText[k].setText(specificDayStr(k));
            } else {
                int k = id - 100;
                pattern[k] = MonthlyPattern.values()[selectedPos];
                activity.createNodes();
            }
        }

        @Override
        public boolean validateState() {
            int num = this.num;
            for (int i = 0; i < num; i++) {
                if (pattern[i] == MonthlyPattern.EVERY_NTH_DAY) {
                    if (Utils.isEmpty(everyNthDayEditText[i])) {
                        everyNthDayEditText[i].setError(activity.getString(R.string.specify_value));
                        return false;
                    }
                }
            }
            if (Utils.isEmpty(repeatMonthsEditText)) {
                repeatMonthsEditText.setError(activity.getString(R.string.specify_value));
                return false;
            }
            return true;
        }

        @Override
        protected void stateToMap(HashMap<String, String> state) {
            state.put(P_INTERVAL, repeatMonthsEditText.getText().toString());
            state.put(P_COUNT, String.valueOf(num));
            for (int i = 0; i < num; i++) {
                String pfx = "_" + i;
                state.put(P_MONTHLY_PATTERN + pfx, pattern[i].name());
                switch (pattern[i]) {
                    case EVERY_NTH_DAY:
                        state.put(P_MONTHLY_PATTERN_PARAMS + pfx, everyNthDayEditText[i].getText().toString());
                        break;
                    case SPECIFIC_DAY:
                        state.put(P_MONTHLY_PATTERN_PARAMS + pfx, prefix[i].name() + "-" + postfix[i].name());
                        break;
                }
            }
        }

        @Override
        protected void stateFromMap(HashMap<String, String> state) {
            repeatMonthsEditText.setText(state.get(P_INTERVAL));
            num = Integer.parseInt(state.get(P_COUNT));
            for (int i = 0; i < num; i++) {
                String pfx = "_" + i;
                pattern[i] = MonthlyPattern.valueOf(state.get(P_MONTHLY_PATTERN + pfx));
                patternText[i].setText(pattern[i].titleId);
                switch (pattern[i]) {
                    case EVERY_NTH_DAY:
                        everyNthDayEditText[i].setText(state.get(P_MONTHLY_PATTERN_PARAMS + pfx));
                        break;
                    case SPECIFIC_DAY:
                        String s = state.get(P_MONTHLY_PATTERN_PARAMS + pfx);
                        String[] a = s.split("-");
                        prefix[i] = SpecificDayPrefix.valueOf(a[0]);
                        postfix[i] = SpecificDayPostfix.valueOf(a[1]);
                        specificDayText[i].setText(specificDayStr(i));
                        break;
                }
            }
        }

    }

    private class MonthlyView extends AbstractMonthlyView {

        MonthlyView(String state) {
            super(RecurrenceFrequency.MONTHLY, 1);
            HashMap<String, String> map = parseState(state);
            if (!map.isEmpty()) {
                pattern[0] = MonthlyPattern.valueOf(map.get(P_MONTHLY_PATTERN + "_0"));
            }
        }

    }

    // ******************************************************************
    // SEMI-MONTHLY
    // ******************************************************************

//	class SemiMonthlyView extends AbstractMonthlyView {
//
//		public SemiMonthlyView() {
//			super(RecurrenceFrequency.SEMI_MONTHLY, 2);
//		}
//		
//	}

    // EXACTLY_TIMES
    private class ExactlyTimesView extends AbstractView {

        private final EditText countEditText = numericEditText(activity);

        ExactlyTimesView() {
            super(RecurrenceUntil.EXACTLY_TIMES);
            countEditText.setText("10");
        }

        @Override
        public void createNodes(LinearLayout layout) {
            removeAllViewsFromParent(countEditText);
            x.addEditNode(layout, R.string.recur_exactly_n_times, countEditText);
        }

        @Override
        protected void onClick(View v, int id) {
        }

        @Override
        public boolean validateState() {
            if (Utils.isEmpty(countEditText)) {
                countEditText.setError(activity.getString(R.string.specify_value));
                return false;
            }
            return true;
        }

        @Override
        protected void stateToMap(HashMap<String, String> state) {
            state.put(P_COUNT, countEditText.getText().toString());
        }

        @Override
        protected void stateFromMap(HashMap<String, String> state) {
            countEditText.setText(state.get(P_COUNT));
        }

    }

    // STOPS_ON_DATE
    private class StopsOnDateView extends AbstractView {

        private TextView onDateText;
        private final Calendar c = Calendar.getInstance();

        StopsOnDateView() {
            super(RecurrenceUntil.STOPS_ON_DATE);
            DateUtils.startOfDay(c);
            c.add(Calendar.MONTH, 6);
        }

        @Override
        public void createNodes(LinearLayout layout) {
            onDateText = x.addInfoNode(layout, R.id.date, R.string.recur_repeat_stops_on,
                    DateUtils.getMediumDateFormat(activity).format(c.getTime()));
        }

        @Override
        protected void onClick(View v, int id) {
            DatePickerDialog dialog = DatePickerDialog.newInstance(
                    (view, year, monthOfYear, dayOfMonth) -> {
                        c.set(Calendar.YEAR, year);
                        c.set(Calendar.MONTH, monthOfYear);
                        c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        onDateText.setText(DateUtils.getMediumDateFormat(activity).format(c.getTime()));
                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
            );
            applyTheme(activity, dialog);
            dialog.show(activity.getFragmentManager(), "DatePickerDialog");
        }

        @Override
        public boolean validateState() {
            return true;
        }

        @Override
        protected void stateToMap(HashMap<String, String> state) {
            DateUtils.startOfDay(c);
            state.put(P_DATE, DateUtils.FORMAT_DATE_RFC_2445.format(c.getTime()));
        }

        @Override
        protected void stateFromMap(HashMap<String, String> state) {
            Date d;
            try {
                d = DateUtils.FORMAT_DATE_RFC_2445.parse(state.get(P_DATE));
            } catch (ParseException e) {
                throw new IllegalArgumentException(state.get(P_DATE));
            }
            c.setTime(d);
            DateUtils.startOfDay(c);
            onDateText.setText(DateUtils.getMediumDateFormat(activity).format(c.getTime()));
        }

    }

    private static EditText numericEditText(Context context) {
        EditText et = new EditText(context);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        return et;

    }

    private void removeAllViewsFromParent(View v) {
        if (v.getParent() != null) {
            ((ViewGroup) v.getParent()).removeAllViews();
        }
    }

}
