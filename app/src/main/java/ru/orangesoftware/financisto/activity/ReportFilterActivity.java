/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package ru.orangesoftware.financisto.activity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.*;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.datetime.DateUtils;
import ru.orangesoftware.financisto.datetime.Period;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.filter.DateTimeCriteria;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.*;
import ru.orangesoftware.financisto.utils.EnumUtils;
import ru.orangesoftware.financisto.utils.TransactionUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReportFilterActivity extends FilterAbstractActivity {

    private static final TransactionStatus[] statuses = TransactionStatus.values();

    private WhereFilter filter = WhereFilter.empty();

    private TextView period;
    private TextView account;
    private TextView currency;
    private TextView category;
    private TextView project;
    private TextView payee;
    private TextView location;
    private TextView status;

    private ProjectSelector<ReportFilterActivity> projectSelector;
    private PayeeSelector<ReportFilterActivity> payeeSelector;

    private DateFormat df;
    private String filterValueNotFound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.blotter_filter);

        df = DateUtils.getShortDateFormat(this);
        filterValueNotFound = getString(R.string.filter_value_not_found);

        LinearLayout layout = findViewById(R.id.layout);
        period = x.addFilterNodeMinus(layout, R.id.period, R.id.period_clear, R.string.period, R.string.no_filter);
        account = x.addFilterNodeMinus(layout, R.id.account, R.id.account_clear, R.string.account, R.string.no_filter);
        currency = x.addFilterNodeMinus(layout, R.id.currency, R.id.currency_clear, R.string.currency, R.string.no_filter);

        projectSelector = new ProjectSelector<>(this, db, x, 0, R.id.project_clear, R.string.no_filter);
        projectSelector.initMultiSelect();

        payeeSelector = new PayeeSelector<>(this, db, x, 0, R.id.payee_clear, R.string.no_filter);
        payeeSelector.initMultiSelect();
        
        // todo.mb: add filter here too
        category = x.addFilterNodeMinus(layout, R.id.category, R.id.category_clear, R.string.category, R.string.no_filter);
