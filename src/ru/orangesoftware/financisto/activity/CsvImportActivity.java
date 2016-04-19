/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package ru.orangesoftware.financisto.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.MyEntityAdapter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.utils.CurrencyExportPreferences;

import java.util.List;


public class CsvImportActivity extends AbstractImportActivity {

    public static final String CSV_IMPORT_SELECTED_ACCOUNT_2 = "CSV_IMPORT_SELECTED_ACCOUNT_2";
    public static final String CSV_IMPORT_DATE_FORMAT = "CSV_IMPORT_DATE_FORMAT";
    public static final String CSV_IMPORT_FILENAME = "CSV_IMPORT_FILENAME";
    public static final String CSV_IMPORT_FIELD_SEPARATOR = "CSV_IMPORT_FIELD_SEPARATOR";
    public static final String CSV_IMPORT_USE_HEADER_FROM_FILE = "CSV_IMPORT_USE_HEADER_FROM_FILE";

    private final CurrencyExportPreferences currencyPreferences = new CurrencyExportPreferences("csv");

    private DatabaseAdapter db;
    private List<Account> accounts;
    private Spinner accountSpinner;
    private CheckBox useHeaderFromFile;

    public CsvImportActivity() {
        super(R.layout.csv_import);
    }

    @Override
    protected void internalOnCreate() {
        db = new DatabaseAdapter(this);
        db.open();

        accounts = db.em().getAllAccountsList();
        ArrayAdapter<Account> accountsAdapter = new MyEntityAdapter<Account>(this, android.R.layout.simple_spinner_item, android.R.id.text1, accounts);
        accountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        accountSpinner = (Spinner) findViewById(R.id.spinnerAccount);
        accountSpinner.setAdapter(accountsAdapter);

        useHeaderFromFile = (CheckBox)findViewById(R.id.cbUseHeaderFromFile);

        Button bOk = (Button) findViewById(R.id.bOK);
        bOk.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                if (edFilename.getText().toString().equals("")) {
                    Toast.makeText(CsvImportActivity.this, R.string.select_filename, Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent data = new Intent();
                updateResultIntentFromUi(data);
                setResult(RESULT_OK, data);
                finish();
            }
        });

        Button bCancel = (Button) findViewById(R.id.bCancel);
        bCancel.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

    }


    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    @Override
    protected void updateResultIntentFromUi(Intent data) {
        currencyPreferences.updateIntentFromUI(this, data);
        data.putExtra(CSV_IMPORT_SELECTED_ACCOUNT_2, getSelectedAccountId());
        Spinner dateFormats = (Spinner) findViewById(R.id.spinnerDateFormats);
        data.putExtra(CSV_IMPORT_DATE_FORMAT, dateFormats.getSelectedItem().toString());
        data.putExtra(CSV_IMPORT_FILENAME, edFilename.getText().toString());
        Spinner fieldSeparator = (Spinner) findViewById(R.id.spinnerFieldSeparator);
        data.putExtra(CSV_IMPORT_FIELD_SEPARATOR, fieldSeparator.getSelectedItem().toString().charAt(1));
        data.putExtra(CSV_IMPORT_USE_HEADER_FROM_FILE, useHeaderFromFile.isChecked());
    }

    @Override
    protected void savePreferences() {
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();

        currencyPreferences.savePreferences(this, editor);
        editor.putLong(CSV_IMPORT_SELECTED_ACCOUNT_2, getSelectedAccountId());
        Spinner dateFormats = (Spinner) findViewById(R.id.spinnerDateFormats);
        editor.putInt(CSV_IMPORT_DATE_FORMAT, dateFormats.getSelectedItemPosition());
        editor.putString(CSV_IMPORT_FILENAME, edFilename.getText().toString());
        Spinner fieldSeparator = (Spinner) findViewById(R.id.spinnerFieldSeparator);
        editor.putInt(CSV_IMPORT_FIELD_SEPARATOR, fieldSeparator.getSelectedItemPosition());
        editor.putBoolean(CSV_IMPORT_USE_HEADER_FROM_FILE, useHeaderFromFile.isChecked());
        editor.commit();
    }

    @Override
    protected void restorePreferences() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        currencyPreferences.restorePreferences(this, preferences);

        long selectedAccountId = preferences.getLong(CSV_IMPORT_SELECTED_ACCOUNT_2, 0);
        selectedAccount(selectedAccountId);

        Spinner dateFormats = (Spinner) findViewById(R.id.spinnerDateFormats);
        dateFormats.setSelection(preferences.getInt(CSV_IMPORT_DATE_FORMAT, 0));
        edFilename = (EditText) findViewById(R.id.edFilename);
        edFilename.setText(preferences.getString(CSV_IMPORT_FILENAME, ""));
        Spinner fieldSeparator = (Spinner) findViewById(R.id.spinnerFieldSeparator);
        fieldSeparator.setSelection(preferences.getInt(CSV_IMPORT_FIELD_SEPARATOR, 0));
        useHeaderFromFile.setChecked(preferences.getBoolean(CSV_IMPORT_USE_HEADER_FROM_FILE, true));
    }

    private long getSelectedAccountId() {
        return accountSpinner.getSelectedItemId();
    }

    private void selectedAccount(long selectedAccountId) {
        for (int i=0; i<accounts.size(); i++) {
            Account a = accounts.get(i);
            if (a.id == selectedAccountId) {
                accountSpinner.setSelection(i);
                break;
            }
        }
    }

}
