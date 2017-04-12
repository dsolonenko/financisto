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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.MultiChoiceItem;
import ru.orangesoftware.financisto.utils.CurrencyExportPreferences;
import ru.orangesoftware.financisto.view.NodeInflater;

import java.util.ArrayList;
import java.util.List;

public class QifExportActivity extends AbstractExportActivity implements ActivityLayoutListener {

    public static final String QIF_EXPORT_SELECTED_ACCOUNTS = "QIF_EXPORT_SELECTED_ACCOUNTS";
    public static final String QIF_EXPORT_DATE_FORMAT = "QIF_EXPORT_DATE_FORMAT";
    public static final String QIF_EXPORT_UPLOAD_TO_DROPBOX = "QIF_EXPORT_UPLOAD_TO_DROPBOX";

    private final CurrencyExportPreferences currencyPreferences = new CurrencyExportPreferences("qif");

    private DatabaseAdapter db;
    private List<Account> accounts;

    private Button bAccounts;

    public QifExportActivity() {
        super(R.layout.qif_export);
    }

    @Override
    protected void internalOnCreate() {
        LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        NodeInflater nodeInflater = new NodeInflater(layoutInflater);
        final ActivityLayout activityLayout = new ActivityLayout(nodeInflater, this);

        db = new DatabaseAdapter(this);
        db.open();

        accounts = db.getAllAccountsList();

        bAccounts = (Button)findViewById(R.id.bAccounts);
        bAccounts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityLayout.selectMultiChoice(QifExportActivity.this, R.id.bAccounts, R.string.accounts, accounts);
            }
        });

        clearFilter();
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    @Override
    public void onSelected(int id, List<? extends MultiChoiceItem> items) {
        List<Account> selectedAccounts = getSelectedAccounts();
        if (selectedAccounts.size() == 0 || selectedAccounts.size() == accounts.size()) {
            bAccounts.setText(R.string.all_accounts);
        } else {
            StringBuilder sb = new StringBuilder();
            for (Account a : selectedAccounts) {
                appendItemTo(sb, a.title);
            }
            bAccounts.setText(sb.toString());
        }
    }

    private ArrayList<Account> getSelectedAccounts() {
        ArrayList<Account> selected = new ArrayList<Account>();
        for (MultiChoiceItem i : accounts) {
            if (i.isChecked()) {
                selected.add((Account)i);
            }
        }
        return selected;
    }

    private void appendItemTo(StringBuilder sb, String s) {
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(s);
    }

    @Override
    public void onSelectedPos(int id, int selectedPos) {
    }

    @Override
    public void onSelectedId(int id, long selectedId) {
    }

    @Override
    public void onClick(View view) {
    }

    @Override
    protected void updateResultIntentFromUi(Intent data) {
        currencyPreferences.updateIntentFromUI(this, data);
        long[] selectedIds = getSelectedAccountsIds();
        if (selectedIds.length > 0) {
            data.putExtra(QIF_EXPORT_SELECTED_ACCOUNTS, selectedIds);
        }
        Spinner dateFormats = (Spinner)findViewById(R.id.spinnerDateFormats);
        data.putExtra(QIF_EXPORT_DATE_FORMAT, dateFormats.getSelectedItem().toString());
        CheckBox uploadToDropbox = (CheckBox)findViewById(R.id.checkboxUploadToDropbox);
        data.putExtra(QIF_EXPORT_UPLOAD_TO_DROPBOX, uploadToDropbox.isChecked());
    }

    private long[] getSelectedAccountsIds() {
        List<Long> selectedAccounts = new ArrayList<Long>(accounts.size());
        for (Account account : accounts) {
            if (account.isChecked()) {
                selectedAccounts.add(account.id);
            }
        }
        int count = selectedAccounts.size();
        long[] ids = new long[count];
        for (int i=0; i<count; i++) {
            ids[i] = selectedAccounts.get(i);
        }
        return ids;
    }

	protected void savePreferences() {
		SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();

        currencyPreferences.savePreferences(this, editor);

        long[] selectedIds = getSelectedAccountsIds();
        if (selectedIds.length > 0) {
            editor.putString(QIF_EXPORT_SELECTED_ACCOUNTS, joinSelectedAccounts(selectedIds));
        }

        Spinner dateFormats = (Spinner)findViewById(R.id.spinnerDateFormats);
		editor.putInt(QIF_EXPORT_DATE_FORMAT, dateFormats.getSelectedItemPosition());
        CheckBox uploadToDropbox = (CheckBox)findViewById(R.id.checkboxUploadToDropbox);
        editor.putBoolean(QIF_EXPORT_UPLOAD_TO_DROPBOX, uploadToDropbox.isChecked());

		editor.apply();
	}

    private String joinSelectedAccounts(long[] selectedIds) {
        StringBuilder sb = new StringBuilder();
        for (long selectedId : selectedIds) {
            if (sb.length() > 0) sb.append(",");
            sb.append(selectedId);
        }
        return sb.toString();
    }

    protected void restorePreferences() {
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        currencyPreferences.restorePreferences(this, preferences);

        String selectedIds = preferences.getString(QIF_EXPORT_SELECTED_ACCOUNTS, "");
        parseSelectedAccounts(selectedIds);
        onSelected(-1, accounts);

        Spinner dateFormats = (Spinner)findViewById(R.id.spinnerDateFormats);
        dateFormats.setSelection(preferences.getInt(QIF_EXPORT_DATE_FORMAT, 0));

        CheckBox uploadToDropbox = (CheckBox)findViewById(R.id.checkboxUploadToDropbox);
        uploadToDropbox.setChecked(preferences.getBoolean(QIF_EXPORT_UPLOAD_TO_DROPBOX, false));
	}

    private void parseSelectedAccounts(String selectedIds) {
        try {
            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(',');
            splitter.setString(selectedIds);
            for (String s : splitter) {
                long id = Long.parseLong(s);
                for (Account account : accounts) {
                    if (account.id == id) {
                        account.setChecked(true);
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            // ignore
        }
    }

}
