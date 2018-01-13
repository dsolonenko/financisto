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

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.datetime.DateUtils;
import ru.orangesoftware.financisto.recur.DateRecurrenceIterator;
import ru.orangesoftware.financisto.recur.Recurrence;
import ru.orangesoftware.financisto.recur.RecurrenceFrequency;
import ru.orangesoftware.financisto.recur.RecurrencePattern;
import ru.orangesoftware.financisto.recur.RecurrencePeriod;
import ru.orangesoftware.financisto.recur.RecurrenceUntil;
import ru.orangesoftware.financisto.recur.RecurrenceView;
import ru.orangesoftware.financisto.recur.RecurrenceViewFactory;
import ru.orangesoftware.financisto.utils.EnumUtils;

import static ru.orangesoftware.financisto.activity.UiUtils.applyTheme;

public class RecurrenceActivity extends AbstractActivity {

    public static final String RECURRENCE_PATTERN = "recurrence_pattern";

    private static final RecurrenceFrequency[] frequencies = RecurrenceFrequency.values();
    private static final RecurrenceUntil[] until = RecurrenceUntil.values();

    private LinearLayout layout;
    private RecurrenceViewFactory viewFactory;

    private TextView startDateView;
    private TextView startTimeView;
    private Recurrence recurrence = Recurrence.noRecur();
    private RecurrenceView recurrencePatternView;
    private RecurrenceView recurrencePeriodView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.recurrence);

        layout = findViewById(R.id.layout);
        viewFactory = new RecurrenceViewFactory(this);

        Intent intent = getIntent();
        if (intent != null) {
            String recurrencePattern = intent.getStringExtra(RECURRENCE_PATTERN);
            if (recurrencePattern != null) {
                try {
                    recurrence = Recurrence.parse(recurrencePattern);
                } catch (Exception e) {
                    recurrence = Recurrence.noRecur();
                }
                recurrencePatternView = viewFactory.create(recurrence.pattern);
                recurrencePeriodView = viewFactory.create(recurrence.period.until);
            }
        }

        createNodes();
        if (recurrencePatternView != null) {
            recurrencePatternView.stateFromString(recurrence.pattern.params);
            if (recurrencePeriodView != null) {
                recurrencePeriodView.stateFromString(recurrence.period.params);
            }
        }

        Button bOK = findViewById(R.id.bOK);
        bOK.setOnClickListener(arg0 -> {
            if (recurrencePatternView == null) {
                Intent data = new Intent();
                setResult(RESULT_OK, data);
                finish();
            } else {
                if (recurrencePatternView.validateState() && (recurrencePeriodView == null || recurrencePeriodView.validateState())) {
                    Intent data = new Intent();
                    data.putExtra(RECURRENCE_PATTERN, stateToString());
                    setResult(RESULT_OK, data);
                    finish();
                }
            }
        });

        Button bCancel = findViewById(R.id.bCancel);
        bCancel.setOnClickListener(arg0 -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }

    protected String stateToString() {
        if (recurrencePatternView != null) {
            recurrence.pattern = RecurrencePattern.parse(recurrencePatternView.stateToString());
            if (recurrencePeriodView != null) {
                recurrence.period = RecurrencePeriod.parse(recurrencePeriodView.stateToString());
            } else {
                recurrence.period = RecurrencePeriod.noEndDate();
            }
        } else {
            recurrence.pattern = RecurrencePattern.noRecur();
            recurrence.period = RecurrencePeriod.noEndDate();
        }
        return recurrence.stateToString();
    }

    public void createNodes() {
        layout.removeAllViews();
        x.addListNode(layout, R.id.recurrence_pattern, R.string.recurrence_pattern, getString(recurrence.pattern.frequency.titleId));
        if (recurrencePatternView != null) {
            recurrencePatternView.createNodes(layout);
            startDateView = x.addInfoNode(layout, R.id.start_date, R.string.recurrence_period_starts_on_date,
                    DateUtils.getShortDateFormat(this).format(recurrence.getStartDate().getTime()));
            startTimeView = x.addInfoNode(layout, R.id.start_time, R.string.recurrence_period_starts_on_time,
                    DateUtils.getTimeFormat(this).format(recurrence.getStartDate().getTime()));
            if (recurrence.pattern.frequency != RecurrenceFrequency.GEEKY) {
                x.addListNode(layout, R.id.recurrence_period, R.string.recurrence_period, getString(recurrence.period.until.titleId));
                if (recurrencePeriodView != null) {
                    recurrencePeriodView.createNodes(layout);
                }
            }
            x.addInfoNodeSingle(layout, R.id.result, R.string.recurrence_evaluate);
        }
    }

    @Override
    protected void onClick(View v, int id) {
        switch (id) {
            case R.id.recurrence_pattern: {
                ArrayAdapter<String> adapter = EnumUtils.createDropDownAdapter(this, frequencies);
                x.selectPosition(this, R.id.recurrence_pattern, R.string.recurrence_pattern, adapter, recurrence.pattern.frequency.ordinal());
            }
            break;
            case R.id.recurrence_period: {
                ArrayAdapter<String> adapter = EnumUtils.createDropDownAdapter(this, until);
                x.selectPosition(this, R.id.recurrence_period, R.string.recurrence_period, adapter, recurrence.period.until.ordinal());
            }
            break;
            case R.id.start_date: {
                final Calendar c = recurrence.getStartDate();
                DatePickerDialog dialog = DatePickerDialog.newInstance(
                        (view, year, monthOfYear, dayOfMonth) -> {
                            recurrence.updateStartDate(year, monthOfYear, dayOfMonth);
                            startDateView.setText(DateUtils.getMediumDateFormat(RecurrenceActivity.this).format(c.getTime()));
                        },
                        c.get(Calendar.YEAR),
                        c.get(Calendar.MONTH),
                        c.get(Calendar.DAY_OF_MONTH)
                );
                applyTheme(this, dialog);
                dialog.show(getFragmentManager(), "DatePickerDialog");
            }
            break;
            case R.id.start_time: {
                final Calendar c = recurrence.getStartDate();
                boolean is24Format = DateUtils.is24HourFormat(RecurrenceActivity.this);
                TimePickerDialog dialog = TimePickerDialog.newInstance(
                        (view, hourOfDay, minute, second) -> {
                            recurrence.updateStartTime(hourOfDay, minute, 0);
                            startTimeView.setText(DateUtils.getTimeFormat(RecurrenceActivity.this).format(c.getTime()));
                        },
                        c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), is24Format
                );
                applyTheme(this, dialog);
                dialog.show(getFragmentManager(), "TimePickerDialog");
            }
            break;
            case R.id.result: {
                try {
                    String stateAsString = stateToString();
                    Log.d("RRULE", stateAsString);
                    Recurrence r = Recurrence.parse(stateAsString);
                    DateRecurrenceIterator ri = r.createIterator(new Date());
                    StringBuilder sb = new StringBuilder();
                    DateFormat df = DateUtils.getMediumDateFormat(this);
                    String n = String.format("%n");
                    int count = 0;
                    while (count++ < 10 && ri.hasNext()) {
                        Date nextDate = ri.next();
                        if (count > 1) {
                            sb.append(n);
                        }
                        sb.append(df.format(nextDate.getTime()));
                    }
                    if (ri.hasNext()) {
                        sb.append(n).append("...");
                    }
                    new AlertDialog.Builder(this)
                            .setTitle(getString(r.pattern.frequency.titleId))
                            .setMessage(sb.toString())
                            .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                            .show();
                } catch (Exception ex) {
                    Toast.makeText(this, ex.getClass().getSimpleName() + ":" + ex.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
            break;
        }
    }

    @Override
    public void onSelectedPos(int id, int selectedPos) {
        switch (id) {
            case R.id.recurrence_pattern:
                RecurrenceFrequency newFrequency = frequencies[selectedPos];
                if (recurrence.pattern.frequency != newFrequency) {
                    recurrence.pattern = RecurrencePattern.empty(newFrequency);
                    recurrencePatternView = viewFactory.create(recurrence.pattern);
                    createNodes();
                }
                break;
            case R.id.recurrence_period:
                RecurrenceUntil newUntil = until[selectedPos];
                if (recurrence.period.until != newUntil) {
                    recurrence.period = RecurrencePeriod.empty(newUntil);
                    recurrencePeriodView = viewFactory.create(newUntil);
                    createNodes();
                }
                break;
        }
    }

}
