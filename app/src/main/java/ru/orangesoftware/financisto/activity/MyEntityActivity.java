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
import android.widget.EditText;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.model.MyEntity;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.PinProtection;

public abstract class MyEntityActivity<T extends MyEntity> extends Activity {
	
	public static final String ENTITY_ID_EXTRA = "entityId";

    private final Class<T> clazz;

	private DatabaseAdapter db;	

	private T entity;

    protected MyEntityActivity(Class<T> clazz) {
        try {
            this.clazz = clazz;
            this.entity = clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(MyPreferences.switchLocale(base));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.project);

		db = new DatabaseAdapter(this);
		db.open();
		
		Button bOK = findViewById(R.id.bOK);
		bOK.setOnClickListener(arg0 -> {
            EditText title = findViewById(R.id.title);
            entity.title = title.getText().toString();
            updateEntity(entity);
            long id = db.saveOrUpdate(entity);
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
				entity = db.load(clazz, id);
				editEntity();
			}
		}
		
	}

    protected void updateEntity(T entity) {
        // do nothing
    }

    private void editEntity() {
		EditText title = findViewById(R.id.title);
		title.setText(entity.title);
	}

	@Override
	protected void onDestroy() {
		db.close();
		super.onDestroy();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		PinProtection.lock(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		PinProtection.unlock(this);
	}
}
