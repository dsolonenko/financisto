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

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import java.util.ArrayList;
import ru.orangesoftware.financisto.R;
import static ru.orangesoftware.financisto.activity.CategorySelector.SelectorType.PLAIN;
import ru.orangesoftware.financisto.adapter.MyEntityAdapter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper.SmsTemplateColumns;
import ru.orangesoftware.financisto.model.Account;
import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.SmsTemplate;
import ru.orangesoftware.financisto.service.SmsTransactionProcessor;
import ru.orangesoftware.financisto.utils.Utils;

public class SmsTemplateActivity extends AbstractActivity implements CategorySelector.CategorySelectorListener {

    private DatabaseAdapter db;

    private EditText smsNumber;
    private EditText templateTxt;
    private EditText exampleTxt;
    private Spinner accountSpinner;
    private ToggleButton toggleIncome;
    private ArrayList<Account> accounts;
    private long categoryId = -1;
    private SmsTemplate smsTemplate = new SmsTemplate();
    private CategorySelector categorySelector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.smstemplate);

        db = new DatabaseAdapter(this);
        db.open();

        smsNumber = (EditText)findViewById(R.id.sms_number);
        initTitleAndDynamicDescription();
        templateTxt = (EditText)findViewById(R.id.sms_template);
        initAccounts();
        toggleIncome = (ToggleButton) findViewById(R.id.toggle);

        Button bOK = (Button) findViewById(R.id.bOK);
        bOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                updateSmsTemplateFromUI();
                if (Utils.checkEditText(smsNumber, "sms number", true, 30)
                    && Utils.checkEditText(templateTxt, "sms template", true, 160)) {
                    long id = db.saveOrUpdate(smsTemplate);
                    Intent intent = new Intent();
                    intent.putExtra(SmsTemplateColumns._id.name(), id);
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

        initExampleField();

        fillByCallerData();
        initCategories();
    }

    private void initTitleAndDynamicDescription() {
        TextView templateTitle = (TextView) findViewById(R.id.sms_tpl_title);
        final TextView templateDesc = (TextView) findViewById(R.id.sms_tpl_desc);
        templateDesc.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                templateDesc.setVisibility(View.GONE);
            }
        });
        templateTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                templateDesc.setVisibility( templateDesc.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void initExampleField() {
        exampleTxt = (EditText) findViewById(R.id.sms_example);
        exampleTxt.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                exampleTxt.setAlpha(1F);
            }
        });
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

    private void initCategories() {
        if (categoryId == -1) {
            categorySelector = new CategorySelector(this, db, x);
            LinearLayout layout = (LinearLayout) findViewById(R.id.list);
            categorySelector.createNode(layout, PLAIN);
            categorySelector.setListener(this);
            categorySelector.fetchCategories(false);
            categorySelector.doNotShowSplitCategory();

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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        categorySelector.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCategorySelected(Category category, boolean selectLast) {
        categoryId = category.id;
    }

    private void validateExampleAndHighlight(String template, String example) {
        if (Utils.isNotEmpty(template) && template.length() > 4 && Utils.isNotEmpty(example)) {
//            Log.d("777", String.format("template: `%s`", template));
            final Resources resources = SmsTemplateActivity.this.getResources();
            final String[] matches = SmsTransactionProcessor.findTemplateMatches(template, example);
            if (matches == null) {
                exampleTxt.setBackgroundColor(resources.getColor(R.color.negative_amount));
            } else {
                // todo.mb: Toast about parsed items
                exampleTxt.setBackgroundColor(resources.getColor(R.color.cleared_transaction_color));
            }
        }
    }
}
