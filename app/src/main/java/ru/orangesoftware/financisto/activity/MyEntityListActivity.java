/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * <p/>
 * Contributors:
 * Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.EntityListAdapter;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.model.MyEntity;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.widget.SearchFilterTextWatcherListener;

import java.util.List;

public abstract class MyEntityListActivity<T extends MyEntity> extends AbstractListActivity {

    private static final int NEW_ENTITY_REQUEST = 1;
    private static final int EDIT_ENTITY_REQUEST = 2;
    
    public static final int FILTER_DELAY_MILLIS = 500;

    private final Class<T> clazz;
    private final int emptyResId;

    private List<T> entities;
    private EditText searchFilter;
    protected volatile String titleFilter;
    
    public MyEntityListActivity(Class<T> clazz, int emptyResId) {
        this(clazz, R.layout.project_list, emptyResId);
    }
    
    public MyEntityListActivity(Class<T> clazz, int layoutId, int emptyResId) {
        super(layoutId);
        this.clazz = clazz;
        this.emptyResId = emptyResId;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    @Override
    protected void internalOnCreate(Bundle savedInstanceState) {
        super.internalOnCreate(savedInstanceState);
        entities = loadEntities();
        ((TextView) findViewById(android.R.id.empty)).setText(emptyResId);

        searchFilter = findViewById(R.id.searchFilter);
        if (searchFilter != null) {
            searchFilter.addTextChangedListener(new SearchFilterTextWatcherListener(FILTER_DELAY_MILLIS) {
                @Override
                public void clearFilter(String oldFilter) {
                    titleFilter = null;
                }

                @Override
                public void applyFilter(String filter) {
                    if (!TextUtils.isEmpty(filter))  titleFilter = filter;
                    
                    recreateCursor();
                }
            });
        }
    }

    protected List<T> loadEntities() {
        return db.getAllEntitiesList(clazz, false, false, titleFilter);
    }

    @Override
    protected void addItem() {
        Intent intent = new Intent(MyEntityListActivity.this, getEditActivityClass());
        startActivityForResult(intent, NEW_ENTITY_REQUEST);
    }

    protected abstract Class<? extends MyEntityActivity> getEditActivityClass();

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
        return new EntityListAdapter<>(this, entities);
    }

    @Override
    protected Cursor createCursor() {
        return null;
    }

    @Override
    public void recreateCursor() {
        entities = loadEntities();
        @SuppressWarnings("unchecked")
        EntityListAdapter<T> a = (EntityListAdapter<T>) adapter;
        a.setEntities(entities);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            recreateCursor();
        }
    }

    @Override
    protected void deleteItem(View v, int position, final long id) {
        db.delete(clazz, id);
        recreateCursor();
    }

    @Override
    public void editItem(View v, int position, long id) {
        Intent intent = new Intent(MyEntityListActivity.this, getEditActivityClass());
        intent.putExtra(MyEntityActivity.ENTITY_ID_EXTRA, id);
        startActivityForResult(intent, EDIT_ENTITY_REQUEST);
    }

    @Override
    protected void viewItem(View v, int position, long id) {
        T e = db.load(clazz, id);
        Intent intent = new Intent(this, BlotterActivity.class);
        Criteria blotterFilter = createBlotterCriteria(e);
        blotterFilter.toIntent(e.title, intent);
        startActivity(intent);
    }

    protected abstract Criteria createBlotterCriteria(T e);

}
