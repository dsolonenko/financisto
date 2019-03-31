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
		setContentView(R.layout.entity);

		CheckBox activityCheckBox = findViewById(R.id.isActive);
		activityCheckBox.setChecked(true);

		db = new DatabaseAdapter(this);
		db.open();
		
		Button bOK = findViewById(R.id.bOK);
		bOK.setOnClickListener(arg0 -> {
			EditText title = findViewById(R.id.title);
            entity.title = title.getText().toString();
			entity.isActive = activityCheckBox.isChecked();
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

    private void editEntity() {
		EditText title = findViewById(R.id.title);
		title.setText(entity.title);
		CheckBox activityCheckBox = findViewById(R.id.isActive);
		activityCheckBox.setChecked(entity.isActive);
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
