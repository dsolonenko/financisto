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
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.*;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.TemplateListAdapter;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.widget.SearchFilterTextWatcherListener;

import static ru.orangesoftware.financisto.activity.MyEntityListActivity.FILTER_DELAY_MILLIS;
import static ru.orangesoftware.financisto.blotter.BlotterFilter.CATEGORY_NAME;
import static ru.orangesoftware.financisto.blotter.BlotterFilter.TEMPLATE_NAME;
import static ru.orangesoftware.financisto.filter.WhereFilter.Operation.LIKE;

public class SelectTemplateActivity extends TemplatesListActivity {

    public static final String TEMPATE_ID = "template_id";
    public static final String MULTIPLIER = "multiplier";
    public static final String EDIT_AFTER_CREATION = "edit_after_creation";

    private TextView multiplierText;
    private EditText searchFilter;
    private int multiplier = 1;

    public SelectTemplateActivity() {
        super(R.layout.templates);
    }

    @Override
    protected void internalOnCreate(Bundle savedInstanceState) {
        internalOnCreateTemplates();

        getListView().setOnItemLongClickListener((parent, view, position, id) -> {
            returnResult(id, true);
            return true;
        });

        Button b = findViewById(R.id.bEditTemplates);
        b.setOnClickListener(arg0 -> {
            setResult(RESULT_CANCELED);
            finish();
            Intent intent = new Intent(SelectTemplateActivity.this, TemplatesListActivity.class);
            startActivity(intent);
        });
        b = findViewById(R.id.bCancel);
        b.setOnClickListener(arg0 -> {
            setResult(RESULT_CANCELED);
            finish();
        });
        multiplierText = findViewById(R.id.multiplier);
        ImageButton ib = findViewById(R.id.bPlus);
        ib.setOnClickListener(arg0 -> incrementMultiplier());
        ib = findViewById(R.id.bMinus);
        ib.setOnClickListener(arg0 -> decrementMultiplier());
        
        searchFilter = findViewById(R.id.searchFilter);
        searchFilter.addTextChangedListener(new SearchFilterTextWatcherListener(FILTER_DELAY_MILLIS) {
            @Override
            public void clearFilter(String oldFilter) {
                blotterFilter.remove(TEMPLATE_NAME);
            }

            @Override
            public void applyFilter(String filter) {
                if (!TextUtils.isEmpty(filter)) {
                    filter = "%" + filter.replace(" ", "%") + "%";
                    blotterFilter.put(Criteria.or(
                            new Criteria(TEMPLATE_NAME, LIKE, filter),
                            new Criteria(CATEGORY_NAME, LIKE, filter)));
                }
                recreateCursor();
            }
        });
    }

    @Override
    protected Cursor createCursor() {
        return super.createCursor();
    }

    protected void incrementMultiplier() {
        ++multiplier;
        multiplierText.setText("x" + multiplier);
    }

    protected void decrementMultiplier() {
        --multiplier;
        if (multiplier < 1) {
            multiplier = 1;
        }
        multiplierText.setText("x" + multiplier);
    }

    @Override
    public void registerForContextMenu(View view) {
    }

    @Override
    protected ListAdapter createAdapter(Cursor cursor) {
        return new TemplateListAdapter(this, db, cursor);
    }

    @Override
    protected void onItemClick(View v, int position, long id) {
        returnResult(id, false);
    }

    @Override
    protected void viewItem(View v, int position, long id) {
        returnResult(id, false);
    }

    @Override
    public void editItem(View v, int position, long id) {
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        // do nothing
    }

    void returnResult(long id, boolean edit) {
        Intent intent = new Intent();
        intent.putExtra(TEMPATE_ID, id);
        intent.putExtra(MULTIPLIER, multiplier);
        if (edit) intent.putExtra(EDIT_AFTER_CREATION, true);
        setResult(RESULT_OK, intent);
        finish();
    }

}