//        payee = x.addFilterNodeMinus(layout, R.id.payee, R.id.payee_clear, R.string.payee, R.string.no_filter);
//        project = x.addFilterNodeMinus(layout, R.id.project, R.id.project_clear, R.string.project, R.string.no_filter);
        payee = payeeSelector.createNode(layout);
        project = projectSelector.createNode(layout);
        location = x.addFilterNodeMinus(layout, R.id.location, R.id.location_clear, R.string.location, R.string.no_filter);
        status = x.addFilterNodeMinus(layout, R.id.status, R.id.status_clear, R.string.transaction_status, R.string.no_filter);

        Button bOk = findViewById(R.id.bOK);
        bOk.setOnClickListener(v -> {
            Intent data = new Intent();
            filter.toIntent(data);
            setResult(RESULT_OK, data);
            finish();
        });

        Button bCancel = findViewById(R.id.bCancel);
        bCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        ImageButton bNoFilter = findViewById(R.id.bNoFilter);
        bNoFilter.setOnClickListener(v -> {
            setResult(RESULT_FIRST_USER);
            finish();
        });

        Intent intent = getIntent();
        if (intent != null) {
            filter = WhereFilter.fromIntent(intent);
            updatePeriodFromFilter();
            updateAccountFromFilter();
            updateCurrencyFromFilter();
            updateCategoryFromFilter();
            updateProjectFromFilter();
            updatePayeeFromFilter();
            updateLocationFromFilter();
            updateStatusFromFilter();
        }

    }

    private void updateLocationFromFilter() {
        Criteria c = filter.get(BlotterFilter.LOCATION_ID);
        if (c != null) {
            MyLocation loc = db.get(MyLocation.class, c.getLongValue1());
            location.setText(loc != null ? loc.name : filterValueNotFound);
            showMinusButton(location);
        } else {
            location.setText(R.string.no_filter);
            hideMinusButton(location);
        }
    }

    private void updateProjectFromFilter() {
        updateEntityFromFilter(BlotterFilter.PROJECT_ID, Project.class, project);
    }

    private void updatePayeeFromFilter() {
        updateEntityFromFilter(BlotterFilter.PAYEE_ID, Payee.class, payee);
    }

    private void updateCategoryFromFilter() {
        Criteria c = filter.get(BlotterFilter.CATEGORY_LEFT);
        if (c != null) {
            Category cat = db.getCategoryByLeft(c.getLongValue1());
            if (cat.id > 0) {
                category.setText(cat.title);
            } else {
                category.setText(filterValueNotFound);
            }
            showMinusButton(category);
        } else {
            category.setText(R.string.no_filter);
            hideMinusButton(category);
        }
    }

    private void updatePeriodFromFilter() {
        DateTimeCriteria c = (DateTimeCriteria)filter.get(BlotterFilter.DATETIME);
        if (c != null) {
            Period p = c.getPeriod();
            if (p.isCustom()) {
                long periodFrom = c.getLongValue1();
                long periodTo = c.getLongValue2();
                period.setText(df.format(new Date(periodFrom))+"-"+df.format(new Date(periodTo)));
            } else {
                period.setText(p.type.titleId);
            }
            showMinusButton(period);
        } else {
            clear(BlotterFilter.DATETIME, period);
        }
    }

    private void updateAccountFromFilter() {
        updateEntityFromFilter(BlotterFilter.FROM_ACCOUNT_ID, Account.class, account);
    }

    private void updateCurrencyFromFilter() {
        updateEntityFromFilter(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID, Currency.class, currency);
    }

    private void updateStatusFromFilter() {
        Criteria c = filter.get(BlotterFilter.STATUS);
        if (c != null) {
            TransactionStatus s = TransactionStatus.valueOf(c.getStringValue());
            status.setText(getString(s.titleId));
            showMinusButton(status);
        } else {
            status.setText(R.string.no_filter);
            hideMinusButton(status);
        }
    }

    private <T extends MyEntity> void updateEntityFromFilter(String filterCriteriaName, Class<T> entityClass, TextView filterView) {
        Criteria c = filter.get(filterCriteriaName);
        if (c != null) {
            T e = db.get(entityClass, c.getLongValue1());
            if (e != null) {
                filterView.setText(e.title);
            } else {
                filterView.setText(filterValueNotFound);
            }
            showMinusButton(filterView);
        } else {
            filterView.setText(R.string.no_filter);
            hideMinusButton(filterView);
        }
    }

    @Override
    protected void onClick(View v, int id) {
        switch (id) {
            case R.id.period:
                Intent intent = new Intent(this, DateFilterActivity.class);
                filter.toIntent(intent);
                startActivityForResult(intent, 1);
                break;
            case R.id.period_clear:
                clear(BlotterFilter.DATETIME, period);
                break;
            case R.id.account: {
                Cursor cursor = db.getAllAccounts();
                startManagingCursor(cursor);
                ListAdapter adapter = TransactionUtils.createAccountAdapter(this, cursor);
                Criteria c = filter.get(BlotterFilter.FROM_ACCOUNT_ID);
                long selectedId = c != null ? c.getLongValue1() : -1;
                x.select(this, R.id.account, R.string.account, cursor, adapter, "_id", selectedId);
            } break;
            case R.id.account_clear:
                clear(BlotterFilter.FROM_ACCOUNT_ID, account);
                break;
            case R.id.currency: {
                Cursor cursor = db.getAllCurrencies("name");
                startManagingCursor(cursor);
                ListAdapter adapter = TransactionUtils.createCurrencyAdapter(this, cursor);
                Criteria c = filter.get(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID);
                long selectedId = c != null ? c.getLongValue1() : -1;
                x.select(this, R.id.currency, R.string.currency, cursor, adapter, "_id", selectedId);
            } break;
            case R.id.currency_clear:
                clear(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID, currency);
                break;
            case R.id.category: {
                Cursor cursor = db.getCategories(false);
                startManagingCursor(cursor);
                ListAdapter adapter = TransactionUtils.createCategoryAdapter(db, this, cursor);
                Criteria c = filter.get(BlotterFilter.CATEGORY_LEFT);
                long selectedId = c != null ? c.getLongValue1() : -1;
                x.select(this, R.id.category, R.string.category, cursor, adapter, DatabaseHelper.CategoryViewColumns.left.name(), selectedId);
            } break;
            case R.id.category_clear:
                clear(BlotterFilter.CATEGORY_LEFT, category);
                break;
            case R.id.project: {
                ArrayList<Project> projects = db.getActiveProjectsList(false);
                ListAdapter adapter = TransactionUtils.createProjectAdapter(this, projects);
                Criteria c = filter.get(BlotterFilter.PROJECT_ID);
                long selectedId = c != null ? c.getLongValue1() : -1;
                int selectedPos = MyEntity.indexOf(projects, selectedId);
                x.selectItemId(this, R.id.project, R.string.project, adapter, selectedPos);
            } break;
            case R.id.project_clear:
                clear(BlotterFilter.PROJECT_ID, project);
                break;
            case R.id.payee: {
                List<Payee> payees = db.getAllPayeeList();
                ListAdapter adapter = TransactionUtils.createPayeeAdapter(this, payees);
                Criteria c = filter.get(BlotterFilter.PAYEE_ID);
                long selectedId = c != null ? c.getLongValue1() : -1;
                int selectedPos = MyEntity.indexOf(payees, selectedId);
                x.selectItemId(this, R.id.payee, R.string.payee, adapter, selectedPos);
            } break;
            case R.id.payee_clear:
                clear(BlotterFilter.PAYEE_ID, payee);
                break;
            case R.id.location: {
                Cursor cursor = db.getAllLocations(false);
                startManagingCursor(cursor);
                ListAdapter adapter = TransactionUtils.createLocationAdapter(this, cursor);
                Criteria c = filter.get(BlotterFilter.LOCATION_ID);
                long selectedId = c != null ? c.getLongValue1() : -1;
                x.select(this, R.id.location, R.string.location, cursor, adapter, "_id", selectedId);
            } break;
            case R.id.location_clear:
                clear(BlotterFilter.LOCATION_ID, location);
                break;
            case R.id.status: {
                ArrayAdapter<String> adapter = EnumUtils.createDropDownAdapter(this, statuses);
                Criteria c = filter.get(BlotterFilter.STATUS);
                int selectedPos = c != null ? TransactionStatus.valueOf(c.getStringValue()).ordinal() : -1;
                x.selectPosition(this, R.id.status, R.string.transaction_status, adapter, selectedPos);
            } break;
            case R.id.status_clear:
                clear(BlotterFilter.STATUS, status);
                break;
        }
    }

    private void clear(String criteria, TextView textView) {
        filter.remove(criteria);
        textView.setText(R.string.no_filter);
        hideMinusButton(textView);
    }

    @Override
    public void onSelectedId(int id, long selectedId) {
        switch (id) {
            case R.id.account:
                filter.put(Criteria.eq(BlotterFilter.FROM_ACCOUNT_ID, String.valueOf(selectedId)));
                updateAccountFromFilter();
                break;
            case R.id.currency:
                filter.put(Criteria.eq(BlotterFilter.FROM_ACCOUNT_CURRENCY_ID, String.valueOf(selectedId)));
                updateCurrencyFromFilter();
                break;
            case R.id.category:
                Category cat = db.getCategoryByLeft(selectedId);
                filter.put(Criteria.btw(BlotterFilter.CATEGORY_LEFT, String.valueOf(cat.left), String.valueOf(cat.right)));
                updateCategoryFromFilter();
                break;
            case R.id.project:
                filter.put(Criteria.eq(BlotterFilter.PROJECT_ID, String.valueOf(selectedId)));
                updateProjectFromFilter();
                break;
            case R.id.payee:
                filter.put(Criteria.eq(BlotterFilter.PAYEE_ID, String.valueOf(selectedId)));
                updatePayeeFromFilter();
                break;
            case R.id.location:
                filter.put(Criteria.eq(BlotterFilter.LOCATION_ID, String.valueOf(selectedId)));
                updateLocationFromFilter();
                break;
        }
    }

    @Override
    public void onSelectedPos(int id, int selectedPos) {
        switch (id) {
            case R.id.status:
                filter.put(Criteria.eq(BlotterFilter.STATUS, statuses[selectedPos].name()));
                updateStatusFromFilter();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_FIRST_USER) {
                onClick(period, R.id.period_clear);
            } else if (resultCode == RESULT_OK) {
                DateTimeCriteria c = WhereFilter.dateTimeFromIntent(data);
                filter.put(c);
                updatePeriodFromFilter();
            }
        }
    }
	
}
