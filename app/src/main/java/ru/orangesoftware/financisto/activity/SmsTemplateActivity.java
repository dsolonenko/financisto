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
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.MyEntityAdapter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper.SmsTemplateColumns;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.SmsTemplate;
import ru.orangesoftware.financisto.service.SmsTransactionProcessor;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.Utils;

import java.util.ArrayList;

import static ru.orangesoftware.financisto.activity.CategorySelector.SelectorType.FILTER;

public class SmsTemplateActivity extends AbstractActivity {

    private DatabaseAdapter db;

    private EditText smsNumber;
    private EditText templateTxt;
    private EditText exampleTxt;
    private Spinner accountSpinner;
    private ToggleButton toggleIncome;
    private ArrayList<Account> accounts;
    private long categoryId = -1;
    private SmsTemplate smsTemplate = new SmsTemplate();
    private CategorySelector<SmsTemplateActivity> categorySelector;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.smstemplate);

        db = new DatabaseAdapter(this);
        db.open();

        smsNumber = findViewById(R.id.sms_number);
        initTitleAndDynamicDescription();
        templateTxt = findViewById(R.id.sms_template);
        initAccounts();
        toggleIncome = findViewById(R.id.toggle);

        Button bOK = findViewById(R.id.bOK);
        bOK.setOnClickListener(arg0 -> {
            updateSmsTemplateFromUI();
            if (Utils.checkEditText(smsNumber, "sms number", true, 30)
                && Utils.checkEditText(templateTxt, "sms template", true, 160)) {
                long id = db.saveOrUpdate(smsTemplate);
                Intent intent = new Intent();
                intent.putExtra(SmsTemplateColumns._id.name(), id);
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        Button bCancel = findViewById(R.id.bCancel);
        bCancel.setOnClickListener(arg0 -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        initExampleField();

        fillByCallerData();
        initCategorySelector();
    }

    private void initTitleAndDynamicDescription() {
        TextView templateTitle = findViewById(R.id.sms_tpl_title);
        final TextView templateDesc = findViewById(R.id.sms_tpl_desc);
        templateDesc.setOnClickListener(v -> templateDesc.setVisibility(View.GONE));
        templateTitle.setOnClickListener(v -> templateDesc.setVisibility( templateDesc.getVisibility() == View.GONE ? View.VISIBLE : View.GONE));
    }

    private void initExampleField() {
        exampleTxt = findViewById(R.id.sms_example);
        exampleTxt.setOnFocusChangeListener((v, hasFocus) -> exampleTxt.setAlpha(1F));
        exampleTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateExampleAndHighlight(templateTxt.getText().toString(), s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        templateTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateExampleAndHighlight(s.toString(), exampleTxt.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void initCategorySelector() {
        if (categoryId == -1) {
            categorySelector = new CategorySelector<>(this, db, x);
            categorySelector.setEmptyResId(R.string.no_category);
            categorySelector.doNotShowSplitCategory();
            categorySelector.fetchCategories(false);
            categorySelector.createNode(findViewById(R.id.list2), FILTER);
            
            if (smsTemplate != null) {
                categorySelector.selectCategory(smsTemplate.categoryId, false);
            }
        }
    }

    @Override
    protected void onClick(View v, int id) {
        categorySelector.onClick(id);
    }

    private void initAccounts() {
        accounts = new ArrayList<>();
        Account emptyItem = new Account();
        emptyItem.id = -1;
        emptyItem.title = getString(R.string.no_account);
        accounts.add(emptyItem);
        accounts.addAll(db.getAllAccountsList());

        ArrayAdapter<Account> accountsAdapter = new MyEntityAdapter<>(this, android.R.layout.simple_spinner_item, android.R.id.text1, accounts);
        accountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        accountSpinner = findViewById(R.id.spinnerAccount);
        accountSpinner.setAdapter(accountsAdapter);
    }

    private void updateSmsTemplateFromUI() {
        smsTemplate.title = smsNumber.getText().toString();
        smsTemplate.template = templateTxt.getText().toString();
        smsTemplate.categoryId = categorySelector == null ? categoryId : categorySelector.getSelectedCategoryId();
        smsTemplate.isIncome = toggleIncome.isChecked();
        smsTemplate.accountId = accountSpinner.getSelectedItemId();

    }

    private void fillByCallerData() {
        final Intent intent = getIntent();
        if (intent != null) {
            long id = intent.getLongExtra(SmsTemplateColumns._id.name(), -1);
            categoryId = intent.getLongExtra(SmsTemplateColumns.category_id.name(), -1);
            if (id != -1) {
                smsTemplate = db.load(SmsTemplate.class, id);
                editSmsTemplate();
            }
        }
    }

    private void editSmsTemplate() {
        smsNumber.setText(smsTemplate.title);
        templateTxt.setText(smsTemplate.template);
        selectedAccount(smsTemplate.accountId);
        toggleIncome.setChecked(smsTemplate.isIncome);
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

    @Override
    public void onSelectedId(int id, long selectedId) {
        categorySelector.onSelectedId(id, selectedId);
        switch (id) {
            case R.id.category:
                categoryId = categorySelector.getSelectedCategoryId();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        categorySelector.onActivityResult(requestCode, resultCode, data);
    }

    private void validateExampleAndHighlight(String template, String example) {
        if (Utils.isNotEmpty(template) && template.length() > 4 && Utils.isNotEmpty(example)) {
            final Resources resources = SmsTemplateActivity.this.getResources();
            final String[] matches = SmsTransactionProcessor.findTemplateMatches(template, example);
            if (matches == null) {
                exampleTxt.setBackgroundColor(resources.getColor(R.color.negative_amount));
            } else {
                exampleTxt.setBackgroundColor(resources.getColor(R.color.cleared_transaction_color));
            }
        }
    }
}
