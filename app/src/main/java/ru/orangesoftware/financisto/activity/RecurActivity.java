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
package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.text.DateFormat;
import java.util.Calendar;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.datetime.DateUtils;
import ru.orangesoftware.financisto.utils.LocalizableEnum;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.PinProtection;
import ru.orangesoftware.financisto.utils.RecurUtils;
import ru.orangesoftware.financisto.utils.RecurUtils.DayOfWeek;
import ru.orangesoftware.financisto.utils.RecurUtils.EveryXDay;
import ru.orangesoftware.financisto.utils.RecurUtils.Layoutable;
import ru.orangesoftware.financisto.utils.RecurUtils.Recur;
import ru.orangesoftware.financisto.utils.RecurUtils.RecurInterval;
import ru.orangesoftware.financisto.utils.RecurUtils.RecurPeriod;
import ru.orangesoftware.financisto.utils.RecurUtils.SemiMonthly;
import ru.orangesoftware.financisto.utils.RecurUtils.Weekly;
import ru.orangesoftware.financisto.utils.Utils;

import static ru.orangesoftware.financisto.activity.UiUtils.applyTheme;

public class RecurActivity extends Activity {

    public static final String EXTRA_RECUR = "recur";

    private static final RecurPeriod[] periods = RecurPeriod.values();

    private Spinner sInterval;
    private Spinner sPeriod;
    private LinearLayout layoutInterval;
    private LinearLayout layoutRecur;
    private Button bStartDate;

    private final Calendar startDate = Calendar.getInstance();
    private final Calendar stopsOnDate = Calendar.getInstance();

