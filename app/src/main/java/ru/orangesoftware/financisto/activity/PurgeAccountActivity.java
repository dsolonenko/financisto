/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.text.DateFormat;
import java.util.Calendar;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.backup.DatabaseExport;
import ru.orangesoftware.financisto.datetime.DateUtils;
import ru.orangesoftware.financisto.model.Account;

import static ru.orangesoftware.financisto.activity.UiUtils.applyTheme;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 6/12/12 11:14 PM
 */
public class PurgeAccountActivity extends AbstractActivity {

    public static final String ACCOUNT_ID = "ACCOUNT_ID";

    private Account account;
    private Calendar date;

    private LinearLayout layout;
    private CheckBox databaseBackup;
    private TextView dateText;
    private DateFormat df;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.purge_account);

        df = DateUtils.getLongDateFormat(this);

        layout = findViewById(R.id.layout);
        date = Calendar.getInstance();
        date.add(Calendar.YEAR, -1);
        date.add(Calendar.DAY_OF_YEAR, -1);

        Button bOk = findViewById(R.id.bOK);
        bOk.setOnClickListener(view -> deleteOldTransactions());

        Button bCancel = findViewById(R.id.bCancel);
        bCancel.setOnClickListener(view -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        loadAccount();
        createNodes();
        setDateText();
    }

    private void deleteOldTransactions() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.purge_account_confirm_title)
            .setMessage(getString(R.string.purge_account_confirm_message, new Object[]{account.title, getDateString()}))
            .setPositiveButton(R.string.ok, (dialogInterface, i) -> new PurgeAccountTask().execute())
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void loadAccount() {
        Intent intent = getIntent();
        if (intent == null) {
            Toast.makeText(this, "No account specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        long accountId = intent.getLongExtra(ACCOUNT_ID, -1);
        if (accountId <= 0) {
            Toast.makeText(this, "Invalid account specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        account = db.getAccount(accountId);
        if (account == null) {
            Toast.makeText(this, "No account found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void createNodes() {
        x.addInfoNode(layout, 0, R.string.account, account.title);
        x.addInfoNode(layout, 0, R.string.warning, R.string.purge_account_date_summary);
        dateText = x.addInfoNode(layout, R.id.date, R.string.date, "?");
        databaseBackup = x.addCheckboxNode(layout, R.id.backup, R.string.database_backup, R.string.purge_account_backup_database, true);
    }

    @Override
    protected void onClick(View v, int id) {
        switch (id) {
            case R.id.date:
                DatePickerDialog dialog = DatePickerDialog.newInstance(
                        (view, year, monthOfYear, dayOfMonth) -> {
                            date.set(year, monthOfYear, dayOfMonth);
                            setDateText();
                        },
                        date.get(Calendar.YEAR),
                        date.get(Calendar.MONTH),
                        date.get(Calendar.DAY_OF_MONTH)
                );
                applyTheme(this, dialog);
                dialog.show(getFragmentManager(), "DatePickerDialog");
                break;
            case R.id.backup:
                databaseBackup.setChecked(!databaseBackup.isChecked());
                break;
        }
    }

    private void setDateText() {
        dateText.setText(getDateString());
    }

    private String getDateString() {
        return df.format(date.getTime());
    }

    private class PurgeAccountTask extends AsyncTask<Void, Void, Exception> {

        private final Context context;
        private final boolean databaseBackupChecked;

        private Dialog d;

        private PurgeAccountTask() {
            this.context = PurgeAccountActivity.this;
            this.databaseBackupChecked = databaseBackup.isChecked();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            d = ProgressDialog.show(context, null, getString(R.string.purge_account_in_progress), true);
        }

        @Override
        protected void onPostExecute(Exception e) {
            super.onPostExecute(e);
            if (e != null) {
                Toast.makeText(context, R.string.purge_account_unable_to_do_backup, Toast.LENGTH_LONG).show();
            }
            d.dismiss();
        }

        @Override
        protected Exception doInBackground(Void... voids) {
            if (databaseBackupChecked) {
                DatabaseExport export = new DatabaseExport(context, db.db(), true);
                try {
                    export.export();
                } catch (Exception e) {
                    Log.e("Financisto", "Unexpected error", e);
                    return e;
                }
            }
            db.purgeAccountAtDate(account, date.getTimeInMillis());
            setResult(RESULT_OK);
            finish();
            return null;
        }

    }

}
