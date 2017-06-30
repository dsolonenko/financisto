/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * <p/>
 * Contributors:
 * Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.activity;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.TabHost;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.dialog.WebViewDialog;
import ru.orangesoftware.financisto.utils.CurrencyCache;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.PinProtection;

public class MainActivity extends TabActivity implements TabHost.OnTabChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        initialLoad();

        final TabHost tabHost = getTabHost();

        setupAccountsTab(tabHost);
        setupBlotterTab(tabHost);
        setupBudgetsTab(tabHost);
        setupReportsTab(tabHost);
        setupMenuTab(tabHost);

        MyPreferences.StartupScreen screen = MyPreferences.getStartupScreen(this);
        tabHost.setCurrentTabByTag(screen.tag);

        tabHost.setOnTabChangedListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        PinProtection.unlock(this);
        if (PinProtection.isUnlocked()) {
            WebViewDialog.checkVersionAndShowWhatsNewIfNeeded(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        PinProtection.lock(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PinProtection.immediateLock(this);
    }

    private void initialLoad() {
        long t3, t2, t1, t0 = System.currentTimeMillis();
        DatabaseAdapter db = new DatabaseAdapter(this);
        db.open();
        try {
            SQLiteDatabase x = db.db();
            x.beginTransaction();
            t1 = System.currentTimeMillis();
            try {
                updateFieldInTable(x, DatabaseHelper.CATEGORY_TABLE, 0, "title", getString(R.string.no_category));
                updateFieldInTable(x, DatabaseHelper.CATEGORY_TABLE, -1, "title", getString(R.string.split));
                updateFieldInTable(x, DatabaseHelper.PROJECT_TABLE, 0, "title", getString(R.string.no_project));
                updateFieldInTable(x, DatabaseHelper.LOCATIONS_TABLE, 0, "name", getString(R.string.current_location));
                updateFieldInTable(x, DatabaseHelper.LOCATIONS_TABLE, 0, "title", getString(R.string.current_location));
                x.setTransactionSuccessful();
            } finally {
                x.endTransaction();
            }
            t2 = System.currentTimeMillis();
            if (MyPreferences.shouldUpdateHomeCurrency(this)) {
                db.setDefaultHomeCurrency();
            }
            CurrencyCache.initialize(db);
            t3 = System.currentTimeMillis();
            if (MyPreferences.shouldRebuildRunningBalance(this)) {
                db.rebuildRunningBalances();
            }
            if (MyPreferences.shouldUpdateAccountsLastTransactionDate(this)) {
                db.updateAccountsLastTransactionDate();
            }
        } finally {
            db.close();
        }
        long t4 = System.currentTimeMillis();
        Log.d("Financisto", "Load time = " + (t4 - t0) + "ms = " + (t2 - t1) + "ms+" + (t3 - t2) + "ms+" + (t4 - t3) + "ms");
    }

    private void updateFieldInTable(SQLiteDatabase db, String table, long id, String field, String value) {
        db.execSQL("update " + table + " set " + field + "=? where _id=?", new Object[]{value, id});
    }

    @Override
    public void onTabChanged(String tabId) {
        Log.d("Financisto", "About to update tab " + tabId);
        long t0 = System.currentTimeMillis();
        refreshCurrentTab();
        long t1 = System.currentTimeMillis();
        Log.d("Financisto", "Tab " + tabId + " updated in " + (t1 - t0) + "ms");
    }

    public void refreshCurrentTab() {
        Context c = getTabHost().getCurrentView().getContext();
        if (c instanceof RefreshSupportedActivity) {
            RefreshSupportedActivity activity = (RefreshSupportedActivity) c;
            activity.recreateCursor();
            activity.integrityCheck();
        }
    }

    private void setupAccountsTab(TabHost tabHost) {
        tabHost.addTab(tabHost.newTabSpec("accounts")
                .setIndicator(getString(R.string.accounts), getResources().getDrawable(R.drawable.ic_tab_accounts))
                .setContent(new Intent(this, AccountListActivity.class)));
    }

    private void setupBlotterTab(TabHost tabHost) {
        Intent intent = new Intent(this, BlotterActivity.class);
        intent.putExtra(BlotterActivity.SAVE_FILTER, true);
        intent.putExtra(BlotterActivity.EXTRA_FILTER_ACCOUNTS, true);
        tabHost.addTab(tabHost.newTabSpec("blotter")
                .setIndicator(getString(R.string.blotter), getResources().getDrawable(R.drawable.ic_tab_blotter))
                .setContent(intent));
    }

    private void setupBudgetsTab(TabHost tabHost) {
        tabHost.addTab(tabHost.newTabSpec("budgets")
                .setIndicator(getString(R.string.budgets), getResources().getDrawable(R.drawable.ic_tab_budgets))
                .setContent(new Intent(this, BudgetListActivity.class)));
    }

    private void setupReportsTab(TabHost tabHost) {
        tabHost.addTab(tabHost.newTabSpec("reports")
                .setIndicator(getString(R.string.reports), getResources().getDrawable(R.drawable.ic_tab_reports))
                .setContent(new Intent(this, ReportsListActivity.class)));
    }

    private void setupMenuTab(TabHost tabHost) {
        tabHost.addTab(tabHost.newTabSpec("menu")
                .setIndicator(getString(R.string.menu), getResources().getDrawable(R.drawable.ic_tab_menu))
                .setContent(new Intent(this, MenuListActivity_.class)));
    }

}