    private DateFormat df;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.recur);
        setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_dialog_time);

        df = DateUtils.getLongDateFormat(this);

        stopsOnDate.add(Calendar.YEAR, 1);

        sInterval = findViewById(R.id.intervalSpinner);
        sPeriod = findViewById(R.id.recurSpinner);
        layoutInterval = findViewById(R.id.layoutInterval);
        layoutRecur = findViewById(R.id.recurInterval);

        bStartDate = findViewById(R.id.bStartDate);
        bStartDate.setOnClickListener(v -> {
            final Calendar c = startDate;
            DatePickerDialog dialog = DatePickerDialog.newInstance(
                    (view, year, monthOfYear, dayOfMonth) -> {
                        c.set(Calendar.YEAR, year);
                        c.set(Calendar.MONTH, monthOfYear);
                        c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        DateUtils.startOfDay(c);
                        editStartDate(c.getTimeInMillis());
                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
            );
            applyTheme(this, dialog);
            dialog.show(getFragmentManager(), "DatePickerDialog");
        });

        addSpinnerItems(sInterval, new RecurInterval[]{RecurInterval.NO_RECUR, RecurInterval.WEEKLY, RecurInterval.MONTHLY});
        addSpinnerItems(sPeriod, periods);

        LayoutInflater inflater = getLayoutInflater();
        //addLayouts(inflater, layoutInterval, intervals);
        addLayouts(inflater, layoutRecur, periods);

        Recur recur = RecurUtils.createDefaultRecur();
        Intent intent = getIntent();
        if (intent != null) {
            String extra = intent.getStringExtra(EXTRA_RECUR);
            if (extra != null) {
                recur = RecurUtils.createFromExtraString(extra);
            }
        }
        editRecur(recur);

        sInterval.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                RecurInterval interval = getRecurInterval(sInterval.getSelectedItem());
                selectInterval(interval);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }

        });

        sPeriod.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                RecurPeriod period = periods[position];
                selectPeriod(period);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }

        });

        Button bOk = findViewById(R.id.bOK);
        bOk.setOnClickListener(view -> {
            RecurInterval interval = getRecurInterval(sInterval.getSelectedItem());
            RecurPeriod period = periods[sPeriod.getSelectedItemPosition()];
            Recur r = RecurUtils.createRecur(interval);
            r.startDate = startDate.getTimeInMillis();
            r.period = period;
            if (updateInterval(r) && updatePeriod(r)) {
                Intent data = new Intent();
                data.putExtra(EXTRA_RECUR, r.toString());
                setResult(RESULT_OK, data);
                finish();
            }
        });

        Button bCancel = findViewById(R.id.bCancel);
        bCancel.setOnClickListener(view -> {
            setResult(RESULT_CANCELED, null);
            finish();
        });

    }

    private static class SpinnerItem {
        public final String title;
        public final String value;

        public SpinnerItem(String title, String value) {
            super();
            this.title = title;
            this.value = value;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    private void addSpinnerItems(Spinner spinner, LocalizableEnum[] a) {
        int length = a.length;
        SpinnerItem[] items = new SpinnerItem[length];
        for (int i = 0; i < length; i++) {
            LocalizableEnum x = a[i];
            String title = getString(x.getTitleId());
            String value = x.name();
            items[i] = new SpinnerItem(title, value);
        }
        ArrayAdapter<SpinnerItem> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    protected RecurInterval getRecurInterval(Object item) {
        return RecurInterval.valueOf(((SpinnerItem) item).value);
    }

    protected boolean updateInterval(Recur r) {
        RecurInterval interval = r.interval;
        View v = selectInterval(interval);
        switch (interval) {
            case EVERY_X_DAY:
                return updateEveryXDay(v, r);
            case WEEKLY:
                return updateWeekly(v, r);
            case SEMI_MONTHLY:
                return updateSemiMonthly(v, r);
        }
        return true;
    }

    protected boolean updatePeriod(Recur r) {
        RecurPeriod period = r.period;
        View v = selectPeriod(period);
        switch (period) {
            case EXACTLY_TIMES:
                return updateExactlyTimes(v, r);
            case STOPS_ON_DATE:
                return updateStopsOnDate(v, r);
        }
        return true;
    }

    private void addLayouts(LayoutInflater inflater, LinearLayout layout, Layoutable[] items) {
        for (Layoutable i : items) {
            int layoutId = i.getLayoutId();
            if (layoutId != 0) {
                final View v = inflater.inflate(layoutId, null);
                v.setTag(i);
                v.setVisibility(View.INVISIBLE);
                if (i == RecurPeriod.STOPS_ON_DATE) {
                    Button b = v.findViewById(R.id.bStopsOnDate);
                    final Calendar c = this.stopsOnDate;
                    editStopsOnDate(v, c.getTimeInMillis());
                    b.setOnClickListener(view -> {
                        DatePickerDialog dialog = DatePickerDialog.newInstance(
                                (view1, year, monthOfYear, dayOfMonth) -> {
                                    c.set(Calendar.YEAR, year);
                                    c.set(Calendar.MONTH, monthOfYear);
                                    c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                    DateUtils.endOfDay(c);
                                    editStopsOnDate(v, c.getTimeInMillis());
                                },
                                c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH),
                                c.get(Calendar.DAY_OF_MONTH)
                        );
                        applyTheme(RecurActivity.this, dialog);
                        dialog.show(getFragmentManager(), "DatePickerDialog");
                    });
                }
                layout.addView(v, LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void editRecur(Recur recur) {
        editStartDate(recur.startDate);
        RecurInterval interval = recur.interval;
        SpinnerAdapter adapter = sInterval.getAdapter();
        int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            SpinnerItem item = (SpinnerItem) adapter.getItem(i);
            if (interval == RecurInterval.valueOf(item.value)) {
                sInterval.setSelection(i);
                break;
            }
        }
        View v = selectInterval(interval);
        switch (interval) {
            case EVERY_X_DAY:
                editEveryXDay(v, recur);
                break;
            case WEEKLY:
                editWeekly(v, recur);
                break;
            case SEMI_MONTHLY:
                editSemiMonthly(v, recur);
                break;
        }
        RecurPeriod period = recur.period;
        sPeriod.setSelection(period.ordinal());
        v = selectPeriod(period);
        switch (period) {
            case EXACTLY_TIMES:
                editExactlyTimes(v, recur.periodParam);
                break;
            case STOPS_ON_DATE:
                editStopsOnDate(v, recur.periodParam);
                break;
        }
    }

    private void editEveryXDay(View v, Recur recur) {
        EveryXDay x = (EveryXDay) recur;
        EditText t = v.findViewById(R.id.edEveryXDays);
        t.setText(String.valueOf(x.days));
    }

    private void editWeekly(View v, Recur recur) {
        return;
    }

    private void editSemiMonthly(View v, Recur recur) {
        SemiMonthly sm = (SemiMonthly) recur;
        EditText t1 = v.findViewById(R.id.edFirstDay);
        t1.setText(String.valueOf(sm.firstDay));
        EditText t2 = v.findViewById(R.id.edSecondDay);
        t2.setText(String.valueOf(sm.secondDay));
    }

    private boolean updateEveryXDay(View v, Recur r) {
        EveryXDay x = (EveryXDay) r;
        EditText t = v.findViewById(R.id.edEveryXDays);
        if (Utils.isEmpty(t)) {
            showError(t, R.string.recur_error_specify_days);
            return false;
        }
        x.days = Integer.parseInt(Utils.text(t));
        return true;
    }

    private void showError(EditText t, int messageId) {
        t.setError(getString(messageId));
    }

    private boolean updateWeekly(View v, Recur r) {
        Weekly w = (Weekly) r;
        int i = startDate.get(Calendar.DAY_OF_WEEK);
        DayOfWeek[] days = DayOfWeek.values();
        for (DayOfWeek d : days) {
            w.unset(d);
        }
        w.set(days[i - 1]);
        return true;
    }

    private boolean updateSemiMonthly(View v, Recur r) {
        SemiMonthly sm = (SemiMonthly) r;
        EditText t1 = v.findViewById(R.id.edFirstDay);
        if (Utils.isEmpty(t1)) {
            showError(t1, R.string.recur_error_specify_first_day);
            return false;
        }
        sm.firstDay = Integer.parseInt(Utils.text(t1));
        EditText t2 = v.findViewById(R.id.edSecondDay);
        if (Utils.isEmpty(t2)) {
            showError(t2, R.string.recur_error_specify_second_day);
            return false;
        }
        sm.secondDay = Integer.parseInt(Utils.text(t2));
        return true;
    }

    private void editExactlyTimes(View v, long times) {
        EditText e = v.findViewById(R.id.edTimes);
        e.setText(times > 0 ? String.valueOf(times) : "1");
    }

    private void editStartDate(long date) {
        Calendar c = startDate;
        c.setTimeInMillis(date);
        bStartDate.setText(df.format(c.getTime()));
    }

    private void editStopsOnDate(View v, long date) {
        Calendar c = stopsOnDate;
        c.setTimeInMillis(date);
        Button b = v.findViewById(R.id.bStopsOnDate);
        b.setText(df.format(c.getTime()));
    }

    private boolean updateExactlyTimes(View v, Recur r) {
        EditText e = v.findViewById(R.id.edTimes);
        if (Utils.isEmpty(e)) {
            showError(e, R.string.recur_error_specify_times);
            return false;
        }
        r.periodParam = Long.parseLong(Utils.text(e));
        return true;
    }

    private boolean updateStopsOnDate(View v, Recur r) {
        r.periodParam = stopsOnDate.getTimeInMillis();
        return true;
    }

    protected View selectInterval(RecurInterval interval) {
        if (interval == RecurInterval.NO_RECUR) {
            sPeriod.setSelection(RecurPeriod.STOPS_ON_DATE.ordinal());
            sPeriod.setEnabled(false);
        } else {
            sPeriod.setEnabled(true);
        }
        return selectInLayout(layoutInterval, interval);
    }

    protected View selectPeriod(RecurPeriod period) {
        return selectInLayout(layoutRecur, period);
    }

    private View selectInLayout(LinearLayout layout, Object tag) {
        View selected = null;
        int count = layout.getChildCount();
        for (int i = 0; i < count; i++) {
            View v = layout.getChildAt(i);
            if (tag == v.getTag()) {
                selected = v;
            } else {
                v.setVisibility(View.GONE);
            }
        }
        if (selected != null) {
            selected.setVisibility(View.VISIBLE);
        }
        return selected;
    }

    @Override
    protected void onPause() {
        super.onPause();
        PinProtection.lock(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PinProtection.unlock(this);
    }
}
