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
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ToggleButton;
import java.util.ArrayList;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.MyEntityAdapter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper.SmsTemplateColumns;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.SmsTemplate;
import ru.orangesoftware.financisto.utils.Utils;

public class SmsTemplateActivity extends Activity {

    private DatabaseAdapter db;

    private EditText smsNumber;
    private EditText templateTxt;
    private Spinner accountSpinner;
    private ToggleButton toggleView;

    private ArrayList<Account> accounts;
    private long categoryId = -1;
    private SmsTemplate smsTemplate = new SmsTemplate();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.smstemplate);

        db = new DatabaseAdapter(this);
        db.open();

        smsNumber = (EditText)findViewById(R.id.sms_number);
        templateTxt = (EditText)findViewById(R.id.sms_template);
        initAccounts();
        toggleView = (ToggleButton) findViewById(R.id.toggle);

        Button bOK = (Button) findViewById(R.id.bOK);
        bOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                updateSmsTemplateFromUI();
                if (Utils.checkEditText(smsNumber, "sms number", true, 30)
                    && Utils.checkEditText(templateTxt, "sms template", true, 160)) {
                    long id = db.saveOrUpdate(smsTemplate);
                    Intent intent = new Intent();
                    intent.putExtra(SmsTemplateColumns.ID, id);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });

        Button bCancel = (Button) findViewById(R.id.bCancel);
        bCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        fillByCallerData();
    }

    private void initAccounts() {
        accounts = new ArrayList<Account>();
        Account emptyItem = new Account();
        emptyItem.id = -1;
        emptyItem.title = getString(R.string.no_account);
        accounts.add(emptyItem);
        accounts.addAll(db.getAllAccountsList());

        ArrayAdapter<Account> accountsAdapter = new MyEntityAdapter<Account>(this, android.R.layout.simple_spinner_item, android.R.id.text1, accounts);
        accountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        accountSpinner = (Spinner) findViewById(R.id.spinnerAccount);
        accountSpinner.setAdapter(accountsAdapter);
    }

    private void updateSmsTemplateFromUI() {
        smsTemplate.title = smsNumber.getText().toString();
        smsTemplate.template = templateTxt.getText().toString();
        smsTemplate.categoryId = categoryId;
        smsTemplate.isIncome = toggleView.isChecked();
        smsTemplate.accountId = accountSpinner.getSelectedItemId();

    }

    private void fillByCallerData() {
        final Intent intent = getIntent();
        if (intent != null) {
            long id = intent.getLongExtra(SmsTemplateColumns.ID, -1);
            if (id != -1) {
                smsTemplate = db.load(SmsTemplate.class, id);
                editSmsTemplate();
            }
            categoryId = intent.getLongExtra(SmsTemplateColumns.CATEGORY_ID, -1);
        }
    }

    private void editSmsTemplate() {
        smsNumber.setText(smsTemplate.title);
        templateTxt.setText(smsTemplate.template);
        selectedAccount(smsTemplate.accountId);
        toggleView.setChecked(smsTemplate.isIncome);
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
