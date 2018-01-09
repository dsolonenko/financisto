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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.model.Project;
import ru.orangesoftware.financisto.utils.MyPreferences;

public class ProjectActivity extends Activity {

    public static final String ENTITY_ID_EXTRA = "entityId";

    private DatabaseAdapter db;
    private Project project = new Project();

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.project);

        CheckBox activityCheckBox = findViewById(R.id.isActive);
        activityCheckBox.setChecked(true);

        db = new DatabaseAdapter(this);
        db.open();

        Button bOK = findViewById(R.id.bOK);
        bOK.setOnClickListener(arg0 -> {
            EditText title = findViewById(R.id.title);
            project.title = title.getText().toString();
            project.isActive = activityCheckBox.isChecked();
            long id = db.saveOrUpdate(project);
            Intent intent = new Intent();
            intent.putExtra(DatabaseHelper.EntityColumns.ID, id);
            setResult(RESULT_OK, intent);
            finish();
        });

        Button bCancel = findViewById(R.id.bCancel);
        bCancel.setOnClickListener(arg0 -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        Intent intent = getIntent();
        if (intent != null) {
            long id = intent.getLongExtra(ENTITY_ID_EXTRA, -1);
            if (id != -1) {
                project = db.load(Project.class, id);
                editProject();
            }
        }

    }

    private void editProject() {
        EditText title = findViewById(R.id.title);
        CheckBox activityCheckBox = findViewById(R.id.isActive);
        title.setText(project.title);
        activityCheckBox.setChecked(project.isActive);
    }

}
