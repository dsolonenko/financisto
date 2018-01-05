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
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.BudgetListAdapter;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.filter.DateTimeCriteria;
import ru.orangesoftware.financisto.db.BudgetsTotalCalculator;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.model.Budget;
import ru.orangesoftware.financisto.model.Total;
import ru.orangesoftware.financisto.datetime.PeriodType;
import ru.orangesoftware.financisto.utils.RecurUtils;
import ru.orangesoftware.financisto.utils.RecurUtils.Recur;
import ru.orangesoftware.financisto.utils.RecurUtils.RecurInterval;
import ru.orangesoftware.financisto.utils.Utils;

import java.util.ArrayList;

public class BudgetListActivity extends AbstractListActivity {

    private static final int NEW_BUDGET_REQUEST = 1;
    private static final int EDIT_BUDGET_REQUEST = 2;
    private static final int VIEW_BUDGET_REQUEST = 3;
    private static final int FILTER_BUDGET_REQUEST = 4;

    private ImageButton bFilter;

    private WhereFilter filter = WhereFilter.empty();

    public BudgetListActivity() {
        super(R.layout.budget_list);
    }

    private ArrayList<Budget> budgets;
    private Handler handler;

    @Override
    protected void internalOnCreate(Bundle savedInstanceState) {
        super.internalOnCreate(savedInstanceState);

        TextView totalText = findViewById(R.id.total);
        totalText.setOnClickListener(view -> showTotals());

        bFilter = findViewById(R.id.bFilter);
        bFilter.setOnClickListener(v -> {
            Intent intent = new Intent(BudgetListActivity.this, DateFilterActivity.class);
            filter.toIntent(intent);
            startActivityForResult(intent, FILTER_BUDGET_REQUEST);
        });

        if (filter.isEmpty()) {
            filter = WhereFilter.fromSharedPreferences(getPreferences(0));
        }
        if (filter.isEmpty()) {
            filter.put(new DateTimeCriteria(PeriodType.THIS_MONTH));
        }

        budgets = db.getAllBudgets(filter);
        handler = new Handler();

        applyFilter();
        calculateTotals();
    }

    private void showTotals() {
        Intent intent = new Intent(this, BudgetListTotalsDetailsActivity.class);
        filter.toIntent(intent);
        startActivityForResult(intent, -1);
    }

    private void saveFilter() {
        SharedPreferences preferences = getPreferences(0);
        filter.toSharedPreferences(preferences);
        applyFilter();
        recreateCursor();
    }

    private void applyFilter() {
        FilterState.updateFilterColor(this, filter, bFilter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILTER_BUDGET_REQUEST) {
            if (resultCode == RESULT_FIRST_USER) {
                filter.clear();
            } else if (resultCode == RESULT_OK) {
                String periodType = data.getStringExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_TYPE);
                PeriodType p = PeriodType.valueOf(periodType);
                if (PeriodType.CUSTOM == p) {
                    long periodFrom = data.getLongExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_FROM, 0);
                    long periodTo = data.getLongExtra(DateFilterActivity.EXTRA_FILTER_PERIOD_TO, 0);
                    filter.put(new DateTimeCriteria(periodFrom, periodTo));
                } else {
                    filter.put(new DateTimeCriteria(p));
                }
            }
            saveFilter();
        }
        recreateCursor();
    }

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
        return new BudgetListAdapter(this, budgets);
    }

    @Override
    protected Cursor createCursor() {
        return null;
    }

    @Override
    public void recreateCursor() {
        budgets = db.getAllBudgets(filter);
        updateAdapter();
        calculateTotals();
    }

    private void updateAdapter() {
        ((BudgetListAdapter) adapter).setBudgets(budgets);
    }

    private BudgetTotalsCalculationTask totalCalculationTask;

    private void calculateTotals() {
        if (totalCalculationTask != null) {
            totalCalculationTask.stop();
            totalCalculationTask.cancel(true);
        }
        TextView totalText = findViewById(R.id.total);
        totalCalculationTask = new BudgetTotalsCalculationTask(totalText);
        totalCalculationTask.execute((Void[]) null);
    }

    @Override
    protected void addItem() {
        Intent intent = new Intent(this, BudgetActivity.class);
        startActivityForResult(intent, NEW_BUDGET_REQUEST);
    }

    @Override
    protected void deleteItem(View v, int position, final long id) {
        final Budget b = db.load(Budget.class, id);
        if (b.parentBudgetId > 0) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.delete_budget_recurring_select)
                    .setPositiveButton(R.string.delete_budget_one_entry, (arg0, arg1) -> {
                        db.deleteBudgetOneEntry(id);
                        recreateCursor();
                    })
                    .setNeutralButton(R.string.delete_budget_all_entries, (arg0, arg1) -> {
                        db.deleteBudget(b.parentBudgetId);
                        recreateCursor();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            Recur recur = RecurUtils.createFromExtraString(b.recur);
            new AlertDialog.Builder(this)
                    .setMessage(recur.interval == RecurInterval.NO_RECUR ? R.string.delete_budget_confirm : R.string.delete_budget_recurring_confirm)
                    .setPositiveButton(R.string.yes, (arg0, arg1) -> {
                        db.deleteBudget(id);
                        recreateCursor();
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        }
    }

    @Override
    public void editItem(View v, int position, long id) {
        Budget b = db.load(Budget.class, id);
        Recur recur = b.getRecur();
        if (recur.interval != RecurInterval.NO_RECUR) {
            Toast t = Toast.makeText(this, R.string.edit_recurring_budget, Toast.LENGTH_LONG);
            t.show();
        }
        Intent intent = new Intent(this, BudgetActivity.class);
        intent.putExtra(BudgetActivity.BUDGET_ID_EXTRA, b.parentBudgetId > 0 ? b.parentBudgetId : id);
        startActivityForResult(intent, EDIT_BUDGET_REQUEST);
    }

    @Override
    protected void viewItem(View v, int position, long id) {
        Budget b = db.load(Budget.class, id);
        Intent intent = new Intent(this, BudgetBlotterActivity.class);
        Criteria.eq(BlotterFilter.BUDGET_ID, String.valueOf(id))
                .toIntent(b.title, intent);
        startActivityForResult(intent, VIEW_BUDGET_REQUEST);
    }

    public class BudgetTotalsCalculationTask extends AsyncTask<Void, Total, Total> {

        private volatile boolean isRunning = true;

        private final TextView totalText;

        public BudgetTotalsCalculationTask(TextView totalText) {
            this.totalText = totalText;
        }

        @Override
        protected Total doInBackground(Void... params) {
            try {
                BudgetsTotalCalculator c = new BudgetsTotalCalculator(db, budgets);
                c.updateBudgets(handler);
                return c.calculateTotalInHomeCurrency();
            } catch (Exception ex) {
                Log.e("BudgetTotals", "Unexpected error", ex);
                return Total.ZERO;
            }

        }

        @Override
        protected void onPostExecute(Total result) {
            if (isRunning) {
                Utils u = new Utils(BudgetListActivity.this);
                u.setTotal(totalText, result);
                ((BudgetListAdapter) adapter).notifyDataSetChanged();
            }
        }

        public void stop() {
            isRunning = false;
        }

    }

}
