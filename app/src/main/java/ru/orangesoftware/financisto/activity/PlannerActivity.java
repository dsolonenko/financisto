/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.ScheduledListAdapter;
import ru.orangesoftware.financisto.datetime.Period;
import ru.orangesoftware.financisto.datetime.PeriodType;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.filter.DateTimeCriteria;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.utils.FuturePlanner;
import ru.orangesoftware.financisto.utils.TransactionList;
import ru.orangesoftware.financisto.utils.Utils;

import java.util.Calendar;
import java.util.Date;

public class PlannerActivity extends AbstractListActivity {

    private TextView totalText;
    private TextView filterText;

    private WhereFilter filter = WhereFilter.empty();

    public PlannerActivity() {
        super(R.layout.planner);
    }

    @Override
    protected void internalOnCreate(Bundle savedInstanceState) {
        totalText = findViewById(R.id.total);
        filterText = findViewById(R.id.period);
        ImageButton bFilter = findViewById(R.id.bFilter);
        bFilter.setOnClickListener(view -> showFilter());

        loadFilter();
        setupFilter();
        FilterState.updateFilterColor(this, filter, bFilter);
    }

    private void loadFilter() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        filter = WhereFilter.fromSharedPreferences(preferences);
        applyDateTimeCriteria(filter.getDateTime());
    }

    private void setupFilter() {
        if (filter.isEmpty()) {
            applyDateTimeCriteria(null);
        }
    }

    private void applyDateTimeCriteria(DateTimeCriteria criteria) {
        if (criteria == null) {
            Calendar date = Calendar.getInstance();
            date.add(Calendar.MONTH, 1);
            criteria = new DateTimeCriteria(PeriodType.THIS_MONTH);
        }
        long now = System.currentTimeMillis();
        if (now > criteria.getLongValue1()) {
            Period period = criteria.getPeriod();
            period.start = now;
            criteria = new DateTimeCriteria(period);
        }
        filter.put(criteria);
    }

    private void showFilter() {
        Intent intent = new Intent(this, DateFilterActivity.class);
        intent.putExtra(DateFilterActivity.EXTRA_FILTER_DONT_SHOW_NO_FILTER, true);
        intent.putExtra(DateFilterActivity.EXTRA_FILTER_SHOW_PLANNER, true);
        filter.toIntent(intent);
        startActivityForResult(intent, 1);
    }

    private void saveFilter() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        filter.toSharedPreferences(preferences);
        SharedPreferences.Editor editor = preferences.edit();
        editor.apply();
    }

    @Override
    protected Cursor createCursor() {
        retrieveData();
        return null;
    }

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
        return null;
    }

    @Override
    protected void deleteItem(View v, int position, long id) {
    }

    @Override
    protected void editItem(View v, int position, long id) {
    }

    @Override
    protected void viewItem(View v, int position, long id) {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            DateTimeCriteria c = WhereFilter.dateTimeFromIntent(data);
            applyDateTimeCriteria(c);
            saveFilter();
            retrieveData();
        }
    }

    private PlannerTask task;

    private void retrieveData() {
        if (task != null) {
            task.cancel(true);
        }
        task = new PlannerTask(filter);
        task.execute();
    }

    private class PlannerTask extends AsyncTask<Void, Void, TransactionList> {

        private final WhereFilter filter;

        private PlannerTask(WhereFilter filter) {
            this.filter = WhereFilter.copyOf(filter);
        }

        @Override
        protected TransactionList doInBackground(Void... voids) {
            FuturePlanner planner = new FuturePlanner(db, filter, new Date());
            return planner.getPlannedTransactionsWithTotals();
        }

        @Override
        protected void onPostExecute(TransactionList data) {
            ScheduledListAdapter adapter = new ScheduledListAdapter(PlannerActivity.this, data.transactions);
            setListAdapter(adapter);
            setTotals(data.totals);
            updateFilterText(filter);
        }

    }

    private void updateFilterText(WhereFilter filter) {
        Criteria c = filter.get(DatabaseHelper.ReportColumns.DATETIME);
        if (c != null) {
            filterText.setText(DateUtils.formatDateRange(this, c.getLongValue1(), c.getLongValue2(),
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_MONTH));
        } else {
            filterText.setText(R.string.no_filter);
        }
    }

    private void setTotals(Total[] totals) {
        Utils u = new Utils(this);
        u.setTotal(totalText, totals[0]);
    }

}
