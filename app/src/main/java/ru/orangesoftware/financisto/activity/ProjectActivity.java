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
import ru.orangesoftware.financisto.db.MyEntityManager;
import ru.orangesoftware.financisto.model.Project;

public class ProjectActivity extends Activity {


    public static final String ENTITY_ID_EXTRA = "entityId";
    private DatabaseAdapter db;
    private MyEntityManager em;

    private Project project = new Project();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.project);

        CheckBox activityCheckBox = (CheckBox) findViewById(R.id.isActive);
        activityCheckBox.setChecked(true);

        db = new DatabaseAdapter(this);
        db.open();

        em = db.em();

        Button bOK = (Button)findViewById(R.id.bOK);
        bOK.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View arg0) {
                EditText title = (EditText)findViewById(R.id.title);
                CheckBox activityCheckBox = (CheckBox) findViewById(R.id.isActive);
                project.title = title.getText().toString();
                project.isActive = activityCheckBox.isChecked();
                long id = em.saveOrUpdate(project);
                Intent intent = new Intent();
                intent.putExtra(DatabaseHelper.EntityColumns.ID, id);
                setResult(RESULT_OK, intent);
                finish();
            }

        });

        Button bCancel = (Button)findViewById(R.id.bCancel);
        bCancel.setOnClickListener(new View.OnClickListener(){
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
                project = em.load(Project.class, id);
                editProject();
            }
        }

    }

    private void editProject() {
        EditText title = (EditText)findViewById(R.id.title);
        CheckBox activityCheckBox = (CheckBox) findViewById(R.id.isActive);
        title.setText(project.title);
        activityCheckBox.setChecked(project.isActive);
    }

}
