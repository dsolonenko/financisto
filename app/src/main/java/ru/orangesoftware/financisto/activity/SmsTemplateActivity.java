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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.model.SmsTemplate;

public class SmsTemplateActivity extends Activity {

    public static final String ENTITY_ID_EXTRA = "entityId";

    private DatabaseAdapter db;
    private SmsTemplate smsTemplate = new SmsTemplate();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.smstemplate);

        db = new DatabaseAdapter(this);
        db.open();

        Button bOK = (Button) findViewById(R.id.bOK);
        bOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                EditText smsNumber = (EditText) findViewById(R.id.title);
                EditText templateTxt = (EditText) findViewById(R.id.sms_template);
                // todo.mb: add account spinner
                smsTemplate.title = smsNumber.getText().toString();
                smsTemplate.template = templateTxt.getText().toString();
                long id = db.saveOrUpdate(smsTemplate);
                Intent intent = new Intent();
                intent.putExtra(DatabaseHelper.EntityColumns.ID, id);
                setResult(RESULT_OK, intent);
                finish();
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

        Intent intent = getIntent();
        if (intent != null) {
            long id = intent.getLongExtra(ENTITY_ID_EXTRA, -1);
            if (id != -1) {
                smsTemplate = db.load(SmsTemplate.class, id);
                editSmsTemplate();
            }
        }

    }

    private void editSmsTemplate() {
        EditText smsNumber = (EditText) findViewById(R.id.title);
        EditText templateTxt = (EditText) findViewById(R.id.sms_template);
        CheckBox activityCheckBox = (CheckBox) findViewById(R.id.isActive);
        smsNumber.setText(smsTemplate.title);

    }

}
